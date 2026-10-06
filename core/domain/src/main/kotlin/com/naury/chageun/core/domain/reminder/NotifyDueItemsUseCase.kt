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
        val alreadyNotified = reminderRepository.notifiedStates(vehicleId)
        val plan = ReminderPlanner.plan(overview.statuses, alreadyNotified)
        if (plan.toNotify.isNotEmpty()) notifier.notify(plan.toNotify)
        // 기록 수정이나 주기 변경처럼 저장 화면을 거치지 않고 정상으로 돌아온 항목도 알림 창에 남기지 않는다.
        (alreadyNotified.keys - plan.notifiedStates.keys).forEach(notifier::cancel)
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
