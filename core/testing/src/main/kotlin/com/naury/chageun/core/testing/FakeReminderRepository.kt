package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.reminder.MaintenanceReminderStage
import com.naury.chageun.core.domain.reminder.ReminderRepository
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.VehicleId

class FakeReminderRepository : ReminderRepository {
    var stages: Map<MaintenanceItem, MaintenanceReminderStage> = emptyMap()

    override suspend fun notifiedStages(vehicleId: VehicleId) = stages

    override suspend fun replaceNotifiedStages(
        vehicleId: VehicleId,
        stages: Map<MaintenanceItem, MaintenanceReminderStage>,
    ) {
        this.stages = stages
    }
}
