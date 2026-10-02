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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    vehicleRepository: VehicleRepository,
    observeMaintenanceOverview: ObserveMaintenanceOverviewUseCase,
    historyRepository: HistoryRepository,
    photoRepository: VehiclePhotoRepository,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = vehicleRepository.observePrimaryVehicle()
        .filterNotNull()
        .flatMapLatest { vehicle ->
            combine(
                observeMaintenanceOverview(vehicle.id),
                historyRepository.observeTimeline(vehicle.id, TimelineQuery()).map { it.take(RECENT_RECORD_COUNT) },
                photoRepository.observe(vehicle.id),
            ) { overview, recent, photo ->
                HomeUiState.Content(
                    vehicle = vehicle,
                    overview = overview,
                    today = LocalDate.now(clock),
                    recentRecords = recent,
                    photoPath = photo,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val RECENT_RECORD_COUNT = 3
    }
}
