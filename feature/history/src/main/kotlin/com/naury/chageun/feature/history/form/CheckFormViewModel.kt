package com.naury.chageun.feature.history.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.history.AddHistoryRecordUseCase
import com.naury.chageun.core.domain.history.HistoryEntryError
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.Kilometers
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class CheckFormUiState(
    val kind: CheckKind = CheckKind.Inspection,
    val date: LocalDate,
    val title: String = "",
    val mileage: String = "",
    val cost: String = "",
    val memo: String = "",
    val errors: Set<FormError> = emptySet(),
    val isSaving: Boolean = false,
    val hasSaveFailed: Boolean = false,
    val isSaved: Boolean = false,
)

@HiltViewModel
class CheckFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val addRecord: AddHistoryRecordUseCase,
    private val analytics: AnalyticsTracker,
    clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CheckFormUiState(
            kind = savedStateHandle.get<String>(KEY_KIND)?.let(CheckKind::valueOf) ?: CheckKind.Inspection,
            date = savedStateHandle.get<Long>(KEY_DATE)?.let(LocalDate::ofEpochDay) ?: LocalDate.now(clock),
            title = savedStateHandle[KEY_TITLE] ?: "",
            mileage = savedStateHandle[KEY_MILEAGE] ?: "",
            cost = savedStateHandle[KEY_COST] ?: "",
            memo = savedStateHandle[KEY_MEMO] ?: "",
        ),
    )
    val uiState: StateFlow<CheckFormUiState> = _uiState.asStateFlow()

    fun onKindSelected(kind: CheckKind) = edit { copy(kind = kind) }

    fun onDateSelected(date: LocalDate) = edit { copy(date = date) }

    fun onTitleChanged(value: String) = edit { copy(title = value.take(MAX_TITLE_LENGTH)) }

    fun onMileageChanged(value: String) = edit { copy(mileage = value.filter(Char::isDigit).take(MAX_DIGITS)) }

    fun onCostChanged(value: String) = edit { copy(cost = value.filter(Char::isDigit).take(MAX_DIGITS)) }

    fun onMemoChanged(value: String) = edit { copy(memo = value.take(MAX_MEMO_LENGTH)) }

    fun save() {
        val state = _uiState.value
        val entry = CheckEntry(
            kind = state.kind,
            date = state.date,
            title = state.title,
            mileage = state.mileage.toLongOrNull()?.let(::Kilometers),
            costWon = state.cost.toLongOrNull(),
            memo = state.memo,
        )
        _uiState.update { it.copy(isSaving = true, hasSaveFailed = false) }
        viewModelScope.launch {
            val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
            runCatching { addRecord.addCheck(vehicle.id, entry) }
                .onSuccess { result ->
                    if (result.isEmpty()) analytics.track(AnalyticsEvent.CheckRecordAdded)
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

    private fun edit(transform: CheckFormUiState.() -> CheckFormUiState) {
        _uiState.update { it.transform().copy(errors = emptySet(), hasSaveFailed = false) }
        val state = _uiState.value
        savedStateHandle[KEY_KIND] = state.kind.name
        savedStateHandle[KEY_DATE] = state.date.toEpochDay()
        savedStateHandle[KEY_TITLE] = state.title
        savedStateHandle[KEY_MILEAGE] = state.mileage
        savedStateHandle[KEY_COST] = state.cost
        savedStateHandle[KEY_MEMO] = state.memo
    }

    private companion object {
        const val KEY_KIND = "check_kind"
        const val KEY_DATE = "check_date"
        const val KEY_TITLE = "check_title"
        const val KEY_MILEAGE = "check_mileage"
        const val KEY_COST = "check_cost"
        const val KEY_MEMO = "check_memo"
        const val MAX_DIGITS = 9
        const val MAX_TITLE_LENGTH = 60
        const val MAX_MEMO_LENGTH = 500
    }
}
