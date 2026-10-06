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
import java.time.Clock
import java.time.LocalDate
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val historyRepository: HistoryRepository,
    private val attachmentRepository: AttachmentRepository,
    private val deleteRecord: DeleteHistoryRecordUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val attachFailedCount = MutableStateFlow(0)

    // 기록이 많아도 첫 화면이 빨리 뜨도록 한 번에 PAGE_SIZE개씩 늘려 읽는다. 조건이 바뀌면 첫 페이지로 돌아간다.
    private val pageLimit = MutableStateFlow(PAGE_SIZE)

    private val filter = savedStateHandle.getStateFlow(KEY_FILTER, HistoryFilter.All.name)
    private val keyword = savedStateHandle.getStateFlow(KEY_KEYWORD, "")
    private val matchingItems = savedStateHandle.getStateFlow(KEY_MATCHING_ITEMS, arrayListOf<String>())
    private val selected = savedStateHandle.getStateFlow<String?>(KEY_SELECTED, null)
    private val advanced = savedStateHandle.getStateFlow<String?>(KEY_ADVANCED, null)

    val uiState: StateFlow<HistoryUiState> = vehicleRepository.observePrimaryVehicle()
        .filterNotNull()
        .flatMapLatest { vehicle ->
            val query = combine(filter, keyword, matchingItems, advanced) { filterName, text, items, encoded ->
                val advancedFilter = AdvancedFilter.decode(encoded)
                val base = TimelineQuery(
                    types = HistoryFilter.valueOf(filterName).types,
                    keyword = text,
                    matchingItems = items.map(MaintenanceItem::valueOf).toSet(),
                )
                Triple(
                    HistoryFilter.valueOf(filterName),
                    advancedFilter,
                    advancedFilter.applyTo(base, LocalDate.now(clock)),
                )
            }
            val timeline = query
                .onEach { pageLimit.value = PAGE_SIZE }
                .flatMapLatest { (activeFilter, advancedFilter, timelineQuery) ->
                    combine(
                        pageLimit.flatMapLatest { limit ->
                            historyRepository.observeTimeline(vehicle.id, timelineQuery, limit)
                                .map { items -> TimelinePage(items, hasMore = items.size >= limit) }
                        },
                        historyRepository.observeMonthlyCosts(vehicle.id, timelineQuery),
                    ) { page, totals -> Triple(activeFilter, advancedFilter, page to totals) }
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
                    (activeFilter, advancedFilter, pageAndTotals),
                    text,
                    (record, attachments),
                    failed,
                ->
                val (page, totals) = pageAndTotals
                HistoryUiState(
                    isLoading = false,
                    filter = activeFilter,
                    advanced = advancedFilter,
                    keyword = text,
                    sections = page.items.groupIntoMonths(totals),
                    hasMore = page.hasMore,
                    selected = record?.ref,
                    detail = record,
                    attachments = if (record != null) attachments else emptyList(),
                    attachFailedCount = failed,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState())

    /** 목록 끝 근처에 닿으면 부른다. 이미 읽는 중이어도 한 페이지씩만 늘린다. */
    fun loadMore() {
        val state = uiState.value
        if (!state.hasMore) return
        val loaded = state.sections.sumOf { it.items.size }
        pageLimit.update { current -> if (loaded >= current) current + PAGE_SIZE else current }
    }

    fun applyAdvancedFilter(filter: AdvancedFilter) {
        savedStateHandle[KEY_ADVANCED] = filter.encode()
    }

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

    fun attach(ref: RecordRef, sourceUris: List<String>, highQuality: Boolean = false) {
        if (sourceUris.isEmpty()) return
        viewModelScope.launch {
            attachFailedCount.value = attachmentRepository.attach(vehicleId(), ref, sourceUris, highQuality).failed
        }
    }

    fun deleteAttachment(attachmentId: String) {
        viewModelScope.launch { attachmentRepository.delete(vehicleId(), attachmentId) }
    }

    fun dismissAttachFailure() {
        attachFailedCount.value = 0
    }

    private suspend fun vehicleId() = vehicleRepository.observePrimaryVehicle().filterNotNull().first().id

    private fun List<TimelineItem>.groupIntoMonths(totals: Map<YearMonth?, Long>): List<TimelineSection> =
        groupBy { item -> item.date?.let(YearMonth::from) }
            .map { (month, items) -> TimelineSection(month, items, totals[month] ?: 0) }

    private companion object {
        const val KEY_FILTER = "history_filter"
        const val KEY_KEYWORD = "history_keyword"
        const val KEY_MATCHING_ITEMS = "history_matching_items"
        const val KEY_SELECTED = "history_selected"
        const val KEY_ADVANCED = "history_advanced"
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val PAGE_SIZE = 50
    }
}

/** SavedStateHandle과 rememberSaveable에 "종류:ID"로 보관한 기록 참조를 되돌린다. */
internal fun String.toRecordRef(): RecordRef? {
    val type = substringBefore(':').let { name -> TimelineEventType.entries.firstOrNull { it.name == name } }
    return type?.let { RecordRef(it, substringAfter(':')) }
}

private data class TimelinePage(val items: List<TimelineItem>, val hasMore: Boolean)
