package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.flow.Flow

data class MaintenanceInputs(
    val rules: List<MaintenanceRule>,
    val lastServices: Map<MaintenanceItem, ServiceRecord>,
    val mileageHistory: List<MileageReading>,
)

interface MaintenanceRepository {
    /** Emits again whenever rules, service records or mileage readings of the vehicle change. */
    fun observeInputs(vehicleId: VehicleId): Flow<MaintenanceInputs>
}
