package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus

/**
 * 정비 항목 알림 단계. 긴급도 순이며 단계마다 한 번만 알린다(기획 §16.2).
 * 거리형은 2,000km·500km·도래, 기간형은 D-60·D-14·도래에 도달한다. 둘 다 있으면 먼저 닿는 쪽을 따른다.
 */
enum class MaintenanceReminderStage(val withinKm: Long, val withinDays: Long) {
    Early(withinKm = 2_000, withinDays = 60),
    Near(withinKm = 500, withinDays = 14),
    Due(withinKm = 0, withinDays = 0),
    ;

    companion object {
        /**
         * 지금 도달한 가장 긴급한 단계. 아직 어느 단계에도 닿지 않았거나 정보가 없으면 null이다.
         * 교체 주기가 단계 기준보다 짧으면(예: 1,500km마다 교체) 교체하자마자 알리게 되므로 그 단계는 건너뛴다.
         */
        fun reached(status: MaintenanceStatus, rule: MaintenanceRule?): MaintenanceReminderStage? {
            if (status.state == MaintenanceState.Unknown) return null
            return entries.lastOrNull { stage ->
                val byKm = status.remainingKm?.let { remaining ->
                    remaining <= stage.withinKm && rule?.intervalKm?.let { it > stage.withinKm } != false
                } ?: false
                val byDays = status.remainingDays?.let { remaining ->
                    remaining <= stage.withinDays &&
                        rule?.intervalMonths?.let { it * DAYS_PER_MONTH > stage.withinDays } != false
                } ?: false
                byKm || byDays
            }
        }

        private const val DAYS_PER_MONTH = 30
    }
}
