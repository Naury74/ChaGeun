package com.naury.chageun.feature.history

import com.naury.chageun.core.domain.analytics.CalendarItemKind
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.UpcomingEvent
import java.time.LocalDate

/** 달력에서 다른 탭·화면으로 가는 길. feature 모듈끼리 의존하지 않도록 앱 셸이 채운다. */
data class HistoryCalendarLinks(
    val onOpenManageItem: (MaintenanceItem) -> Unit = {},
    val onOpenInspection: () -> Unit = {},
    val onUpdateMileage: () -> Unit = {},
    /** 앨범을 그 사진에서 연다. */
    val onOpenPhoto: (String) -> Unit = {},
    /** 소모품 교체 입력 창을 그 날짜로 연다. null이면 날짜 없이 [HistoryRoute]의 onRecordService를 쓴다. */
    val onRecordServiceOn: ((MaintenanceItem, LocalDate) -> Unit)? = null,
    /** 홈·앨범에서 고른 날짜. 기록 탭이 달력을 그날로 열고 나면 소비된다. */
    val pendingDate: LocalDate? = null,
    val onPendingDateHandled: () -> Unit = {},
) {
    fun open(event: UpcomingEvent) = when (event) {
        is UpcomingEvent.MaintenanceDue -> onOpenManageItem(event.item)
        is UpcomingEvent.InspectionDue -> onOpenInspection()
        is UpcomingEvent.MileageUpdate -> onUpdateMileage()
    }
}

internal val UpcomingEvent.itemKind: CalendarItemKind
    get() = when (this) {
        is UpcomingEvent.MaintenanceDue -> CalendarItemKind.MaintenanceDue
        is UpcomingEvent.InspectionDue -> CalendarItemKind.InspectionDue
        is UpcomingEvent.MileageUpdate -> CalendarItemKind.MileageUpdate
    }
