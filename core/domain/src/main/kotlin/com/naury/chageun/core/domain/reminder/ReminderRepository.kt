package com.naury.chageun.core.domain.reminder

import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.VehicleId

interface ReminderRepository {
    suspend fun notifiedStates(vehicleId: VehicleId): Map<MaintenanceItem, MaintenanceState>

    suspend fun replaceNotifiedStates(vehicleId: VehicleId, states: Map<MaintenanceItem, MaintenanceState>)
}
