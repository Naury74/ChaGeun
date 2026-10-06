package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.VehicleId

interface ReminderRepository {
    suspend fun notifiedStages(vehicleId: VehicleId): Map<MaintenanceItem, MaintenanceReminderStage>

    suspend fun replaceNotifiedStages(vehicleId: VehicleId, stages: Map<MaintenanceItem, MaintenanceReminderStage>)
}
