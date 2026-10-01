package com.naury.chageun.core.domain.reminder

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.RuleSource
import org.junit.Test

class ReminderPlannerTest {

    private fun status(item: MaintenanceItem, state: MaintenanceState) = MaintenanceStatus(
        item = item,
        state = state,
        remainingKm = null,
        remainingDays = null,
        distanceDue = null,
        dateDue = null,
        estimatedDue = null,
        missingInputs = emptySet(),
        ruleSource = RuleSource.Generic,
    )

    @Test
    fun notifiesFirstTimeAnItemNeedsAttention() {
        val plan = ReminderPlanner.plan(
            listOf(status(MaintenanceItem.EngineOil, MaintenanceState.Upcoming)),
            emptyMap(),
        )

        assertThat(plan.toNotify.map { it.item }).containsExactly(MaintenanceItem.EngineOil)
        assertThat(plan.notifiedStates).containsExactly(MaintenanceItem.EngineOil, MaintenanceState.Upcoming)
    }

    @Test
    fun doesNotRepeatTheSameStage() {
        val plan = ReminderPlanner.plan(
            listOf(status(MaintenanceItem.EngineOil, MaintenanceState.Due)),
            mapOf(MaintenanceItem.EngineOil to MaintenanceState.Due),
        )

        assertThat(plan.toNotify).isEmpty()
    }

    @Test
    fun notifiesAgain_whenMovingToAMoreUrgentStage() {
        val plan = ReminderPlanner.plan(
            listOf(status(MaintenanceItem.EngineOil, MaintenanceState.Overdue)),
            mapOf(MaintenanceItem.EngineOil to MaintenanceState.Due),
        )

        assertThat(plan.toNotify).hasSize(1)
        assertThat(plan.notifiedStates[MaintenanceItem.EngineOil]).isEqualTo(MaintenanceState.Overdue)
    }

    @Test
    fun resetsAfterReplacement_soTheNextCycleNotifies() {
        val reset = ReminderPlanner.plan(
            listOf(status(MaintenanceItem.EngineOil, MaintenanceState.Good)),
            mapOf(MaintenanceItem.EngineOil to MaintenanceState.Overdue),
        )
        val nextCycle = ReminderPlanner.plan(
            listOf(status(MaintenanceItem.EngineOil, MaintenanceState.Upcoming)),
            reset.notifiedStates,
        )

        assertThat(reset.notifiedStates).isEmpty()
        assertThat(nextCycle.toNotify).hasSize(1)
    }

    @Test
    fun neverNotifiesUnknownItems_andKeepsTheirHistory() {
        val plan = ReminderPlanner.plan(
            listOf(status(MaintenanceItem.Tire, MaintenanceState.Unknown)),
            mapOf(MaintenanceItem.Tire to MaintenanceState.Due),
        )

        assertThat(plan.toNotify).isEmpty()
        assertThat(plan.notifiedStates).containsExactly(MaintenanceItem.Tire, MaintenanceState.Due)
    }
}
