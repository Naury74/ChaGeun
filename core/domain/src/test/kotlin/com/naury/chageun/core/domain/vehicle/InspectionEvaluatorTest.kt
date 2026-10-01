package com.naury.chageun.core.domain.vehicle

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.reminder.InspectionReminderStage
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.InspectionState
import com.naury.chageun.core.model.InspectionStatus
import java.time.LocalDate
import org.junit.Test

class InspectionEvaluatorTest {

    private val today = LocalDate.of(2026, 10, 1)

    private fun statusIn(days: Long) =
        InspectionEvaluator.evaluate(InspectionSchedule(today.plusDays(days), InspectionSource.User), today)

    @Test
    fun noSchedule_isUnknown() {
        assertThat(InspectionEvaluator.evaluate(null, today)).isEqualTo(InspectionStatus.Unknown)
    }

    @Test
    fun state_followsDaysLeft() {
        assertThat(statusIn(31).state).isEqualTo(InspectionState.Ok)
        assertThat(statusIn(30).state).isEqualTo(InspectionState.DueSoon)
        assertThat(statusIn(0).state).isEqualTo(InspectionState.DueSoon)
        assertThat(statusIn(-1).state).isEqualTo(InspectionState.Overdue)
        assertThat(statusIn(-1).daysLeft).isEqualTo(-1)
    }

    @Test
    fun reminderStage_reachesD30_D7_D1_thenOverdueOnce() {
        assertThat(InspectionReminderStage.next(statusIn(31), null)).isNull()
        assertThat(InspectionReminderStage.next(statusIn(30), null)).isEqualTo(InspectionReminderStage.Days30)
        assertThat(InspectionReminderStage.next(statusIn(8), InspectionReminderStage.Days30)).isNull()
        assertThat(InspectionReminderStage.next(statusIn(7), InspectionReminderStage.Days30))
            .isEqualTo(InspectionReminderStage.Days7)
        assertThat(InspectionReminderStage.next(statusIn(0), InspectionReminderStage.Days7))
            .isEqualTo(InspectionReminderStage.Days1)
        assertThat(InspectionReminderStage.next(statusIn(-3), InspectionReminderStage.Days1))
            .isEqualTo(InspectionReminderStage.Overdue)
        assertThat(InspectionReminderStage.next(statusIn(-40), InspectionReminderStage.Overdue)).isNull()
    }

    @Test
    fun reminderStage_skipsMissedStages_withOneNotification() {
        assertThat(InspectionReminderStage.next(statusIn(3), null)).isEqualTo(InspectionReminderStage.Days7)
    }

    @Test
    fun reminderStage_unknownSchedule_neverNotifies() {
        assertThat(InspectionReminderStage.next(InspectionStatus.Unknown, null)).isNull()
    }
}
