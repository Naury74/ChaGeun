package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.settings.SettingsRepository
import com.naury.chageun.core.domain.vehicle.InspectionRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MaintenanceStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Shows maintenance and inspection notifications; implemented by the platform layer. */
interface ReminderNotifier {
    fun canNotify(): Boolean

    fun notify(statuses: List<MaintenanceStatus>)

    fun notifyInspection(status: InspectionStatus)
}

class EvaluateRemindersUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val observeOverview: ObserveMaintenanceOverviewUseCase,
    private val reminderRepository: ReminderRepository,
    private val inspectionRepository: InspectionRepository,
    private val notifier: ReminderNotifier,
    private val settingsRepository: SettingsRepository,
) {
    /** @return the number of notifications shown. */
    suspend operator fun invoke(): Int {
        // Stages are only recorded when notifications can actually be shown, so granting the
        // permission later still delivers what the user missed.
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
