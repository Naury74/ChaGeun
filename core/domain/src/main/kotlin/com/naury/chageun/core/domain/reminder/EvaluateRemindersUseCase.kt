package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.settings.SettingsRepository
import com.naury.chageun.core.domain.vehicle.InspectionRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MaintenanceStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** 정비·검사 알림을 띄운다. 구현은 플랫폼 레이어가 맡는다. */
interface ReminderNotifier {
    fun canNotify(): Boolean

    fun notify(statuses: List<MaintenanceStatus>)

    fun notifyInspection(status: InspectionStatus)

    fun cancelInspection()
}

class EvaluateRemindersUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val observeOverview: ObserveMaintenanceOverviewUseCase,
    private val reminderRepository: ReminderRepository,
    private val inspectionRepository: InspectionRepository,
    private val notifier: ReminderNotifier,
    private val settingsRepository: SettingsRepository,
) {
    /** @return 띄운 알림 수. */
    suspend operator fun invoke(): Int {
        // 알림을 실제로 띄울 수 있을 때만 단계를 기록하므로, 나중에 권한을 허용해도
        // 사용자가 놓친 알림이 전달된다.
        if (!notifier.canNotify()) return 0
        if (!settingsRepository.settings.first().isMaintenanceReminderEnabled) return 0
        val vehicle = vehicleRepository.observePrimaryVehicle().first() ?: return 0
        val overview = observeOverview(vehicle.id).first()
        val plan = ReminderPlanner.plan(overview.statuses, reminderRepository.notifiedStates(vehicle.id))
        if (plan.toNotify.isNotEmpty()) notifier.notify(plan.toNotify)
        reminderRepository.replaceNotifiedStates(vehicle.id, plan.notifiedStates)

        val inspectionStage = InspectionReminderStage.next(
            overview.inspection,
            inspectionRepository.notifiedStage(vehicle.id),
        )
        if (inspectionStage != null) {
            notifier.notifyInspection(overview.inspection)
            inspectionRepository.markNotified(vehicle.id, inspectionStage)
        }
        return plan.toNotify.size + if (inspectionStage != null) 1 else 0
    }
}
