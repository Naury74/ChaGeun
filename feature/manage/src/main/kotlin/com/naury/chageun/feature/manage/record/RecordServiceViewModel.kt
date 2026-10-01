package com.naury.chageun.feature.manage.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.maintenance.MaintenanceRepository
import com.naury.chageun.core.domain.maintenance.RecordServiceResult
import com.naury.chageun.core.domain.maintenance.RecordServiceUseCase
import com.naury.chageun.core.domain.maintenance.ServiceEntryError
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.ServiceEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = RecordServiceViewModel.Factory::class)
class RecordServiceViewModel @AssistedInject constructor(
    @Assisted private val item: MaintenanceItem,
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val recordService: RecordServiceUseCase,
    clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RecordServiceUiState(
            item = item,
            date = savedStateHandle.get<Long>(KEY_DATE)?.let(LocalDate::ofEpochDay) ?: LocalDate.now(clock),
            mileage = savedStateHandle[KEY_MILEAGE] ?: "",
            cost = savedStateHandle[KEY_COST] ?: "",
            shopName = savedStateHandle[KEY_SHOP] ?: "",
            memo = savedStateHandle[KEY_MEMO] ?: "",
        ),
    )
    val uiState: StateFlow<RecordServiceUiState> = _uiState.asStateFlow()

    init {
        if (_uiState.value.mileage.isEmpty()) {
            viewModelScope.launch {
                val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
                val current = maintenanceRepository.findCurrentMileage(vehicle.id) ?: return@launch
                if (_uiState.value.mileage.isEmpty()) onMileageChanged(current.mileage.value.toString())
            }
        }
    }

    fun onDateSelected(date: LocalDate) = edit(RecordServiceField.Date) { copy(date = date) }

    fun onMileageChanged(value: String) =
        edit(RecordServiceField.Mileage) { copy(mileage = value.filter(Char::isDigit).take(MAX_DIGITS)) }

    fun onCostChanged(value: String) = edit(RecordServiceField.Cost) {
        copy(cost = value.filter(Char::isDigit).take(MAX_DIGITS))
    }

    fun onShopNameChanged(value: String) = edit(null) { copy(shopName = value.take(MAX_TEXT_LENGTH)) }

    fun onMemoChanged(value: String) = edit(null) { copy(memo = value.take(MAX_TEXT_LENGTH)) }

    fun save() = submit(isLowerMileageConfirmed = false)

    fun confirmLowerMileage() = submit(isLowerMileageConfirmed = true)

    fun dismissLowerMileageWarning() = _uiState.update { it.copy(lowerMileageWarning = null) }

    private fun submit(isLowerMileageConfirmed: Boolean) {
        val state = _uiState.value
        val mileage = state.mileage.toLongOrNull()
        if (mileage == null) {
            _uiState.update { it.copy(errors = mapOf(RecordServiceField.Mileage to RecordServiceError.Required)) }
            return
        }
        val entry = ServiceEntry(
            item = item,
            date = state.date,
            mileage = Kilometers(mileage),
            costWon = state.cost.toLongOrNull(),
            shopName = state.shopName,
            memo = state.memo,
        )
        _uiState.update { it.copy(isSaving = true, lowerMileageWarning = null, hasSaveFailed = false) }
        viewModelScope.launch {
            val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
            runCatching { recordService(vehicle.id, entry, isLowerMileageConfirmed) }
                .onSuccess(::applyResult)
                .onFailure { _uiState.update { it.copy(isSaving = false, hasSaveFailed = true) } }
        }
    }

    private fun applyResult(result: RecordServiceResult) {
        _uiState.update { state ->
            when (result) {
                is RecordServiceResult.Saved ->
                    state.copy(isSaving = false, savedResult = SavedResult(result.nextDistanceDue, result.nextDateDue))
                is RecordServiceResult.NeedsConfirmation ->
                    state.copy(isSaving = false, lowerMileageWarning = result.previousMileage)
                is RecordServiceResult.Rejected -> state.copy(
                    isSaving = false,
                    errors = result.errors.associate { error ->
                        when (error) {
                            ServiceEntryError.FutureDate -> RecordServiceField.Date to RecordServiceError.FutureDate
                            ServiceEntryError.NegativeCost ->
                                RecordServiceField.Cost to
                                    RecordServiceError.InvalidNumber
                        }
                    },
                )
            }
        }
    }

    private fun edit(field: RecordServiceField?, transform: RecordServiceUiState.() -> RecordServiceUiState) {
        _uiState.update { state ->
            state.transform().copy(errors = field?.let { state.errors - it } ?: state.errors, hasSaveFailed = false)
        }
        val state = _uiState.value
        savedStateHandle[KEY_DATE] = state.date.toEpochDay()
        savedStateHandle[KEY_MILEAGE] = state.mileage
        savedStateHandle[KEY_COST] = state.cost
        savedStateHandle[KEY_SHOP] = state.shopName
        savedStateHandle[KEY_MEMO] = state.memo
    }

    @AssistedFactory
    interface Factory {
        fun create(item: MaintenanceItem): RecordServiceViewModel
    }

    private companion object {
        const val KEY_DATE = "record_date"
        const val KEY_MILEAGE = "record_mileage"
        const val KEY_COST = "record_cost"
        const val KEY_SHOP = "record_shop"
        const val KEY_MEMO = "record_memo"
        const val MAX_DIGITS = 9
        const val MAX_TEXT_LENGTH = 200
    }
}
