package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus

data class ReminderPlan(
    val toNotify: List<MaintenanceStatus>,
    val notifiedStages: Map<MaintenanceItem, MaintenanceReminderStage>,
)

/**
 * 항목이 [MaintenanceReminderStage]의 다음 단계에 도달할 때 한 번씩 알린다.
 *
 * 교체 기록 등으로 어느 단계에도 해당하지 않게 되면 항목 상태를 지워 다음 주기에 다시 알리게 한다.
 * Unknown 항목은 알리지도 지우지도 않는다. 데이터가 없으면 믿을 만한 안내를 할 수 없기 때문이다.
 */
object ReminderPlanner {

    fun plan(
        statuses: List<MaintenanceStatus>,
        rules: Map<MaintenanceItem, MaintenanceRule>,
        alreadyNotified: Map<MaintenanceItem, MaintenanceReminderStage>,
    ): ReminderPlan {
        val toNotify = mutableListOf<MaintenanceStatus>()
        val notified = alreadyNotified.toMutableMap()
        statuses.forEach { status ->
            if (status.state == MaintenanceState.Unknown) return@forEach
            val stage = MaintenanceReminderStage.reached(status, rules[status.item])
            val previous = alreadyNotified[status.item]
            when {
                stage == null -> notified.remove(status.item)
                previous == null || stage.ordinal > previous.ordinal -> {
                    toNotify += status
                    notified[status.item] = stage
                }
            }
        }
        return ReminderPlan(toNotify, notified)
    }
}
