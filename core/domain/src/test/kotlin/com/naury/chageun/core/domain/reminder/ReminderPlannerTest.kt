package com.naury.chageun.core.domain.reminder

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.reminder.MaintenanceReminderStage.Due
import com.naury.chageun.core.domain.reminder.MaintenanceReminderStage.Early
import com.naury.chageun.core.domain.reminder.MaintenanceReminderStage.Near
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.RuleSource
import org.junit.Test

class ReminderPlannerTest {

    private val oil = MaintenanceItem.EngineOil
    private val rules = mapOf(
        oil to MaintenanceRule(oil, intervalKm = 10_000, intervalMonths = 12),
        MaintenanceItem.Wiper to MaintenanceRule(MaintenanceItem.Wiper, intervalKm = null, intervalMonths = 12),
    )

    private fun status(
        item: MaintenanceItem = oil,
        km: Long? = null,
        days: Long? = null,
        state: MaintenanceState = MaintenanceState.Good,
    ) = MaintenanceStatus(
        item = item,
        state = state,
        remainingKm = km,
        remainingDays = days,
        distanceDue = null,
        dateDue = null,
        estimatedDue = null,
        missingInputs = emptySet(),
        ruleSource = RuleSource.Generic,
    )

    private fun plan(
        vararg statuses: MaintenanceStatus,
        notified: Map<MaintenanceItem, MaintenanceReminderStage> = emptyMap(),
    ) = ReminderPlanner.plan(statuses.toList(), rules, notified)

    @Test
    fun distanceStages_followSpec_2000_500_due() {
        assertThat(MaintenanceReminderStage.reached(status(km = 2_001), rules[oil])).isNull()
        assertThat(MaintenanceReminderStage.reached(status(km = 2_000), rules[oil])).isEqualTo(Early)
        assertThat(MaintenanceReminderStage.reached(status(km = 500), rules[oil])).isEqualTo(Near)
        assertThat(MaintenanceReminderStage.reached(status(km = 0), rules[oil])).isEqualTo(Due)
        assertThat(MaintenanceReminderStage.reached(status(km = -300), rules[oil])).isEqualTo(Due)
    }

    @Test
    fun timeStages_followSpec_d60_d14_due() {
        val wiper = MaintenanceItem.Wiper
        assertThat(MaintenanceReminderStage.reached(status(wiper, days = 61), rules[wiper])).isNull()
        assertThat(MaintenanceReminderStage.reached(status(wiper, days = 60), rules[wiper])).isEqualTo(Early)
        assertThat(MaintenanceReminderStage.reached(status(wiper, days = 14), rules[wiper])).isEqualTo(Near)
        assertThat(MaintenanceReminderStage.reached(status(wiper, days = 0), rules[wiper])).isEqualTo(Due)
    }

    @Test
    fun usesWhicheverOfDistanceAndTimeComesFirst() {
        assertThat(MaintenanceReminderStage.reached(status(km = 5_000, days = 10), rules[oil])).isEqualTo(Near)
    }

    @Test
    fun skipsStagesLongerThanTheInterval() {
        val shortRule = MaintenanceRule(oil, intervalKm = 1_500, intervalMonths = null)

        // 1,500km마다 교체하면 2,000km 단계는 교체 직후부터 해당되므로 건너뛴다.
        assertThat(MaintenanceReminderStage.reached(status(km = 1_400), shortRule)).isNull()
        assertThat(MaintenanceReminderStage.reached(status(km = 400), shortRule)).isEqualTo(Near)
    }

    @Test
    fun notifiesEachStageOnce_inOrder() {
        val early = plan(status(km = 1_800))
        val same = plan(status(km = 1_200), notified = early.notifiedStages)
        val near = plan(status(km = 450), notified = same.notifiedStages)

        assertThat(early.toNotify).hasSize(1)
        assertThat(same.toNotify).isEmpty()
        assertThat(near.toNotify).hasSize(1)
        assertThat(near.notifiedStages).containsExactly(oil, Near)
    }

    @Test
    fun jumpingStraightToDue_notifiesOnce() {
        val result = plan(status(km = -100, state = MaintenanceState.Overdue))

        assertThat(result.toNotify).hasSize(1)
        assertThat(result.notifiedStages).containsExactly(oil, Due)
    }

    @Test
    fun replacement_resetsSoTheNextCycleNotifiesAgain() {
        val reset = plan(status(km = 9_800), notified = mapOf(oil to Due))
        val nextCycle = plan(status(km = 1_900), notified = reset.notifiedStages)

        assertThat(reset.notifiedStages).isEmpty()
        assertThat(nextCycle.toNotify).hasSize(1)
    }

    @Test
    fun unknownItems_areNeitherNotifiedNorReset() {
        val result = plan(status(state = MaintenanceState.Unknown), notified = mapOf(oil to Near))

        assertThat(result.toNotify).isEmpty()
        assertThat(result.notifiedStages).containsExactly(oil, Near)
    }
}
