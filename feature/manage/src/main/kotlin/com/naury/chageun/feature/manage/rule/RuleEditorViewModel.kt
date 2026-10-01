package com.naury.chageun.feature.manage.rule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.maintenance.EditMaintenanceRuleUseCase
import com.naury.chageun.core.domain.maintenance.MaintenanceRepository
import com.naury.chageun.core.domain.maintenance.RuleEditError
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RuleSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RuleEditorUiState(
    val item: MaintenanceItem,
    val isLoading: Boolean = true,
    val intervalKm: String = "",
    val intervalMonths: String = "",
    val isEnabled: Boolean = true,
    val source: RuleSource = RuleSource.Generic,
    val error: RuleEditError? = null,
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
)

@HiltViewModel(assistedFactory = RuleEditorViewModel.Factory::class)
class RuleEditorViewModel @AssistedInject constructor(
    @Assisted private val item: MaintenanceItem,
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val editRule: EditMaintenanceRuleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RuleEditorUiState(item))
    val uiState: StateFlow<RuleEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val rule = maintenanceRepository.findRule(vehicleId(), item)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    intervalKm = savedStateHandle[KEY_KM] ?: rule?.intervalKm?.toString().orEmpty(),
                    intervalMonths = savedStateHandle[KEY_MONTHS] ?: rule?.intervalMonths?.toString().orEmpty(),
                    isEnabled = savedStateHandle[KEY_ENABLED] ?: rule?.isEnabled ?: true,
                    source = rule?.source ?: RuleSource.Generic,
                )
            }
        }
    }

    fun onIntervalKmChanged(value: String) = edit { copy(intervalKm = value.filter(Char::isDigit).take(MAX_DIGITS)) }

    fun onIntervalMonthsChanged(value: String) = edit {
        copy(intervalMonths = value.filter(Char::isDigit).take(MAX_DIGITS))
    }

    fun onEnabledChanged(enabled: Boolean) = edit { copy(isEnabled = enabled) }

    fun save() = run {
        val state = _uiState.value
        editRule.update(
            vehicleId = vehicleId(),
            item = item,
            intervalKm = state.intervalKm.toLongOrNull(),
            intervalMonths = state.intervalMonths.toLongOrNull(),
            isEnabled = state.isEnabled,
        )
    }

    fun resetToGeneric() = run { editRule.resetToGeneric(vehicleId(), item) }

    private fun run(action: suspend () -> RuleEditError?) {
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val error = action()
            _uiState.update { it.copy(isSaving = false, error = error, isDone = error == null) }
        }
    }

    private fun edit(transform: RuleEditorUiState.() -> RuleEditorUiState) {
        _uiState.update { it.transform().copy(error = null) }
        val state = _uiState.value
        savedStateHandle[KEY_KM] = state.intervalKm
        savedStateHandle[KEY_MONTHS] = state.intervalMonths
        savedStateHandle[KEY_ENABLED] = state.isEnabled
    }

    private suspend fun vehicleId() = vehicleRepository.observePrimaryVehicle().filterNotNull().first().id

    @AssistedFactory
    interface Factory {
        fun create(item: MaintenanceItem): RuleEditorViewModel
    }

    private companion object {
        const val KEY_KM = "rule_interval_km"
        const val KEY_MONTHS = "rule_interval_months"
        const val KEY_ENABLED = "rule_enabled"
        const val MAX_DIGITS = 6
    }
}
