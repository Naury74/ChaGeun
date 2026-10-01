package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.settings.SettingsRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.MaintenanceStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Shows maintenance notifications; implemented by the platform layer. */
interface ReminderNotifier {
    fun canNotify(): Boolean

    fun notify(statuses: List<MaintenanceStatus>)
}

class EvaluateRemindersUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val observeOverview: ObserveMaintenanceOverviewUseCase,
    private val reminderRepository: ReminderRepository,
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
        return plan.toNotify.size
    }
}
