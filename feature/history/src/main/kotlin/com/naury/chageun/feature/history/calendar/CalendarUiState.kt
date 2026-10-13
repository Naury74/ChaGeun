package com.naury.chageun.feature.history.calendar

import com.naury.chageun.core.model.CalendarDay
import com.naury.chageun.core.model.CalendarMonth
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class CalendarUiState(
    val isLoading: Boolean = true,
    val calendar: CalendarMonth? = null,
    val selectedDate: LocalDate? = null,
    /** 사용자가 고른 보기. null이면 화면 배치에 맞는 기본값을 쓴다(탁자 자세는 주, 나머지는 월). */
    val mode: CalendarMode? = null,
    val filter: CalendarFilter = CalendarFilter(),
    val search: CalendarSearch = CalendarSearch(),
    /** 오늘을 골랐을 때 이어서 보여 주는 내일부터 30일 동안의 예정. */
    val upcomingSoon: List<UpcomingEvent> = emptyList(),
) {
    /** 필터로 거른 하루. 달이 넘어가는 동안에는 [source]로 앞 달 데이터를 그린다. */
    fun visibleDay(date: LocalDate, source: CalendarMonth? = calendar): CalendarDay =
        filter.apply(source?.day(date) ?: CalendarDay(date))

    val selectedDay: CalendarDay? get() = selectedDate?.let { visibleDay(it) }

    val isTodaySelected: Boolean get() = selectedDate != null && selectedDate == calendar?.today

    val visibleUpcomingSoon: List<UpcomingEvent>
        get() = upcomingSoon.filter(filter::showsUpcoming).takeIf { isTodaySelected }.orEmpty()

    /** 필터를 거친 검색 결과. 최신순이다. */
    val searchResults: List<TimelineItem>
        get() = search.results.filter(filter::showsRecord).takeIf { search.isActive }.orEmpty()

    /** 검색 결과가 있는 날짜. 오래된 순이다. */
    val resultDates: List<LocalDate> get() = searchResults.mapNotNull { it.date }.distinct().sorted()

    val previousResultDate: LocalDate? get() = selectedDate?.let { resultDates.previousBefore(it) }

    val nextResultDate: LocalDate? get() = selectedDate?.let { resultDates.nextAfter(it) }

    val isFilteredOrSearching: Boolean get() = filter.isActive || search.isActive

    /** 고른 날이 있는 한 주. 주 시작 요일은 달력과 같다. */
    fun selectedWeek(source: CalendarMonth): List<LocalDate> {
        val anchor = selectedDate ?: source.today
        val start = anchor.with(TemporalAdjusters.previousOrSame(source.firstDayOfWeek))
        return List(DAYS_PER_WEEK) { start.plusDays(it.toLong()) }
    }

    /** 이번 달 지출·기록 수·사진 수·동영상 수. 필터가 없으면 저장소가 센 값을, 있으면 보이는 것만 다시 센다. */
    fun monthSummary(source: CalendarMonth): MonthSummaryValues {
        val monthDays = source.days.values.filter { YearMonth.from(it.date) == source.month }
        if (!filter.isActive) {
            val videos = monthDays.sumOf { it.videoCount }
            return MonthSummaryValues(source.monthTotalWon, source.recordCount, source.photoCount - videos, videos)
        }
        val days = monthDays.map(filter::apply)
        val records = days.flatMap { it.records }
        return MonthSummaryValues(
            totalWon = records.sumOf { it.costWon ?: 0L },
            recordCount = records.size,
            photoCount = days.sumOf { it.photoCount - it.videoCount },
            videoCount = days.sumOf { it.videoCount },
        )
    }

    private companion object {
        const val DAYS_PER_WEEK = 7
    }
}

/** 목록 화면이 달력 보기로 그릴 때 넘겨받는 상태와 동작. */
data class HistoryCalendarSlot(val state: CalendarUiState, val actions: CalendarActions)
