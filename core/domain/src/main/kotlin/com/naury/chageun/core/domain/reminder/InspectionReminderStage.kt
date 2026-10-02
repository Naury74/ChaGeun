package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.domain.vehicle.InspectionEvaluator
import com.naury.chageun.core.model.InspectionStatus

private const val WEEK_BEFORE_DAYS = 7L
private const val DAY_BEFORE_DAYS = 1L
private const val OVERDUE_DAYS = -1L

/** 긴급도 순으로 정렬한다. 각 단계는 만료일마다 최대 한 번만 알린다. */
enum class InspectionReminderStage(val maxDaysLeft: Long) {
    Days30(InspectionEvaluator.DUE_SOON_DAYS),
    Days7(WEEK_BEFORE_DAYS),
    Days1(DAY_BEFORE_DAYS),
    Overdue(OVERDUE_DAYS),
    ;

    companion object {
        /** 지금 알릴 단계. [alreadyNotified] 이후 새로 도달한 단계가 없으면 null. */
        fun next(status: InspectionStatus, alreadyNotified: InspectionReminderStage?): InspectionReminderStage? {
            val daysLeft = status.daysLeft ?: return null
            val reached = entries.lastOrNull { daysLeft <= it.maxDaysLeft } ?: return null
            return reached.takeIf { alreadyNotified == null || it.ordinal > alreadyNotified.ordinal }
        }
    }
}
