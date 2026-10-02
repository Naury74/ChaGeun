package com.naury.chageun.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.maintenance.UpdateMileageResult
import com.naury.chageun.core.domain.maintenance.UpdateMileageUseCase
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.Kilometers
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
    val isMissing: Boolean = false,
    val lowerThan: Kilometers? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

@HiltViewModel
class MileageUpdateViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val updateMileage: UpdateMileageUseCase,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MileageUpdateUiState(mileage = savedStateHandle[KEY_MILEAGE] ?: ""))
    val uiState: StateFlow<MileageUpdateUiState> = _uiState.asStateFlow()

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
