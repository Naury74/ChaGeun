package com.naury.chageun.feature.history.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.history.AddHistoryRecordUseCase
import com.naury.chageun.core.domain.history.EditHistoryRecordUseCase
import com.naury.chageun.core.domain.history.FuelAmountCalculator
import com.naury.chageun.core.domain.history.HistoryEntryError
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
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
    /** 저장된 기록을 고치는 중이다. 제목과 저장 동작만 다르다. */
    val isEditing: Boolean = false,
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
    private val addRecord: AddHistoryRecordUseCase,
    private val editRecord: EditHistoryRecordUseCase,
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
            isEditing = savedStateHandle.get<String>(KEY_EDIT_ID) != null,
        ),
    )
    val uiState: StateFlow<FuelFormUiState> = _uiState.asStateFlow()

    init {
        if (_uiState.value.mileage.isEmpty()) {
            viewModelScope.launch {
                val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
                addRecord.currentMileage(vehicle.id)?.let { current ->
                    if (_uiState.value.mileage.isEmpty()) edit { copy(mileage = current.value.toString()) }
                }
            }
        }
    }

    /**
     * 저장된 주유 기록을 폼에 채운다. 화면 회전이나 접기 뒤 다시 불려도 사용자가 고친 값을 덮어쓰지 않는다.
     * 자동으로 계산된 칸은 비워 두어, 사용자가 입력했던 두 값에서 다시 계산되게 한다.
     */
    fun startEditing(recordId: String) {
        if (savedStateHandle.get<String>(KEY_EDIT_ID) == recordId) return
        viewModelScope.launch {
            val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
            val ref = RecordRef(TimelineEventType.Fuel, recordId)
            val entry = (editRecord.load(vehicle.id, ref) as? RecordDetail.Fuel)?.entry
                ?: return@launch
            val amounts = entry.amounts
            savedStateHandle[KEY_EDIT_ID] = recordId
            edit {
                copy(
                    date = entry.date,
                    mileage = entry.mileage.value.toString(),
                    total = amounts.totalPriceWon.takeIf { amounts.computedField != FuelField.Total }.asInput(),
                    volumeLitres = amounts.volumeMl.takeIf { amounts.computedField != FuelField.Volume }
                        ?.let { BigDecimal(it).movePointLeft(LITRE_SCALE).stripTrailingZeros().toPlainString() }
                        .orEmpty(),
                    unitPrice = amounts.unitPriceWon.takeIf { amounts.computedField != FuelField.UnitPrice }.asInput(),
                    isFullTank = entry.isFullTank,
                    stationName = entry.stationName.orEmpty(),
                    memo = entry.memo.orEmpty(),
                    isEditing = true,
                )
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
            val editingId = savedStateHandle.get<String>(KEY_EDIT_ID)
            runCatching {
                if (editingId != null) {
                    editRecord.updateFuel(vehicle.id, editingId, entry)
                } else {
                    addRecord.addFuel(vehicle.id, entry)
                }
            }
                .onSuccess { result ->
                    if (result.isEmpty() && editingId == null) analytics.track(AnalyticsEvent.FuelRecordAdded)
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

    private fun Long?.asInput() = this?.toString().orEmpty()

    private companion object {
        const val KEY_DATE = "fuel_date"
        const val KEY_MILEAGE = "fuel_mileage"
        const val KEY_TOTAL = "fuel_total"
        const val KEY_VOLUME = "fuel_volume"
        const val KEY_UNIT = "fuel_unit"
        const val KEY_FULL = "fuel_full"
        const val KEY_STATION = "fuel_station"
        const val KEY_MEMO = "fuel_memo"
        const val KEY_EDIT_ID = "fuel_edit_id"
        const val LITRE_SCALE = 3
        const val MAX_LENGTH = 9
        const val MAX_TEXT_LENGTH = 200
    }
}

internal fun HistoryEntryError.asFormError(): FormError = when (this) {
    HistoryEntryError.FutureDate -> FormError.FutureDate
    HistoryEntryError.MissingTitle, HistoryEntryError.NegativeCost -> FormError.Required
}
