package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.MaintenanceRepository
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.ServiceHistoryEntry
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeMaintenanceRepository : MaintenanceRepository {
    val inputs = MutableStateFlow(MaintenanceInputs(emptyList(), emptyMap(), emptyList()))
    val recordedServices = mutableListOf<Pair<ServiceEntry, Boolean>>()
    val history = MutableStateFlow<Map<MaintenanceItem, List<ServiceHistoryEntry>>>(emptyMap())

    override fun observeInputs(vehicleId: VehicleId): Flow<MaintenanceInputs> = inputs

    override fun observeServiceHistory(vehicleId: VehicleId, item: MaintenanceItem): Flow<List<ServiceHistoryEntry>> =
        history.map { it[item].orEmpty() }

    override suspend fun findRule(vehicleId: VehicleId, item: MaintenanceItem): MaintenanceRule? =
        inputs.value.rules.firstOrNull { it.item == item }

    override suspend fun findLatestService(vehicleId: VehicleId, item: MaintenanceItem): ServiceRecord? =
        inputs.value.lastServices[item]

    override suspend fun findCurrentMileage(vehicleId: VehicleId): MileageReading? =
        inputs.value.mileageHistory.maxByOrNull { it.date }

    override suspend fun recordService(vehicleId: VehicleId, entry: ServiceEntry, advancesOdometer: Boolean) {
        recordedServices += entry to advancesOdometer
        inputs.update { current ->
            current.copy(
                lastServices = current.lastServices + (entry.item to ServiceRecord(entry.date, entry.mileage)),
                mileageHistory = if (advancesOdometer) {
                    current.mileageHistory + MileageReading(entry.date, entry.mileage)
                } else {
                    current.mileageHistory
                },
            )
        }
    }
}
