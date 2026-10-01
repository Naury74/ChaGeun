package com.naury.chageun.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class AppEntry { Loading, Onboarding, Main }

@HiltViewModel
class AppViewModel @Inject constructor(vehicleRepository: VehicleRepository) : ViewModel() {

    val entry: StateFlow<AppEntry> = vehicleRepository.observePrimaryVehicle()
        .map { vehicle -> if (vehicle == null) AppEntry.Onboarding else AppEntry.Main }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppEntry.Loading)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
