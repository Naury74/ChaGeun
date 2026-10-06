package com.naury.chageun.feature.manage.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.history.EditHistoryRecordUseCase
import com.naury.chageun.core.domain.history.HistoryEntryError
import com.naury.chageun.core.domain.maintenance.RecordServiceResult
import com.naury.chageun.core.domain.maintenance.RecordServiceUseCase
import com.naury.chageun.core.domain.maintenance.ServiceEntryError
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.TimelineEventType
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
    @Assisted target: RecordServiceTarget,
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val recordService: RecordServiceUseCase,
    private val editRecord: EditHistoryRecordUseCase,
    clock: Clock,
) : ViewModel() {

    private val item = target.item
    private val editingRecordId = target.editingRecordId

    private val _uiState = MutableStateFlow(
        RecordServiceUiState(
            item = item,
            date = savedStateHandle.get<Long>(KEY_DATE)?.let(LocalDate::ofEpochDay) ?: LocalDate.now(clock),
            mileage = savedStateHandle[KEY_MILEAGE] ?: "",
            cost = savedStateHandle[KEY_COST] ?: "",
            shopName = savedStateHandle[KEY_SHOP] ?: "",
            memo = savedStateHandle[KEY_MEMO] ?: "",
            alsoReplaced = savedStateHandle.get<ArrayList<String>>(KEY_ALSO_REPLACED).orEmpty()
                .mapNotNull { name -> MaintenanceItem.entries.firstOrNull { it.name == name } }
                .toSet(),
            isEditing = editingRecordId != null,
        ),
    )
    val uiState: StateFlow<RecordServiceUiState> = _uiState.asStateFlow()

    init {
        if (editingRecordId != null) {
            loadEditingRecord(editingRecordId)
        } else {
            viewModelScope.launch {
                val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
                val candidates = recordService.companionCandidates(vehicle.id, item)
                _uiState.update { it.copy(companionCandidates = candidates) }
                if (_uiState.value.mileage.isNotEmpty()) return@launch
                val current = recordService.currentMileage(vehicle.id) ?: return@launch
                if (_uiState.value.mileage.isEmpty()) onMileageChanged(current.value.toString())
            }
        }
    }

    /** 화면 회전이나 접기 뒤에는 이미 채운 값을 그대로 쓴다. */
    private fun loadEditingRecord(recordId: String) {
        if (savedStateHandle.get<Boolean>(KEY_EDIT_LOADED) == true) return
        viewModelScope.launch {
            val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
            val ref = RecordRef(TimelineEventType.Maintenance, recordId)
            val detail = editRecord.load(vehicle.id, ref) as? RecordDetail.Maintenance
                ?: return@launch
            savedStateHandle[KEY_EDIT_LOADED] = true
            val entry = detail.entry
            edit(null) {
                copy(
                    date = entry.date ?: date,
                    mileage = entry.mileage?.value?.toString().orEmpty(),
                    cost = entry.costWon?.toString().orEmpty(),
                    shopName = entry.shopName.orEmpty(),
                    memo = detail.memo.orEmpty(),
                )
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

    fun toggleAlsoReplaced(companion: MaintenanceItem) {
        val selected = _uiState.value.alsoReplaced.let { if (companion in it) it - companion else it + companion }
        savedStateHandle[KEY_ALSO_REPLACED] = ArrayList(selected.map { it.name })
        _uiState.update { it.copy(alsoReplaced = selected) }
    }

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
        if (editingRecordId != null) {
            saveEdit(editingRecordId, entry)
            return
        }
        viewModelScope.launch {
            val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
            runCatching { recordService(vehicle.id, entry, isLowerMileageConfirmed, state.alsoReplaced) }
                .onSuccess(::applyResult)
                .onFailure { _uiState.update { it.copy(isSaving = false, hasSaveFailed = true) } }
        }
    }

    /** 고칠 때는 같은 기록과 비교하게 되므로 이전 정비보다 낮은 값인지 확인하지 않는다. */
    private fun saveEdit(recordId: String, entry: ServiceEntry) {
        viewModelScope.launch {
            val vehicle = vehicleRepository.observePrimaryVehicle().filterNotNull().first()
            runCatching { editRecord.updateService(vehicle.id, recordId, entry) }
                .onSuccess { errors ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            isEditSaved = errors.isEmpty(),
                            errors = errors.mapNotNull { error ->
                                when (error) {
                                    HistoryEntryError.FutureDate ->
                                        RecordServiceField.Date to RecordServiceError.FutureDate
                                    HistoryEntryError.NegativeCost ->
                                        RecordServiceField.Cost to RecordServiceError.InvalidNumber
                                    HistoryEntryError.MissingTitle -> null
                                }
                            }.toMap(),
                        )
                    }
                }
                .onFailure { _uiState.update { it.copy(isSaving = false, hasSaveFailed = true) } }
        }
    }

    private fun applyResult(result: RecordServiceResult) {
        _uiState.update { state ->
            when (result) {
                is RecordServiceResult.Saved ->
                    state.copy(
                        isSaving = false,
                        savedResult = SavedResult(result.nextDistanceDue, result.nextDateDue, result.alsoReplaced),
                    )
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
        fun create(target: RecordServiceTarget): RecordServiceViewModel
    }

    private companion object {
        const val KEY_DATE = "record_date"
        const val KEY_MILEAGE = "record_mileage"
        const val KEY_COST = "record_cost"
        const val KEY_SHOP = "record_shop"
        const val KEY_MEMO = "record_memo"
        const val KEY_ALSO_REPLACED = "record_also_replaced"
        const val KEY_EDIT_LOADED = "record_edit_loaded"
        const val MAX_DIGITS = 9
        const val MAX_TEXT_LENGTH = 200
    }
}
