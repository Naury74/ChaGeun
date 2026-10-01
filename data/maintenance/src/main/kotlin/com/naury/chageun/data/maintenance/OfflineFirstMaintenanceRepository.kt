package com.naury.chageun.data.maintenance

import androidx.room.withTransaction
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MileageRecordEntity
import com.naury.chageun.core.domain.maintenance.MaintenanceInputs
import com.naury.chageun.core.domain.maintenance.MaintenanceRepository
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.ServiceHistoryEntry
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal class OfflineFirstMaintenanceRepository @Inject constructor(
    private val database: ChageunDatabase,
    private val clock: Clock,
) : MaintenanceRepository {

    private val maintenanceDao get() = database.maintenanceDao()
    private val mileageRecordDao get() = database.mileageRecordDao()

    override fun observeInputs(vehicleId: VehicleId): Flow<MaintenanceInputs> = combine(
        maintenanceDao.observeEnabledRules(vehicleId.value),
        maintenanceDao.observeLatestRecords(vehicleId.value),
        mileageRecordDao.observeAll(vehicleId.value),
    ) { rules, latestRecords, mileage ->
        MaintenanceInputs(
            rules = rules.mapNotNull { it.asExternalModelOrNull() },
            lastServices = latestRecords
                .mapNotNull { record ->
                    record.itemType.toMaintenanceItemOrNull()?.let { it to record.asServiceRecord() }
                }
                .toMap(),
            mileageHistory = mileage.map { it.asExternalModel() },
        )
    }

    override fun observeServiceHistory(vehicleId: VehicleId, item: MaintenanceItem): Flow<List<ServiceHistoryEntry>> =
        maintenanceDao.observeRecords(vehicleId.value, item.name).map { records ->
            records.map {
                ServiceHistoryEntry(
                    id = it.id,
                    date = it.serviceDate,
                    mileage = it.mileageKm?.let(::Kilometers),
                    costWon = it.costWon,
                    shopName = it.shopName,
                )
            }
        }

    override suspend fun findRule(vehicleId: VehicleId, item: MaintenanceItem): MaintenanceRule? =
        maintenanceDao.findRule(vehicleId.value, item.name)?.asExternalModelOrNull()

    override suspend fun findLatestService(vehicleId: VehicleId, item: MaintenanceItem): ServiceRecord? =
        maintenanceDao.findLatestRecord(vehicleId.value, item.name)?.asServiceRecord()

    override suspend fun findCurrentMileage(vehicleId: VehicleId): MileageReading? =
        mileageRecordDao.findLatest(vehicleId.value)?.asExternalModel()

    override suspend fun recordService(vehicleId: VehicleId, entry: ServiceEntry, advancesOdometer: Boolean) {
        val now = clock.instant()
        val record = MaintenanceRecordEntity(
            id = UUID.randomUUID().toString(),
            vehicleId = vehicleId.value,
            itemType = entry.item.name,
            serviceDate = entry.date,
            mileageKm = entry.mileage.value,
            costWon = entry.costWon,
            shopName = entry.shopName?.trim()?.ifEmpty { null },
            memo = entry.memo?.trim()?.ifEmpty { null },
            sourceType = SOURCE_USER,
            createdAt = now,
            updatedAt = now,
        )
        database.withTransaction {
            maintenanceDao.insertRecord(record)
            if (advancesOdometer) {
                mileageRecordDao.insert(
                    MileageRecordEntity(
                        id = UUID.randomUUID().toString(),
                        vehicleId = vehicleId.value,
                        mileageKm = entry.mileage.value,
                        recordedOn = entry.date,
                        sourceType = MILEAGE_SOURCE_MAINTENANCE,
                        relatedRecordId = record.id,
                        createdAt = now,
                    ),
                )
            }
        }
    }

    private companion object {
        const val SOURCE_USER = "USER"
        const val MILEAGE_SOURCE_MAINTENANCE = "MAINTENANCE"
    }
}
