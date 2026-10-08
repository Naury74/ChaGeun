package com.naury.chageun.core.model

import java.time.LocalDate

enum class InspectionState { Ok, DueSoon, Overdue, Unknown }

enum class InspectionSource { User, Official }

data class InspectionSchedule(val nextDueDate: LocalDate, val source: InspectionSource)

/** [daysLeft]는 기한이 지나면 음수가 되고, 일정을 모르면 null이다. */
data class InspectionStatus(val schedule: InspectionSchedule?, val daysLeft: Long?, val state: InspectionState) {
    companion object {
        val Unknown = InspectionStatus(schedule = null, daysLeft = null, state = InspectionState.Unknown)
    }
}

data class InspectionRecord(val date: LocalDate, val mileage: Kilometers?, val result: PeriodicInspectionResult)
