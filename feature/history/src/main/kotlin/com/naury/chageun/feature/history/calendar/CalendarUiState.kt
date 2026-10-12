package com.naury.chageun.feature.history.calendar

import com.naury.chageun.core.model.CalendarDay
import com.naury.chageun.core.model.CalendarMonth
import com.naury.chageun.core.model.UpcomingEvent
import java.time.LocalDate

/** 달력에 겹쳐 그리는 정보 묶음. 칩으로 하나씩 켜고 끈다. */
enum class CalendarLayer { Records, Photos, Upcoming }

data class CalendarUiState(
    val isLoading: Boolean = true,
    val calendar: CalendarMonth? = null,
    val selectedDate: LocalDate? = null,
    val layers: Set<CalendarLayer> = CalendarLayer.entries.toSet(),
    /** 오늘을 골랐을 때 이어서 보여 주는 내일부터 30일 동안의 예정. */
    val upcomingSoon: List<UpcomingEvent> = emptyList(),
) {
    /** 꺼 둔 레이어를 뺀 하루. 달이 넘어가는 동안에는 [source]로 앞 달 데이터를 그린다. */
    fun visibleDay(date: LocalDate, source: CalendarMonth? = calendar): CalendarDay {
        val day = source?.day(date) ?: CalendarDay(date)
        return CalendarDay(
            date = date,
            records = day.records.takeIf { CalendarLayer.Records in layers }.orEmpty(),
            photos = day.photos.takeIf { CalendarLayer.Photos in layers }.orEmpty(),
            upcoming = day.upcoming.takeIf { CalendarLayer.Upcoming in layers }.orEmpty(),
        )
    }

    val selectedDay: CalendarDay? get() = selectedDate?.let { visibleDay(it) }

    val isTodaySelected: Boolean get() = selectedDate != null && selectedDate == calendar?.today

    val visibleUpcomingSoon: List<UpcomingEvent>
        get() = upcomingSoon.takeIf { isTodaySelected && CalendarLayer.Upcoming in layers }.orEmpty()
}

/** 목록 화면이 달력 보기로 그릴 때 넘겨받는 상태와 동작. */
data class HistoryCalendarSlot(val state: CalendarUiState, val actions: CalendarActions)
