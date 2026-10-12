package com.naury.chageun.feature.history.calendar

import com.naury.chageun.core.model.CalendarDay
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import java.time.LocalDate

/** 달력을 거르는 종류. 점검·수리 칩은 메모 기록도 함께 고른다. */
enum class CalendarKind { Maintenance, Fuel, CheckRepair, Mileage, Photos, Upcoming }

/** 달력을 한 달 전체로 볼지, 고른 날이 있는 한 주만 볼지. */
enum class CalendarMode { Month, Week }

internal val TimelineEventType.calendarKind: CalendarKind
    get() = when (this) {
        TimelineEventType.Maintenance -> CalendarKind.Maintenance
        TimelineEventType.Fuel -> CalendarKind.Fuel
        TimelineEventType.Inspection, TimelineEventType.Repair, TimelineEventType.Note -> CalendarKind.CheckRepair
        TimelineEventType.Mileage -> CalendarKind.Mileage
    }

/**
 * 달력 필터. 아무것도 고르지 않으면 모두 보이고, 종류 칩을 고르면 그 종류만 보인다.
 * 정비 항목([items])을 고르면 정비 기록과 정비 예정은 그 항목만 남는다. 종류를 따로 고르지 않았다면
 * 항목을 고른 것만으로 정비 기록과 그 항목의 예정만 보인다.
 */
data class CalendarFilter(val kinds: Set<CalendarKind> = emptySet(), val items: Set<MaintenanceItem> = emptySet()) {
    /** 머리글 요약의 "필터 n개". 항목 선택은 몇 개를 골랐든 하나로 센다. */
    val activeCount: Int get() = kinds.size + if (items.isEmpty()) 0 else 1

    val isActive: Boolean get() = activeCount > 0

    private val shownKinds: Set<CalendarKind>
        get() = when {
            kinds.isNotEmpty() && items.isNotEmpty() -> kinds + CalendarKind.Maintenance
            kinds.isNotEmpty() -> kinds
            items.isNotEmpty() -> setOf(CalendarKind.Maintenance, CalendarKind.Upcoming)
            else -> CalendarKind.entries.toSet()
        }

    val showsPhotos: Boolean get() = CalendarKind.Photos in shownKinds

    fun showsRecord(record: TimelineItem): Boolean {
        val kind = record.ref.type.calendarKind
        if (kind !in shownKinds) return false
        return items.isEmpty() || kind != CalendarKind.Maintenance || record.maintenanceItem in items
    }

    fun showsUpcoming(event: UpcomingEvent): Boolean {
        if (CalendarKind.Upcoming !in shownKinds) return false
        return items.isEmpty() || (event as? UpcomingEvent.MaintenanceDue)?.item in items
    }

    fun apply(day: CalendarDay): CalendarDay = if (!isActive) {
        day
    } else {
        day.copy(
            records = day.records.filter(::showsRecord),
            photos = day.photos.takeIf { showsPhotos }.orEmpty(),
            upcoming = day.upcoming.filter(::showsUpcoming),
            recordPhotos = day.recordPhotos.takeIf { showsPhotos }.orEmpty(),
        )
    }
}

/**
 * 달력 검색. [results]는 달과 상관없이 찾은 기록이며 최신순이다. 검색어가 비면 검색하지 않은 것으로 본다.
 */
data class CalendarSearch(
    val isOpen: Boolean = false,
    val keyword: String = "",
    val results: List<TimelineItem> = emptyList(),
) {
    val isActive: Boolean get() = keyword.isNotBlank()
}

/** 결과가 있는 날짜(오래된 순)에서 [from]의 앞뒤 날짜. [from]이 결과 날짜여도 그날은 건너뛴다. */
internal fun List<LocalDate>.previousBefore(from: LocalDate): LocalDate? = lastOrNull { it.isBefore(from) }

internal fun List<LocalDate>.nextAfter(from: LocalDate): LocalDate? = firstOrNull { it.isAfter(from) }

/** 필터를 반영한 이번 달 요약. */
data class MonthSummaryValues(val totalWon: Long, val recordCount: Int, val photoCount: Int)
