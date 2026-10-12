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
    /** 너무 길거나 커서 넣지 않은 동영상 수. */
    val tooLargeCount: Int = 0,
    val isCoverUpdated: Boolean = false,
    /** 편집한 사진·동영상을 그 자리에 저장하지 못했다. 사진 보기에 잠깐 알린다. */
    val isEditFailed: Boolean = false,
    /** 길게 눌러 여러 장을 고르는 중인 사진들. 비어 있으면 고르는 중이 아니다. */
    val picked: Set<String> = emptySet(),
    /** 그리드를 두 손가락으로 오므리고 벌려 고른 칸 크기 단계. 클수록 칸이 크고 한 줄에 적게 들어간다. */
    val gridStep: Int = DEFAULT_GRID_STEP,
) {
    val photos: List<AlbumPhoto> get() = days.flatMap { it.photos }
    val isPicking: Boolean get() = picked.isNotEmpty()
}

const val GRID_STEP_COUNT = 5
const val DEFAULT_GRID_STEP = 3

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

    /** 여러 장 고르기. 화면을 다시 만들어도 남도록 SavedStateHandle에 둔다. */
    val picks = PickedPhotos(savedStateHandle)
    private val gridStep = savedStateHandle.getStateFlow(KEY_GRID_STEP, DEFAULT_GRID_STEP)
    private val messages = MutableStateFlow(Messages())
    private val view = combine(selectedId, editingId, picks.ids, gridStep) { selected, editing, picked, step ->
        ViewState(selected, editing, picked.toSet(), step)
    }

    val uiState: StateFlow<AlbumUiState> = vehicleRepository.observePrimaryVehicle()
        .filterNotNull()
        .flatMapLatest { vehicle ->
            combine(albumRepository.observe(vehicle.id), view, messages) { photos, view, message ->
                val ids = photos.mapTo(HashSet()) { it.id }
                AlbumUiState(
                    isLoading = false,
                    days = photos.groupBy { it.takenOn }.map { (date, items) -> AlbumDay(date, items) },
                    selected = photos.firstOrNull { it.id == view.selected },
                    editing = photos.firstOrNull { it.id == view.editing },
                    failedCount = message.failed,
                    tooLargeCount = message.tooLarge,
                    isCoverUpdated = message.coverUpdated,
                    isEditFailed = message.editFailed,
                    picked = view.picked.filterTo(LinkedHashSet()) { it in ids },
                    gridStep = view.gridStep,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AlbumUiState())

    fun select(id: String?) {
        savedStateHandle[KEY_SELECTED] = id
    }

    /** 사진·동영상을 넣는다. 한 장만 넣었으면 바로 날짜·코멘트를 입력받는다. */
    fun add(sourceUris: List<String>, highQuality: Boolean) {
        if (sourceUris.isEmpty()) return
        viewModelScope.launch {
            val result = albumRepository.add(vehicleId(), sourceUris, LocalDate.now(clock), highQuality)
            messages.value = Messages(failed = result.failed, tooLarge = result.tooLarge)
            result.addedIds.singleOrNull()?.let { savedStateHandle[KEY_EDITING] = it }
        }
    }

    /** 편집기에서 고친 파일을 같은 자리에 넣는다. 날짜·메모·순서는 그대로다. */
    fun replaceMedia(id: String, sourceUri: String) {
        viewModelScope.launch {
            val replaced = albumRepository.replace(vehicleId(), id, sourceUri)
            if (!replaced) messages.value = Messages(editFailed = true)
        }
    }

    /** [id]의 날짜·메모 입력 창을 연다. null이면 닫는다. */
    fun editDetails(id: String?) {
        savedStateHandle[KEY_EDITING] = id
    }

    fun saveDetails(id: String, takenOn: LocalDate, comment: String) {
        viewModelScope.launch {
            albumRepository.updateDetails(vehicleId(), id, takenOn, comment)
            editDetails(null)
        }
    }

    /** 여러 장을 한 번에 지운다. 고르던 사진과 열어 둔 사진도 함께 정리한다. */
    fun delete(ids: List<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val vehicleId = vehicleId()
            ids.forEach { albumRepository.delete(vehicleId, it) }
            if (selectedId.value in ids) select(null)
            picks.remove(ids)
        }
    }

    /** 그날 사진을 모두 고른다. 이미 모두 골라져 있으면 그날 사진을 모두 뺀다. */
    fun pickDay(date: LocalDate) {
        val day = uiState.value.days.firstOrNull { it.date == date }?.photos?.map { it.id } ?: return
        picks.toggleAll(day)
    }

    /** [delta]가 양수면 칸을 키우고 음수면 줄인다. 범위를 넘으면 그대로 둔다. */
    fun zoomGrid(delta: Int) {
        savedStateHandle[KEY_GRID_STEP] = (gridStep.value + delta).coerceIn(0, GRID_STEP_COUNT - 1)
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

    private data class Messages(
        val failed: Int = 0,
        val tooLarge: Int = 0,
        val coverUpdated: Boolean = false,
        val editFailed: Boolean = false,
    )

    private data class ViewState(
        val selected: String?,
        val editing: String?,
        val picked: Set<String>,
        val gridStep: Int,
    )

    private companion object {
        const val KEY_SELECTED = "album_selected"
        const val KEY_EDITING = "album_editing"
        const val KEY_GRID_STEP = "album_grid_step"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** 앨범에서 길게 눌러 고른 사진 id. 고른 순서를 지킨다. */
class PickedPhotos(private val savedStateHandle: SavedStateHandle) {
    val ids: StateFlow<ArrayList<String>> = savedStateHandle.getStateFlow(KEY_PICKED, arrayListOf())

    fun toggle(id: String) {
        val current = ids.value
        set(if (id in current) current - id else current + id)
    }

    /** 모두 골라져 있으면 모두 빼고, 아니면 빠진 것을 더한다. */
    fun toggleAll(group: List<String>) {
        val current = ids.value
        set(if (current.containsAll(group)) current - group.toSet() else (current + group).distinct())
    }

    fun remove(group: List<String>) = set(ids.value - group.toSet())

    fun clear() = set(emptyList())

    private fun set(value: List<String>) {
        savedStateHandle[KEY_PICKED] = ArrayList(value)
    }

    private companion object {
        const val KEY_PICKED = "album_picked"
    }
}
