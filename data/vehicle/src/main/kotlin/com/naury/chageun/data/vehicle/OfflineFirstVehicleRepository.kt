package com.naury.chageun.data.vehicle

import androidx.room.withTransaction
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MileageRecordEntity
import com.naury.chageun.core.database.entity.VehicleEntity
import com.naury.chageun.core.domain.maintenance.DefaultMaintenanceRules
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.MileageSource
import com.naury.chageun.core.model.RegistrationMode
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.security.FieldCipher
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class OfflineFirstVehicleRepository @Inject constructor(
    private val database: ChageunDatabase,
    private val cipher: FieldCipher,
    private val clock: Clock,
) : VehicleRepository {

    override fun observePrimaryVehicle(): Flow<Vehicle?> =
        database.vehicleDao().observePrimary().map { it?.asExternalModel() }

    override fun observeMileageLog(vehicleId: VehicleId): Flow<List<MileageEntry>> =
        database.mileageRecordDao().observeAll(vehicleId.value).map { rows ->
            rows.asReversed().map { row ->
                MileageEntry(
                    id = row.id,
                    date = row.recordedOn,
                    mileage = Kilometers(row.mileageKm),
                    source = MILEAGE_SOURCES[row.sourceType] ?: MileageSource.User,
                )
            }
        }

    override suspend fun register(registration: VehicleRegistration): VehicleId {
        val vehicleId = UUID.randomUUID().toString()
        val now = clock.instant()
        val vehicle = VehicleEntity(
            id = vehicleId,
            plateNumberEncrypted = registration.plate?.let { cipher.encrypt(it.normalized) },
            plateMasked = registration.plate?.masked,
            maker = registration.maker.trim(),
            model = registration.model.trim(),
            modelYear = registration.modelYear,
            trim = registration.trim?.trim()?.ifEmpty { null },
            fuelType = registration.fuelType.name,
            firstRegistrationDate = registration.firstRegistrationDate,
            vinEncrypted = null,
            registrationMode = RegistrationMode.Manual.name,
            isPrimary = true,
            createdAt = now,
            updatedAt = now,
        )
        val startingMileage = MileageRecordEntity(
            id = UUID.randomUUID().toString(),
            vehicleId = vehicleId,
            mileageKm = registration.currentMileage.value,
            recordedOn = LocalDate.now(clock),
            sourceType = SOURCE_USER,
            relatedRecordId = null,
            createdAt = now,
        )
        val rules = DefaultMaintenanceRules.forFuel(registration.fuelType)
            .map { it.asEntity(id = UUID.randomUUID().toString(), vehicleId = vehicleId) }

        val knownServices = registration.knownServices
            .filterValues { it.date != null || it.mileage != null }
            .map { (item, record) ->
                MaintenanceRecordEntity(
                    id = UUID.randomUUID().toString(),
                    vehicleId = vehicleId,
                    itemType = item.name,
                    serviceDate = record.date,
                    mileageKm = record.mileage?.value,
                    costWon = null,
                    shopName = null,
                    memo = null,
                    sourceType = SOURCE_USER,
                    createdAt = now,
                    updatedAt = now,
                )
            }

        database.withTransaction {
            database.vehicleDao().clearPrimary()
            database.vehicleDao().upsert(vehicle)
            database.mileageRecordDao().insert(startingMileage)
            database.maintenanceDao().upsertRules(rules)
            knownServices.forEach { database.maintenanceDao().insertRecord(it) }
        }
        return VehicleId(vehicleId)
    }

    private companion object {
        const val SOURCE_USER = "USER"
        val MILEAGE_SOURCES = mapOf(
            "USER" to MileageSource.User,
            "MAINTENANCE" to MileageSource.Maintenance,
            "FUEL" to MileageSource.Fuel,
            "CHECK" to MileageSource.Check,
            "CORRECTION" to MileageSource.Correction,
            "INSPECTION" to MileageSource.Inspection,
        )
    }
}
