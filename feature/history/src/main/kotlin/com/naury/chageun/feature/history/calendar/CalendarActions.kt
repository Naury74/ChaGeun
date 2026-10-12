package com.naury.chageun.feature.history.calendar

import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import java.time.LocalDate
import java.time.YearMonth

/** 달력에서 하는 일. 이동할 곳은 앱 셸이 정한다. */
data class CalendarActions(
    val onSelectDate: (LocalDate) -> Unit = {},
    val onShowMonth: (YearMonth) -> Unit = {},
    val onToday: () -> Unit = {},
    /** 주 보기에서 앞뒤 주로 간다. 음수면 이전 주다. */
    val onMoveWeek: (Long) -> Unit = {},
    val onSetMode: (CalendarMode) -> Unit = {},
    val onToggleKind: (CalendarKind) -> Unit = {},
    val onSetItems: (Set<MaintenanceItem>) -> Unit = {},
    val onOpenSearch: () -> Unit = {},
    val onCloseSearch: () -> Unit = {},
    val onSearch: (String) -> Unit = {},
    val onPreviousResult: () -> Unit = {},
    val onNextResult: () -> Unit = {},
    val onOpenResult: (TimelineItem) -> Unit = {},
    /** 필터와 검색을 모두 지운다. */
    val onClearAll: () -> Unit = {},
    val onOpenUpcoming: (UpcomingEvent) -> Unit = {},
    /** 기록 행과 기록 첨부 사진이 함께 쓴다. 첨부 사진을 누르면 그 기록 상세가 열린다. */
    val onOpenRecord: (RecordRef) -> Unit = {},
    val onOpenPhoto: (AlbumPhoto) -> Unit = {},
    /** 지난 날과 오늘에만 쓴다. 앞으로의 날에는 기록을 남길 수 없다. */
    val onAddOnDate: (LocalDate) -> Unit = {},
)
