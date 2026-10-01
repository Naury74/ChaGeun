package com.naury.chageun.core.model

import java.time.LocalDate

enum class InspectionState { Ok, DueSoon, Overdue, Unknown }

enum class InspectionSource { User, Official }

data class InspectionSchedule(val nextDueDate: LocalDate, val source: InspectionSource)

/** [daysLeft] is negative once the due date has passed; null when no schedule is known. */
data class InspectionStatus(val schedule: InspectionSchedule?, val daysLeft: Long?, val state: InspectionState) {
    companion object {
        val Unknown = InspectionStatus(schedule = null, daysLeft = null, state = InspectionState.Unknown)
    }
}
