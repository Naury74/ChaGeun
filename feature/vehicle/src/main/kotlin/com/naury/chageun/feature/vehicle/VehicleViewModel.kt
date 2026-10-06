package com.naury.chageun.feature.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.album.AlbumRepository
import com.naury.chageun.core.domain.vehicle.CompleteInspectionUseCase
import com.naury.chageun.core.domain.vehicle.InspectionEvaluator
import com.naury.chageun.core.domain.vehicle.InspectionRepository
import com.naury.chageun.core.domain.vehicle.VehiclePhotoRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.CutoutStatus
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface VehicleUiState {
    data object Loading : VehicleUiState

    data class Content(
        val vehicle: Vehicle,
        val mileageLog: List<MileageEntry>,
        val inspection: InspectionStatus = InspectionStatus.Unknown,
        val photoPath: String? = null,
        val isPhotoImportFailed: Boolean = false,
        val cutout: CutoutStatus = CutoutStatus.Idle,
        val isBackgroundRemovalEnabled: Boolean = true,
        /** 내 차 탭에 미리 보여 줄 최근 앨범 사진. */
        val albumPreview: List<AlbumPhoto> = emptyList(),
        val albumCount: Int = 0,
    ) : VehicleUiState {
        val currentMileage: MileageEntry? get() = mileageLog.firstOrNull()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VehicleViewModel @Inject constructor(
    vehicleRepository: VehicleRepository,
    private val inspectionRepository: InspectionRepository,
    private val completeInspection: CompleteInspectionUseCase,
    private val photoRepository: VehiclePhotoRepository,
    albumRepository: AlbumRepository,
    private val clock: Clock,
) : ViewModel() {

    private val photoImportFailed = MutableStateFlow(false)

    val uiState: StateFlow<VehicleUiState> = vehicleRepository.observePrimaryVehicle()
        .filterNotNull()
        .flatMapLatest { vehicle ->
            val vehicleState = combine(
                vehicleRepository.observeMileageLog(vehicle.id),
                inspectionRepository.observeSchedule(vehicle.id),
                photoRepository.observe(vehicle.id),
                photoImportFailed,
                photoRepository.observeCutout(vehicle.id),
            ) { log, schedule, photo, photoFailed, cutout ->
                VehicleUiState.Content(
                    vehicle = vehicle,
                    mileageLog = log,
                    inspection = InspectionEvaluator.evaluate(schedule, LocalDate.now(clock)),
                    photoPath = photo,
                    isPhotoImportFailed = photoFailed,
                    cutout = cutout,
                )
            }
            combine(
                vehicleState,
                albumRepository.observe(vehicle.id),
                photoRepository.observeBackgroundRemoval(vehicle.id),
            ) { state, album, removeBackground ->
                state.copy(
                    albumPreview = album.take(ALBUM_PREVIEW_COUNT),
                    albumCount = album.size,
                    isBackgroundRemovalEnabled = removeBackground,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), VehicleUiState.Loading)

    /** null이면 날짜를 지운다. */
    fun setInspectionDate(date: LocalDate?) {
        val vehicle = (uiState.value as? VehicleUiState.Content)?.vehicle ?: return
        viewModelScope.launch { inspectionRepository.setUserDueDate(vehicle.id, date) }
    }

    fun completeInspection(completion: InspectionCompletion, title: String) {
        val vehicle = (uiState.value as? VehicleUiState.Content)?.vehicle ?: return
        viewModelScope.launch {
            completeInspection(vehicle.id, completion.completedOn, title, completion.mileage, completion.nextDueDate)
        }
    }

    /** [sourceUri]는 Photo Picker가 준 content URI다. */
    /** [removeBackground]는 사진을 넣기 전 확인 시트에서 고른 값이다. */
    fun setPhoto(sourceUri: String, removeBackground: Boolean) {
        val vehicle = (uiState.value as? VehicleUiState.Content)?.vehicle ?: return
        photoImportFailed.value = false
        viewModelScope.launch {
            photoImportFailed.value = !photoRepository.replace(vehicle.id, sourceUri, removeBackground)
        }
    }

    /** 원본과 배경을 지운 사진 사이를 오간다. 켤 때 아직 지운 사진이 없으면 지우기를 시작한다. */
    fun setBackgroundRemoval(enabled: Boolean) {
        val vehicle = (uiState.value as? VehicleUiState.Content)?.vehicle ?: return
        viewModelScope.launch { photoRepository.setBackgroundRemoval(vehicle.id, enabled) }
    }

    /** 배경을 지우지 못한 사진에 다시 시도한다. 모델이 없으면 내려받는 것부터 진행 상태로 보여 준다. */
    fun removeBackground() {
        val vehicle = (uiState.value as? VehicleUiState.Content)?.vehicle ?: return
        photoRepository.removeBackground(vehicle.id)
    }

    fun removePhoto() {
        val vehicle = (uiState.value as? VehicleUiState.Content)?.vehicle ?: return
        viewModelScope.launch { photoRepository.clear(vehicle.id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val ALBUM_PREVIEW_COUNT = 4
    }
}
