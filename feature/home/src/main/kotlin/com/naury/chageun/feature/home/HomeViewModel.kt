package com.naury.chageun.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.history.HistoryRepository
import com.naury.chageun.core.domain.history.TimelineQuery
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.vehicle.VehiclePhotoRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    vehicleRepository: VehicleRepository,
    observeMaintenanceOverview: ObserveMaintenanceOverviewUseCase,
    historyRepository: HistoryRepository,
    private val photoRepository: VehiclePhotoRepository,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = vehicleRepository.observePrimaryVehicle()
        .filterNotNull()
        .flatMapLatest { vehicle ->
            combine(
                observeMaintenanceOverview(vehicle.id),
                historyRepository.observeTimeline(vehicle.id, TimelineQuery()).map { it.take(RECENT_RECORD_COUNT) },
                photoRepository.observe(vehicle.id),
                photoRepository.observeCutout(vehicle.id),
            ) { overview, recent, photo, cutout ->
                HomeUiState.Content(
                    vehicle = vehicle,
                    overview = overview,
                    today = LocalDate.now(clock),
                    recentRecords = recent,
                    photoPath = photo,
                    cutout = cutout,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState.Loading)

    /** [sourceUri]는 Photo Picker가 준 content URI다. 실패해도 홈에서는 실루엣이 그대로 남는다. */
    fun setPhoto(sourceUri: String) {
        val vehicle = (uiState.value as? HomeUiState.Content)?.vehicle ?: return
        viewModelScope.launch { photoRepository.replace(vehicle.id, sourceUri) }
    }

    /** 배경을 지우지 못한 사진에 다시 시도한다. 모델이 없으면 내려받는 것부터 진행 상태로 보여 준다. */
    fun removeBackground() {
        val vehicle = (uiState.value as? HomeUiState.Content)?.vehicle ?: return
        photoRepository.removeBackground(vehicle.id)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val RECENT_RECORD_COUNT = 3
    }
}
