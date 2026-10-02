package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus

data class ReminderPlan(
    val toNotify: List<MaintenanceStatus>,
    val notifiedStates: Map<MaintenanceItem, MaintenanceState>,
)

/**
 * 항목이 Upcoming → Due → Overdue로 넘어갈 때 단계마다 한 번씩 알린다.
 *
 * 교체 기록 등으로 Good으로 돌아오면 항목 상태를 지워 다음 주기에 다시 알리게 한다.
 * Unknown 항목은 알리지 않는다. 데이터가 없으면 믿을 만한 안내를 할 수 없기 때문이다.
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
