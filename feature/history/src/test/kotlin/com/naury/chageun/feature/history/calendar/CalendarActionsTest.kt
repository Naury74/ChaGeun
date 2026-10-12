package com.naury.chageun.feature.history.calendar

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Test

class CalendarActionsTest {
    private val calls = mutableListOf<String>()
    private val actions = CalendarActions(
        onSelectDate = { calls += "select $it" },
        onShowMonth = { calls += "month $it" },
        onToday = { calls += "today" },
        onMoveWeek = { calls += "week $it" },
        onPreviousResult = { calls += "previous" },
        onNextResult = { calls += "next" },
        onOpenResult = { calls += "result ${it.ref.id}" },
        onOpenRecord = { calls += "record ${it.id}" },
        onSetMode = { calls += "mode $it" },
    ).closingDetailOnMove { calls += "close" }

    @Test
    fun movingToAnotherDay_closesDetailFirst() {
        val date = LocalDate.of(2026, 10, 8)
        actions.onSelectDate(date)
        actions.onShowMonth(YearMonth.of(2026, 9))
        actions.onToday()
        actions.onMoveWeek(-1)
        actions.onPreviousResult()
        actions.onNextResult()
        actions.onOpenResult(CalendarFixtures.record("r1", TimelineEventType.Fuel, date))

        assertThat(calls).containsExactly(
            "close", "select $date",
            "close", "month 2026-09",
            "close", "today",
            "close", "week -1",
            "close", "previous",
            "close", "next",
            "close", "result r1",
        ).inOrder()
    }

    @Test
    fun openingRecordOrChangingMode_keepsDetail() {
        actions.onOpenRecord(RecordRef(TimelineEventType.Fuel, "r1"))
        actions.onSetMode(CalendarMode.Week)

        assertThat(calls).containsExactly("record r1", "mode Week").inOrder()
    }
}
