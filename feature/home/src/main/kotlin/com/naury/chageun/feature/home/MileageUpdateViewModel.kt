package com.naury.chageun.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.maintenance.MaintenanceRepository
import com.naury.chageun.core.domain.maintenance.UpdateMileageResult
import com.naury.chageun.core.domain.maintenance.UpdateMileageUseCase
import com.naury.chageun.core.domain.mileage.OdometerReadResult
import com.naury.chageun.core.domain.mileage.ReadOdometerUseCase
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MileageUpdateUiState(
    val mileage: String = "",
    /** 마지막으로 기록한 주행거리. '+500 km' 같은 빠른 선택의 기준이 된다. */
    val previous: Kilometers? = null,
    val isMissing: Boolean = false,
    val lowerThan: Kilometers? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val dashboard: DashboardReadState = DashboardReadState.Idle,
)

/** 계기판 사진 판독 결과. 값은 입력칸에 채우기만 하고, 저장은 사용자가 확인한 뒤에 한다. */
sealed interface DashboardReadState {
    data object Idle : DashboardReadState

    data object Reading : DashboardReadState

    /** 글자 인식 모델을 처음 한 번 내려받는 중이다. [progress]는 0~1이며 크기를 아직 모르면 null이다. */
    data class DownloadingModel(val progress: Float?) : DashboardReadState

    data class Read(val best: Kilometers, val others: List<Kilometers>, val range: Kilometers?) : DashboardReadState

    /** 글자는 읽었지만 주행거리로 볼 숫자가 없다. 숫자 부분만 잘라 다시 시도할 수 있다. */
    data object NotFound : DashboardReadState

    /** 모델을 내려받지 못했다. 같은 사진으로 다시 시도할 수 있다. */
    data object ModelUnavailable : DashboardReadState

    /** 이 기기에서 인식기를 쓸 수 없다(네이티브 오류, 시간 초과). 직접 입력해야 한다. */
    data object Unavailable : DashboardReadState
}

@HiltViewModel
class MileageUpdateViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val updateMileage: UpdateMileageUseCase,
    private val analytics: AnalyticsTracker,
    private val readOdometer: ReadOdometerUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MileageUpdateUiState(mileage = savedStateHandle[KEY_MILEAGE] ?: ""))
    val uiState: StateFlow<MileageUpdateUiState> = _uiState.asStateFlow()

    private var previousReading: MileageReading? = null

    init {
        viewModelScope.launch {
            val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
            previousReading = maintenanceRepository.findCurrentMileage(vehicle.id)
            _uiState.update { it.copy(previous = previousReading?.mileage) }
        }
    }

    private var lastDashboardImage: String? = null

    /** [imageUri]는 사진 편집기가 남긴 캐시 파일이다. 읽은 값을 입력칸에 채운다. */
    fun readDashboard(imageUri: String) {
        lastDashboardImage = imageUri
        _uiState.update { it.copy(dashboard = DashboardReadState.Reading) }
        viewModelScope.launch {
            val result = readOdometer(imageUri, previousReading) { progress ->
                _uiState.update { it.copy(dashboard = DashboardReadState.DownloadingModel(progress)) }
            }
            val state = when (result) {
                OdometerReadResult.ModelUnavailable -> DashboardReadState.ModelUnavailable
                OdometerReadResult.Unavailable -> DashboardReadState.Unavailable
                is OdometerReadResult.Read -> result.candidates.best?.let { best ->
                    onMileageChanged(best.value.toString())
                    DashboardReadState.Read(best, result.candidates.others, result.candidates.range)
                } ?: DashboardReadState.NotFound
            }
            _uiState.update { it.copy(dashboard = state) }
        }
    }

    /** 모델을 내려받지 못했을 때 같은 사진으로 다시 읽는다. */
    fun retryDashboard() {
        lastDashboardImage?.let(::readDashboard)
    }

    fun onMileageChanged(value: String) {
        val digits = value.filter(Char::isDigit).take(MAX_DIGITS)
        savedStateHandle[KEY_MILEAGE] = digits
        _uiState.update { it.copy(mileage = digits, isMissing = false, lowerThan = null) }
    }

    fun save() = submit(isCorrectionConfirmed = false)

    fun confirmCorrection() = submit(isCorrectionConfirmed = true)

    private fun submit(isCorrectionConfirmed: Boolean) {
        val mileage = _uiState.value.mileage.toLongOrNull()
        if (mileage == null) {
            _uiState.update { it.copy(isMissing = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
            when (val result = updateMileage(vehicle.id, Kilometers(mileage), isCorrectionConfirmed)) {
                UpdateMileageResult.Saved -> {
                    analytics.track(AnalyticsEvent.MileageUpdated(isCorrection = isCorrectionConfirmed))
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
                is UpdateMileageResult.NeedsConfirmation -> _uiState.update {
                    it.copy(isSaving = false, lowerThan = result.previous)
                }
            }
        }
    }

    private companion object {
        const val KEY_MILEAGE = "mileage_update"
        const val MAX_DIGITS = 7
    }
}
