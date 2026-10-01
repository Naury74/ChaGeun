package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus

data class ReminderPlan(
    val toNotify: List<MaintenanceStatus>,
    val notifiedStates: Map<MaintenanceItem, MaintenanceState>,
)

/**
 * Notifies each item once per stage as it moves Upcoming → Due → Overdue.
 *
 * Returning to Good (e.g. after a replacement is recorded) clears the item so its next cycle notifies again.
 * Unknown items are never notified: without data there is nothing reliable to say.
 */
object ReminderPlanner {

    private val NOTIFIABLE = setOf(MaintenanceState.Upcoming, MaintenanceState.Due, MaintenanceState.Overdue)

    fun plan(statuses: List<MaintenanceStatus>, alreadyNotified: Map<MaintenanceItem, MaintenanceState>): ReminderPlan {
        val toNotify = mutableListOf<MaintenanceStatus>()
        val notified = alreadyNotified.toMutableMap()
        statuses.forEach { status ->
            val previous = alreadyNotified[status.item]
            when {
                status.state == MaintenanceState.Good -> notified.remove(status.item)
                status.state !in NOTIFIABLE -> Unit
                previous == null || status.state.ordinal < previous.ordinal -> {
                    toNotify += status
                    notified[status.item] = status.state
                }
            }
        }
        return ReminderPlan(toNotify, notified)
    }
}
