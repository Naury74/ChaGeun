package com.naury.chageun.feature.vehicle.album

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.album.AlbumRepository
import com.naury.chageun.core.domain.vehicle.VehiclePhotoRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.AlbumPhoto
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 같은 날 찍은 사진 묶음. */
data class AlbumDay(val date: LocalDate, val photos: List<AlbumPhoto>)

data class AlbumUiState(
    val isLoading: Boolean = true,
    val days: List<AlbumDay> = emptyList(),
    val selected: AlbumPhoto? = null,
    /** 방금 한 장을 넣었거나 수정을 눌러 날짜·코멘트를 입력받는 사진. */
    val editing: AlbumPhoto? = null,
    val failedCount: Int = 0,
    val isCoverUpdated: Boolean = false,
) {
    val photos: List<AlbumPhoto> get() = days.flatMap { it.photos }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val albumRepository: AlbumRepository,
    private val photoRepository: VehiclePhotoRepository,
    private val clock: Clock,
) : ViewModel() {

    private val selectedId = savedStateHandle.getStateFlow<String?>(KEY_SELECTED, null)
    private val editingId = savedStateHandle.getStateFlow<String?>(KEY_EDITING, null)
    private val messages = MutableStateFlow(Messages())

    val uiState: StateFlow<AlbumUiState> = vehicleRepository.observePrimaryVehicle()
        .filterNotNull()
        .flatMapLatest { vehicle ->
            combine(albumRepository.observe(vehicle.id), selectedId, editingId, messages) {
                    photos,
                    selected,
                    editing,
                    message,
                ->
                AlbumUiState(
                    isLoading = false,
                    days = photos.groupBy { it.takenOn }.map { (date, items) -> AlbumDay(date, items) },
                    selected = photos.firstOrNull { it.id == selected },
                    editing = photos.firstOrNull { it.id == editing },
                    failedCount = message.failed,
                    isCoverUpdated = message.coverUpdated,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AlbumUiState())

    fun select(id: String?) {
        savedStateHandle[KEY_SELECTED] = id
    }

    /** 한 장만 넣었으면 바로 날짜·코멘트를 입력받는다. */
    fun add(sourceUris: List<String>, highQuality: Boolean) {
        if (sourceUris.isEmpty()) return
        viewModelScope.launch {
            val result = albumRepository.add(vehicleId(), sourceUris, LocalDate.now(clock), highQuality)
            messages.value = Messages(failed = result.failed)
            result.addedIds.singleOrNull()?.let { savedStateHandle[KEY_EDITING] = it }
        }
    }

    fun startEditing(id: String) {
        savedStateHandle[KEY_EDITING] = id
    }

    fun stopEditing() {
        savedStateHandle[KEY_EDITING] = null
    }

    fun saveDetails(id: String, takenOn: LocalDate, comment: String) {
        viewModelScope.launch {
            albumRepository.updateDetails(vehicleId(), id, takenOn, comment)
            stopEditing()
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            albumRepository.delete(vehicleId(), id)
            if (selectedId.value == id) select(null)
        }
    }

    /** 앨범 사진을 내 차 대표 사진으로 쓴다. 대표 사진은 배경 제거를 거쳐 따로 저장된다. */
    fun setAsCover(photo: AlbumPhoto) {
        viewModelScope.launch {
            val updated = photoRepository.replace(vehicleId(), Uri.fromFile(File(photo.filePath)).toString())
            messages.value = Messages(coverUpdated = updated)
        }
    }

    fun dismissMessage() {
        messages.value = Messages()
    }

    private suspend fun vehicleId() = vehicleRepository.observePrimaryVehicle().filterNotNull().first().id

    private data class Messages(val failed: Int = 0, val coverUpdated: Boolean = false)

    private companion object {
        const val KEY_SELECTED = "album_selected"
        const val KEY_EDITING = "album_editing"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
