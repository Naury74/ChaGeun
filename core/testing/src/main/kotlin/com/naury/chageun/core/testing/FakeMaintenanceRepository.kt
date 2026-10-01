package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.MaintenanceRepository
import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMaintenanceRepository : MaintenanceRepository {
    val inputs = MutableStateFlow(MaintenanceInputs(emptyList(), emptyMap(), emptyList()))

    override fun observeInputs(vehicleId: VehicleId): Flow<MaintenanceInputs> = inputs
}
