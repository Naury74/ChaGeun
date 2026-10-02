package com.naury.chageun.feature.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.vehicle.CompleteInspectionUseCase
import com.naury.chageun.core.domain.vehicle.InspectionEvaluator
import com.naury.chageun.core.domain.vehicle.InspectionRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.Vehicle
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface VehicleUiState {
    data object Loading : VehicleUiState

    data class Content(
        val vehicle: Vehicle,
        val mileageLog: List<MileageEntry>,
        val inspection: InspectionStatus = InspectionStatus.Unknown,
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
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<VehicleUiState> = vehicleRepository.observePrimaryVehicle()
        .filterNotNull()
        .flatMapLatest { vehicle ->
            combine(
                vehicleRepository.observeMileageLog(vehicle.id),
                inspectionRepository.observeSchedule(vehicle.id),
            ) { log, schedule ->
                VehicleUiState.Content(vehicle, log, InspectionEvaluator.evaluate(schedule, LocalDate.now(clock)))
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

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
