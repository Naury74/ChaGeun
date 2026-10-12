package com.naury.chageun.core.domain.calendar

import com.naury.chageun.core.domain.maintenance.MILEAGE_PROMPT_AFTER_DAYS
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.MaintenanceOverview
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.UpcomingEvent
import java.time.LocalDate

/**
 * 정비 개요에서 달력에 놓을 예정 일정을 만든다.
 *
 * - 정비 항목은 항목마다 하나만 둔다. 예상 교체일(estimatedDue)이 교체 기준일(dateDue)과 같으면 확정으로,
 *   주행 속도로 더 이르게 내다봤거나 기준일이 없으면 예상으로 놓는다.
 * - 날짜가 오늘보다 앞서거나 상태가 지남이면 오늘로 모은다.
 * - 주행거리 갱신 권장일은 마지막 기록 뒤 [MILEAGE_PROMPT_AFTER_DAYS]일이다. 기록이 없으면 오늘이다.
 */
object UpcomingEventPlanner {

    fun plan(overview: MaintenanceOverview, today: LocalDate): List<UpcomingEvent> =
        plan(overview.statuses, overview.inspection.schedule, overview.currentMileage, today)

    fun plan(
        statuses: List<MaintenanceStatus>,
        inspection: InspectionSchedule?,
        lastMileage: MileageReading?,
        today: LocalDate,
    ): List<UpcomingEvent> {
        val maintenance = statuses.mapNotNull { it.toEvent(today) }
        val inspectionEvent = inspection?.let {
            UpcomingEvent.InspectionDue(maxOf(it.nextDueDate, today), isOverdue = it.nextDueDate < today)
        }
        val mileageDue = lastMileage?.date?.plusDays(MILEAGE_PROMPT_AFTER_DAYS) ?: today
        val mileageEvent = UpcomingEvent.MileageUpdate(maxOf(mileageDue, today), isOverdue = mileageDue < today)
        return (maintenance + listOfNotNull(inspectionEvent, mileageEvent)).sortedWith(ORDER)
    }

    private fun MaintenanceStatus.toEvent(today: LocalDate): UpcomingEvent.MaintenanceDue? {
        val estimate = estimatedDue
        val isEstimated = estimate != null && estimate.date != dateDue
        val date = (if (isEstimated) estimate?.date else dateDue) ?: return null
        val isOverdue = state == MaintenanceState.Overdue || date < today
        return UpcomingEvent.MaintenanceDue(
            item = item,
            date = if (isOverdue) today else date,
            isEstimated = isEstimated,
            confidence = estimate?.confidence?.takeIf { isEstimated },
            isOverdue = isOverdue,
        )
    }

    // 같은 날에는 지난 일, 정비, 검사, 주행거리 순으로 둔다.
    private val ORDER: Comparator<UpcomingEvent> = compareBy<UpcomingEvent> { it.date }
        .thenByDescending { it.isOverdue }
        .thenBy { it.kindOrder }

    private val UpcomingEvent.kindOrder: Int
        get() = when (this) {
            is UpcomingEvent.MaintenanceDue -> 0
            is UpcomingEvent.InspectionDue -> 1
            is UpcomingEvent.MileageUpdate -> 2
        }
}

/** 양 끝 날짜를 포함한다. */
fun List<UpcomingEvent>.within(from: LocalDate, to: LocalDate): List<UpcomingEvent> =
    filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
