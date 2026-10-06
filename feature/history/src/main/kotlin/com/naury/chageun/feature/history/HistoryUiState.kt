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
}

data class TimelineSection(val month: YearMonth?, val items: List<TimelineItem>)

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
) {
    val isEmpty: Boolean get() = !isLoading && sections.isEmpty()
    val isFiltered: Boolean
        get() = filter != HistoryFilter.All || keyword.isNotBlank() || advanced.activeCount > 0
}
