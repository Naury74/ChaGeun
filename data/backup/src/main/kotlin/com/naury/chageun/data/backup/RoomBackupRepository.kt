package com.naury.chageun.data.backup

import android.content.Context
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.common.storage.AttachmentDirectory
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.domain.backup.BackupRepository
import com.naury.chageun.core.domain.backup.LocalDataSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.OutputStream
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class RoomBackupRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: ChageunDatabase,
    @param:AttachmentDirectory private val attachmentDirectory: File,
    private val clock: Clock,
    private val logger: AppLogger,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : BackupRepository {

    private val backupDao get() = database.backupDao()

    override suspend fun summary(): LocalDataSummary = withContext(ioDispatcher) {
        LocalDataSummary(
            vehicles = backupDao.vehicles().size,
            records = backupDao.recordCount(),
            photos = backupDao.attachments().size,
        )
    }

    override suspend fun export(destinationUri: String): Boolean = withContext(ioDispatcher) {
        runCatching {
            val document = snapshot()
            val output: OutputStream = checkNotNull(context.contentResolver.openOutputStream(destinationUri.toUri()))
            output.use { BackupArchiveWriter.write(it, document, attachmentDirectory) }
        }.fold(
            onSuccess = {
                logger.debug("backup_exported", LogField.Success(true))
                true
            },
            onFailure = {
                logger.warn("backup_export_failed", LogField.Success(false), error = it)
                false
            },
        )
    }

    override suspend fun deleteAll() = withContext(ioDispatcher) {
        database.withTransaction { backupDao.deleteAllVehicles() }
        attachmentDirectory.deleteRecursively()
        Unit
    }

    internal suspend fun snapshot(): BackupDocument = database.withTransaction {
        BackupDocument(
            exportedAt = clock.instant().toString(),
            vehicles = backupDao.vehicles().map {
                VehicleDto(
                    id = it.id,
                    maker = it.maker,
                    model = it.model,
                    modelYear = it.modelYear,
                    trim = it.trim,
                    fuelType = it.fuelType,
                    firstRegistrationDate = it.firstRegistrationDate?.toString(),
                    plateMasked = it.plateMasked,
                    registrationMode = it.registrationMode,
                )
            },
            mileage = backupDao.mileage().map {
                MileageDto(it.id, it.vehicleId, it.mileageKm, it.recordedOn.toString(), it.sourceType)
            },
            maintenanceRules = backupDao.rules().map {
                RuleDto(it.vehicleId, it.itemType, it.intervalKm, it.intervalMonths, it.ruleSource, it.isEnabled)
            },
            maintenanceRecords = backupDao.maintenanceRecords().map {
                MaintenanceDto(
                    it.id,
                    it.vehicleId,
                    it.itemType,
                    it.serviceDate?.toString(),
                    it.mileageKm,
                    it.costWon,
                    it.shopName,
                    it.memo,
                )
            },
            fuelRecords = backupDao.fuelRecords().map {
                FuelDto(
                    it.id, it.vehicleId, it.fuelDate.toString(), it.mileageKm, it.totalPriceWon, it.volumeMl,
                    it.unitPriceWon, it.isFullTank, it.stationName, it.memo,
                )
            },
            checkRecords = backupDao.checkRecords().map {
                CheckDto(
                    it.id,
                    it.vehicleId,
                    it.kind,
                    it.checkDate.toString(),
                    it.title,
                    it.mileageKm,
                    it.costWon,
                    it.memo,
                )
            },
            attachments = backupDao.attachments().map { AttachmentDto(it.ownerType, it.ownerId, it.fileName) },
        )
    }
}
