package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionState
import com.naury.chageun.core.model.InspectionStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object InspectionEvaluator {
    /** Periodic inspections open 31 days before the due date, so the last month counts as due soon. */
    const val DUE_SOON_DAYS = 30L

    fun evaluate(schedule: InspectionSchedule?, today: LocalDate): InspectionStatus {
        if (schedule == null) return InspectionStatus.Unknown
        val daysLeft = ChronoUnit.DAYS.between(today, schedule.nextDueDate)
        val state = when {
            daysLeft < 0 -> InspectionState.Overdue
            daysLeft <= DUE_SOON_DAYS -> InspectionState.DueSoon
            else -> InspectionState.Ok
        }
        return InspectionStatus(schedule, daysLeft, state)
    }
}
