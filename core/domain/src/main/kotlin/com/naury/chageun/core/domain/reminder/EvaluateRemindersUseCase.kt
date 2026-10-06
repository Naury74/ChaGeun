package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.settings.SettingsRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** 정비·검사 알림을 띄운다. 구현은 플랫폼 레이어가 맡는다. */
interface ReminderNotifier {
    fun canNotify(): Boolean

    fun notify(statuses: List<MaintenanceStatus>)

    /** 이미 처리한 항목의 알림을 알림 창에서 지운다. */
    fun cancel(item: MaintenanceItem)

    fun notifyInspection(status: InspectionStatus)

    fun cancelInspection()

    fun notifyMileagePrompt()
}

class EvaluateRemindersUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val observeOverview: ObserveMaintenanceOverviewUseCase,
    private val notifier: ReminderNotifier,
    private val settingsRepository: SettingsRepository,
    private val notifyDueItems: NotifyDueItemsUseCase,
    private val promptMileageUpdate: PromptMileageUpdateUseCase,
) {
    /** @return 띄운 알림 수. */
    suspend operator fun invoke(): Int {
        // 알림을 실제로 띄울 수 있을 때만 단계를 기록하므로, 나중에 권한을 허용해도
        // 사용자가 놓친 알림이 전달된다.
        if (!notifier.canNotify()) return 0
        val settings = settingsRepository.settings.first()
        val vehicle = vehicleRepository.observePrimaryVehicle().first() ?: return 0
        val overview = observeOverview(vehicle.id).first()
        var shown = 0
        shown += notifyDueItems(
            vehicle.id,
            overview,
            includeMaintenance = settings.isMaintenanceReminderEnabled,
            includeInspection = settings.isInspectionReminderEnabled,
        )
        if (settings.isMileageReminderEnabled && promptMileageUpdate(overview.currentMileage?.date)) shown++
        return shown
    }
}
