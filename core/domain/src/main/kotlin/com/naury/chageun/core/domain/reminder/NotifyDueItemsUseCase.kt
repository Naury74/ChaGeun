package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.domain.vehicle.InspectionRepository
import com.naury.chageun.core.model.MaintenanceOverview
import com.naury.chageun.core.model.VehicleId
import javax.inject.Inject

/** 정비 항목은 단계마다, 검사는 D-30·D-7·D-1·초과 단계마다 한 번씩 알린다. */
class NotifyDueItemsUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val inspectionRepository: InspectionRepository,
    private val notifier: ReminderNotifier,
) {
    /** @return 띄운 알림 수. */
    suspend operator fun invoke(vehicleId: VehicleId, overview: MaintenanceOverview): Int {
        val plan = ReminderPlanner.plan(overview.statuses, reminderRepository.notifiedStates(vehicleId))
        if (plan.toNotify.isNotEmpty()) notifier.notify(plan.toNotify)
        reminderRepository.replaceNotifiedStates(vehicleId, plan.notifiedStates)

        val inspectionStage = InspectionReminderStage.next(
            overview.inspection,
            inspectionRepository.notifiedStage(vehicleId),
        )
        if (inspectionStage != null) {
            notifier.notifyInspection(overview.inspection)
            inspectionRepository.markNotified(vehicleId, inspectionStage)
        }
        return plan.toNotify.size + if (inspectionStage != null) 1 else 0
    }
}
