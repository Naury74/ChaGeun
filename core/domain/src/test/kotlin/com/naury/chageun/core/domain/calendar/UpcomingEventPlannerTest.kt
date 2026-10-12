package com.naury.chageun.core.domain.calendar

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.Confidence
import com.naury.chageun.core.model.DueEstimate
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.UpcomingEvent
import java.time.LocalDate
import org.junit.Test

class UpcomingEventPlannerTest {

    private val today = LocalDate.of(2026, 10, 12)
    private val recentMileage = MileageReading(today, Kilometers(40_000))

    private fun status(
        item: MaintenanceItem,
        dateDue: LocalDate?,
        estimatedDue: DueEstimate?,
        state: MaintenanceState = MaintenanceState.Upcoming,
    ) = MaintenanceStatus(item, state, null, null, null, dateDue, estimatedDue, emptySet(), RuleSource.Generic)

    private fun maintenance(vararg statuses: MaintenanceStatus) =
        UpcomingEventPlanner.plan(statuses.toList(), null, recentMileage, today)
            .filterIsInstance<UpcomingEvent.MaintenanceDue>()

    @Test
    fun estimateOnTheDueDate_isFixed() {
        val due = today.plusDays(20)

        val events = maintenance(status(MaintenanceItem.EngineOil, due, DueEstimate(due, Confidence.High)))

        assertThat(events).containsExactly(
            UpcomingEvent.MaintenanceDue(MaintenanceItem.EngineOil, due, isEstimated = false, confidence = null),
        )
    }

    @Test
    fun earlierEstimateFromDriving_isEstimatedWithItsConfidence() {
        val estimated = today.plusDays(10)

        val events = maintenance(
            status(MaintenanceItem.EngineOil, today.plusDays(40), DueEstimate(estimated, Confidence.Low)),
            status(MaintenanceItem.Tire, null, DueEstimate(today.plusDays(90), Confidence.Medium)),
        )

        assertThat(events).containsExactly(
            UpcomingEvent.MaintenanceDue(MaintenanceItem.EngineOil, estimated, true, Confidence.Low),
            UpcomingEvent.MaintenanceDue(MaintenanceItem.Tire, today.plusDays(90), true, Confidence.Medium),
        ).inOrder()
    }

    @Test
    fun overdueItems_areGatheredOnToday() {
        val events = maintenance(
            status(
                MaintenanceItem.BrakeFluid,
                today.minusDays(30),
                DueEstimate(today.minusDays(30), Confidence.High),
                MaintenanceState.Overdue,
            ),
            // 상태는 아직 지남이 아니어도 예상일이 지난 날짜면 오늘로 모은다.
            status(MaintenanceItem.Wiper, null, DueEstimate(today.minusDays(2), Confidence.Medium)),
        )

        assertThat(events.map { it.date }).containsExactly(today, today)
        assertThat(events.all { it.isOverdue }).isTrue()
    }

    @Test
    fun itemsWithoutAnyDate_areLeftOut() {
        assertThat(maintenance(status(MaintenanceItem.Battery, null, null, MaintenanceState.Unknown))).isEmpty()
    }

    @Test
    fun inspection_isPlacedOnItsDueDate_orTodayWhenPast() {
        val upcoming = UpcomingEventPlanner.plan(
            emptyList(),
            InspectionSchedule(today.plusDays(45), InspectionSource.User),
            recentMileage,
            today,
        )
        val overdue = UpcomingEventPlanner.plan(
            emptyList(),
            InspectionSchedule(today.minusDays(3), InspectionSource.Official),
            recentMileage,
            today,
        )

        assertThat(upcoming).contains(UpcomingEvent.InspectionDue(today.plusDays(45)))
        assertThat(overdue).contains(UpcomingEvent.InspectionDue(today, isOverdue = true))
    }

    @Test
    fun mileageUpdate_isSuggestedThirtyDaysAfterTheLastReading() {
        fun mileageEvent(last: MileageReading?) = UpcomingEventPlanner.plan(emptyList(), null, last, today)
            .filterIsInstance<UpcomingEvent.MileageUpdate>().single()

        assertThat(mileageEvent(MileageReading(today.minusDays(10), Kilometers(1))))
            .isEqualTo(UpcomingEvent.MileageUpdate(today.plusDays(20)))
        assertThat(mileageEvent(MileageReading(today.minusDays(45), Kilometers(1))))
            .isEqualTo(UpcomingEvent.MileageUpdate(today, isOverdue = true))
        assertThat(mileageEvent(null)).isEqualTo(UpcomingEvent.MileageUpdate(today))
    }

    @Test
    fun sortsByDate_overdueFirstOnTheSameDay() {
        val events = UpcomingEventPlanner.plan(
            listOf(
                status(MaintenanceItem.AirFilter, today, DueEstimate(today, Confidence.High)),
                status(MaintenanceItem.EngineOil, today.minusDays(1), null, MaintenanceState.Overdue),
            ),
            InspectionSchedule(today.plusDays(1), InspectionSource.User),
            MileageReading(today.minusDays(MILEAGE_DAYS), Kilometers(1)),
            today,
        )

        assertThat(events.map { it::class.simpleName to it.date }).containsExactly(
            "MaintenanceDue" to today,
            "MaintenanceDue" to today,
            "MileageUpdate" to today,
            "InspectionDue" to today.plusDays(1),
        ).inOrder()
        assertThat((events.first() as UpcomingEvent.MaintenanceDue).item).isEqualTo(MaintenanceItem.EngineOil)
    }

    @Test
    fun within_includesBothEnds() {
        val events = listOf(
            UpcomingEvent.MileageUpdate(today),
            UpcomingEvent.InspectionDue(today.plusDays(14)),
            UpcomingEvent.InspectionDue(today.plusDays(15)),
        )

        assertThat(events.within(today, today.plusDays(14))).hasSize(2)
    }

    private companion object {
        const val MILEAGE_DAYS = 30L
    }
}
