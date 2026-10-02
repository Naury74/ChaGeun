package com.naury.chageun.feature.history.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.history.AddHistoryRecordUseCase
import com.naury.chageun.core.domain.history.FuelAmountCalculator
import com.naury.chageun.core.domain.history.HistoryEntryError
import com.naury.chageun.core.domain.maintenance.MaintenanceRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.Kilometers
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FormError { Required, FutureDate, Amounts }

data class FuelFormUiState(
    val date: LocalDate,
    val mileage: String = "",
    val total: String = "",
    val volumeLitres: String = "",
    val unitPrice: String = "",
    val isFullTank: Boolean = true,
    val stationName: String = "",
    val memo: String = "",
    val errors: Set<FormError> = emptySet(),
    val isSaving: Boolean = false,
    val hasSaveFailed: Boolean = false,
    val isSaved: Boolean = false,
) {
    /** 저장 전에 어떤 값이 저장될지 보이도록 계산된 양을 실시간으로 미리 보여준다. */
    val amounts: FuelAmounts? get() = FuelAmountCalculator.complete(
        total.toLongOrNull(),
        volumeLitres.toMillilitres(),
        unitPrice.toLongOrNull(),
    )
}

internal fun String.toMillilitres(): Long? = toBigDecimalOrNull()
    ?.multiply(BigDecimal(ML_PER_LITRE))
    ?.setScale(0, RoundingMode.HALF_UP)
    ?.toLong()
    ?.takeIf { it > 0 }

private const val ML_PER_LITRE = 1_000

@HiltViewModel
class FuelFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val addRecord: AddHistoryRecordUseCase,
    private val analytics: AnalyticsTracker,
    clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FuelFormUiState(
            date = savedStateHandle.get<Long>(KEY_DATE)?.let(LocalDate::ofEpochDay) ?: LocalDate.now(clock),
            mileage = savedStateHandle[KEY_MILEAGE] ?: "",
            total = savedStateHandle[KEY_TOTAL] ?: "",
            volumeLitres = savedStateHandle[KEY_VOLUME] ?: "",
            unitPrice = savedStateHandle[KEY_UNIT] ?: "",
            isFullTank = savedStateHandle[KEY_FULL] ?: true,
            stationName = savedStateHandle[KEY_STATION] ?: "",
            memo = savedStateHandle[KEY_MEMO] ?: "",
        ),
    )
    val uiState: StateFlow<FuelFormUiState> = _uiState.asStateFlow()

    init {
        if (_uiState.value.mileage.isEmpty()) {
            viewModelScope.launch {
                val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
                maintenanceRepository.findCurrentMileage(vehicle.id)?.let { current ->
                    if (_uiState.value.mileage.isEmpty()) edit { copy(mileage = current.mileage.value.toString()) }
                }
            }
        }
    }

    fun onDateSelected(date: LocalDate) = edit { copy(date = date) }

    fun onMileageChanged(value: String) = edit { copy(mileage = value.digits()) }

    fun onTotalChanged(value: String) = edit { copy(total = value.digits()) }

    fun onVolumeChanged(value: String) = edit {
        copy(volumeLitres = value.filter { it.isDigit() || it == '.' }.take(MAX_LENGTH))
    }

    fun onUnitPriceChanged(value: String) = edit { copy(unitPrice = value.digits()) }

    fun onFullTankChanged(value: Boolean) = edit { copy(isFullTank = value) }

    fun onStationChanged(value: String) = edit { copy(stationName = value.take(MAX_TEXT_LENGTH)) }

    fun onMemoChanged(value: String) = edit { copy(memo = value.take(MAX_TEXT_LENGTH)) }

    fun save() {
        val state = _uiState.value
        val mileage = state.mileage.toLongOrNull()
        val amounts = state.amounts
        val errors = buildSet {
            if (mileage == null) add(FormError.Required)
            if (amounts == null) add(FormError.Amounts)
        }
        if (mileage == null || amounts == null) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        val entry = FuelEntry(state.date, Kilometers(mileage), amounts, state.isFullTank, state.stationName, state.memo)
        _uiState.update { it.copy(isSaving = true, hasSaveFailed = false) }
        viewModelScope.launch {
            val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
            runCatching { addRecord.addFuel(vehicle.id, entry) }
                .onSuccess { result ->
                    if (result.isEmpty()) analytics.track(AnalyticsEvent.FuelRecordAdded)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSaved = result.isEmpty(),
                            errors = result.map(HistoryEntryError::asFormError).toSet(),
                        )
                    }
                }
                .onFailure { _uiState.update { it.copy(isSaving = false, hasSaveFailed = true) } }
        }
    }

    private fun edit(transform: FuelFormUiState.() -> FuelFormUiState) {
        _uiState.update { it.transform().copy(errors = emptySet(), hasSaveFailed = false) }
        val state = _uiState.value
        savedStateHandle[KEY_DATE] = state.date.toEpochDay()
        savedStateHandle[KEY_MILEAGE] = state.mileage
        savedStateHandle[KEY_TOTAL] = state.total
        savedStateHandle[KEY_VOLUME] = state.volumeLitres
        savedStateHandle[KEY_UNIT] = state.unitPrice
        savedStateHandle[KEY_FULL] = state.isFullTank
        savedStateHandle[KEY_STATION] = state.stationName
        savedStateHandle[KEY_MEMO] = state.memo
    }

    private fun String.digits() = filter(Char::isDigit).take(MAX_LENGTH)

    private companion object {
        const val KEY_DATE = "fuel_date"
        const val KEY_MILEAGE = "fuel_mileage"
        const val KEY_TOTAL = "fuel_total"
        const val KEY_VOLUME = "fuel_volume"
        const val KEY_UNIT = "fuel_unit"
        const val KEY_FULL = "fuel_full"
        const val KEY_STATION = "fuel_station"
        const val KEY_MEMO = "fuel_memo"
        const val MAX_LENGTH = 9
        const val MAX_TEXT_LENGTH = 200
    }
}

internal fun HistoryEntryError.asFormError(): FormError = when (this) {
    HistoryEntryError.FutureDate -> FormError.FutureDate
    HistoryEntryError.MissingTitle, HistoryEntryError.NegativeCost -> FormError.Required
}
