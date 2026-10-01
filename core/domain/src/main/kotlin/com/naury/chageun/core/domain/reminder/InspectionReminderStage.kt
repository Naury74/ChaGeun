package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.domain.vehicle.InspectionEvaluator
import com.naury.chageun.core.model.InspectionStatus

private const val WEEK_BEFORE_DAYS = 7L
private const val DAY_BEFORE_DAYS = 1L
private const val OVERDUE_DAYS = -1L

/** Ordered by urgency; each stage is notified at most once per due date. */
enum class InspectionReminderStage(val maxDaysLeft: Long) {
    Days30(InspectionEvaluator.DUE_SOON_DAYS),
    Days7(WEEK_BEFORE_DAYS),
    Days1(DAY_BEFORE_DAYS),
    Overdue(OVERDUE_DAYS),
    ;

    companion object {
        /** The stage to notify now, or null when nothing new has been reached since [alreadyNotified]. */
        fun next(status: InspectionStatus, alreadyNotified: InspectionReminderStage?): InspectionReminderStage? {
            val daysLeft = status.daysLeft ?: return null
            val reached = entries.lastOrNull { daysLeft <= it.maxDaysLeft } ?: return null
            return reached.takeIf { alreadyNotified == null || it.ordinal > alreadyNotified.ordinal }
        }
    }
}
