package com.naury.chageun.feature.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface VehicleUiState {
    data object Loading : VehicleUiState

    data class Content(val vehicle: Vehicle, val mileageLog: List<MileageEntry>) : VehicleUiState {
        val currentMileage: MileageEntry? get() = mileageLog.firstOrNull()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VehicleViewModel @Inject constructor(vehicleRepository: VehicleRepository) : ViewModel() {

    val uiState: StateFlow<VehicleUiState> = vehicleRepository.observePrimaryVehicle()
        .filterNotNull()
        .flatMapLatest { vehicle ->
            vehicleRepository.observeMileageLog(vehicle.id).map { log -> VehicleUiState.Content(vehicle, log) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), VehicleUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
