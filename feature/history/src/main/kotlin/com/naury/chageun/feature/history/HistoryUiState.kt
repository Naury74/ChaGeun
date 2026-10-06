package com.naury.chageun.feature.history

import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import java.time.YearMonth

enum class HistoryFilter(val types: Set<TimelineEventType>) {
    All(TimelineEventType.entries.toSet()),
    Maintenance(setOf(TimelineEventType.Maintenance)),
    Fuel(setOf(TimelineEventType.Fuel)),
    Check(setOf(TimelineEventType.Inspection, TimelineEventType.Repair)),
    Other(setOf(TimelineEventType.Note)),
    Mileage(setOf(TimelineEventType.Mileage)),
}

/** [totalWon]은 불러온 기록이 아니라 조건에 맞는 그 달 전체 기록의 비용 합계다. */
data class TimelineSection(val month: YearMonth?, val items: List<TimelineItem>, val totalWon: Long = 0)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val filter: HistoryFilter = HistoryFilter.All,
    val advanced: AdvancedFilter = AdvancedFilter(),
    val keyword: String = "",
    val sections: List<TimelineSection> = emptyList(),
    val selected: RecordRef? = null,
    val detail: RecordDetail? = null,
    val attachments: List<Attachment> = emptyList(),
    val attachFailedCount: Int = 0,
    /** 아직 불러오지 않은 기록이 더 있다. 목록 끝 근처까지 내리면 다음 페이지를 읽는다. */
    val hasMore: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && sections.isEmpty()
    val isFiltered: Boolean
        get() = filter != HistoryFilter.All || keyword.isNotBlank() || advanced.activeCount > 0
}
