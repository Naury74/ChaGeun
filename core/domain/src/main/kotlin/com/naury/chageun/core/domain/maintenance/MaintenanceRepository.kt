package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.ServiceHistoryEntry
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

    /** Newest first; undated entries follow dated ones. */
    fun observeServiceHistory(vehicleId: VehicleId, item: MaintenanceItem): Flow<List<ServiceHistoryEntry>>

    suspend fun findRule(vehicleId: VehicleId, item: MaintenanceItem): MaintenanceRule?

    suspend fun findLatestService(vehicleId: VehicleId, item: MaintenanceItem): ServiceRecord?

    suspend fun findCurrentMileage(vehicleId: VehicleId): MileageReading?

    /**
     * Stores [entry] and, when [advancesOdometer] is true, a mileage reading sourced from this service,
     * in a single transaction.
     */
    suspend fun recordService(vehicleId: VehicleId, entry: ServiceEntry, advancesOdometer: Boolean)
}
