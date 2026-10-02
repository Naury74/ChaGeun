package com.naury.chageun.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.history.AttachmentRepository
import com.naury.chageun.core.domain.history.DeleteHistoryRecordUseCase
import com.naury.chageun.core.domain.history.HistoryRepository
import com.naury.chageun.core.domain.history.TimelineQuery
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val historyRepository: HistoryRepository,
    private val attachmentRepository: AttachmentRepository,
    private val deleteRecord: DeleteHistoryRecordUseCase,
) : ViewModel() {

    private val attachFailedCount = MutableStateFlow(0)

    private val filter = savedStateHandle.getStateFlow(KEY_FILTER, HistoryFilter.All.name)
    private val keyword = savedStateHandle.getStateFlow(KEY_KEYWORD, "")
    private val matchingItems = savedStateHandle.getStateFlow(KEY_MATCHING_ITEMS, arrayListOf<String>())
    private val selected = savedStateHandle.getStateFlow<String?>(KEY_SELECTED, null)

    val uiState: StateFlow<HistoryUiState> = vehicleRepository.observePrimaryVehicle()
        .filterNotNull()
        .flatMapLatest { vehicle ->
            val query = combine(filter, keyword, matchingItems) { filterName, text, items ->
                HistoryFilter.valueOf(filterName) to TimelineQuery(
                    types = HistoryFilter.valueOf(filterName).types,
                    keyword = text,
                    matchingItems = items.map(MaintenanceItem::valueOf).toSet(),
                )
            }
            val timeline = query.flatMapLatest { (activeFilter, timelineQuery) ->
                historyRepository.observeTimeline(vehicle.id, timelineQuery).map { items -> activeFilter to items }
            }
            val detail = selected.flatMapLatest { key ->
                key?.toRecordRef()?.let { ref ->
                    combine(
                        historyRepository.observeRecord(vehicle.id, ref),
                        attachmentRepository.observe(vehicle.id, ref),
                    ) { record, attachments -> record to attachments }
                } ?: flowOf(null to emptyList())
            }
            combine(timeline, keyword, detail, attachFailedCount) {
                    (activeFilter, items),
                    text,
                    (record, attachments),
                    failed,
                ->
                HistoryUiState(
                    isLoading = false,
                    filter = activeFilter,
                    keyword = text,
                    sections = items.groupIntoMonths(),
                    selected = record?.ref,
                    detail = record,
                    attachments = if (record != null) attachments else emptyList(),
                    attachFailedCount = failed,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState())

    fun selectFilter(filter: HistoryFilter) {
        savedStateHandle[KEY_FILTER] = filter.name
    }

    /** [matchingItems]는 현지화된 이름에 [keyword]가 포함된 정비 항목이다. */
    fun search(keyword: String, matchingItems: Set<MaintenanceItem>) {
        savedStateHandle[KEY_KEYWORD] = keyword
        savedStateHandle[KEY_MATCHING_ITEMS] = ArrayList(matchingItems.map { it.name })
    }

    fun select(ref: RecordRef?) {
        savedStateHandle[KEY_SELECTED] = ref?.let { "${it.type.name}:${it.id}" }
    }

    fun delete(ref: RecordRef) {
        viewModelScope.launch {
            deleteRecord(vehicleId(), ref)
            if (selected.value == "${ref.type.name}:${ref.id}") select(null)
        }
    }

    fun attach(ref: RecordRef, sourceUris: List<String>) {
        if (sourceUris.isEmpty()) return
        viewModelScope.launch {
            attachFailedCount.value = attachmentRepository.attach(vehicleId(), ref, sourceUris).failed
        }
    }

    fun deleteAttachment(attachmentId: String) {
        viewModelScope.launch { attachmentRepository.delete(vehicleId(), attachmentId) }
    }

    fun dismissAttachFailure() {
        attachFailedCount.value = 0
    }

    private suspend fun vehicleId() = vehicleRepository.observePrimaryVehicle().filterNotNull().first().id

    private fun String.toRecordRef(): RecordRef? {
        val type = substringBefore(':').let { name -> TimelineEventType.entries.firstOrNull { it.name == name } }
        return type?.let { RecordRef(it, substringAfter(':')) }
    }

    private fun List<TimelineItem>.groupIntoMonths(): List<TimelineSection> =
        groupBy { item -> item.date?.let(YearMonth::from) }
            .map { (month, items) -> TimelineSection(month, items) }

    private companion object {
        const val KEY_FILTER = "history_filter"
        const val KEY_KEYWORD = "history_keyword"
        const val KEY_MATCHING_ITEMS = "history_matching_items"
        const val KEY_SELECTED = "history_selected"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
