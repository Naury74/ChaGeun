package com.naury.chageun.feature.history.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.history.AddHistoryRecordUseCase
import com.naury.chageun.core.domain.history.EditHistoryRecordUseCase
import com.naury.chageun.core.domain.history.HistoryEntryError
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
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
    /** 저장된 기록을 고치는 중이다. 제목과 저장 동작만 다르다. */
    val isEditing: Boolean = false,
)

@HiltViewModel
class CheckFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val addRecord: AddHistoryRecordUseCase,
    private val editRecord: EditHistoryRecordUseCase,
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
            isEditing = savedStateHandle.get<String>(KEY_EDIT_ID) != null,
        ),
    )
    val uiState: StateFlow<CheckFormUiState> = _uiState.asStateFlow()

    /** 저장된 검사·수리·메모 기록을 폼에 채운다. 다시 불려도 사용자가 고친 값을 덮어쓰지 않는다. */
    fun startEditing(ref: RecordRef) {
        if (savedStateHandle.get<String>(KEY_EDIT_ID) == ref.id) return
        viewModelScope.launch {
            val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
            val entry = (editRecord.load(vehicle.id, ref) as? RecordDetail.Check)?.entry
                ?: return@launch
            savedStateHandle[KEY_EDIT_ID] = ref.id
            edit {
                copy(
                    kind = entry.kind,
                    date = entry.date,
                    title = entry.title,
                    mileage = entry.mileage?.value?.toString().orEmpty(),
                    cost = entry.costWon?.toString().orEmpty(),
                    memo = entry.memo.orEmpty(),
                    isEditing = true,
                )
            }
        }
    }

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
            val editingId = savedStateHandle.get<String>(KEY_EDIT_ID)
            runCatching {
                if (editingId != null) {
                    editRecord.updateCheck(vehicle.id, editingId, entry)
                } else {
                    addRecord.addCheck(vehicle.id, entry)
                }
            }
                .onSuccess { result ->
                    if (result.isEmpty() && editingId == null) analytics.track(AnalyticsEvent.CheckRecordAdded)
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
        const val KEY_EDIT_ID = "check_edit_id"
        const val MAX_DIGITS = 9
        const val MAX_TITLE_LENGTH = 60
        const val MAX_MEMO_LENGTH = 500
    }
}
