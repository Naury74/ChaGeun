package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.reminder.ReminderRepository
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.VehicleId

class FakeReminderRepository : ReminderRepository {
    var states: Map<MaintenanceItem, MaintenanceState> = emptyMap()

    override suspend fun notifiedStates(vehicleId: VehicleId) = states

    override suspend fun replaceNotifiedStates(vehicleId: VehicleId, states: Map<MaintenanceItem, MaintenanceState>) {
        this.states = states
    }
}
