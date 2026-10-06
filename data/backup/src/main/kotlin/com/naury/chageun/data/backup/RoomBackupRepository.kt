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
import com.naury.chageun.core.domain.backup.ImportPreview
import com.naury.chageun.core.domain.backup.LocalDataSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
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
            photos = backupDao.attachments().size + backupDao.albumPhotos().size,
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

    override suspend fun previewImport(sourceUri: String): ImportPreview = withContext(ioDispatcher) {
        val content = runCatching {
            openInput(sourceUri).use { BackupArchiveReader.read(it) }
        }.getOrDefault(ArchiveContent.Invalid)
        when (content) {
            is ArchiveContent.Valid -> ImportPreview.Ready(content.document.summary(), summary())
            is ArchiveContent.UnsupportedVersion -> ImportPreview.UnsupportedVersion(content.version)
            ArchiveContent.Invalid -> ImportPreview.Invalid
        }
    }

    override suspend fun import(sourceUri: String): Boolean = withContext(ioDispatcher) {
        val staging = File(attachmentDirectory.parentFile, STAGING_DIRECTORY).apply { deleteRecursively() }
        val result = runCatching {
            val content = openInput(sourceUri).use { BackupArchiveReader.read(it, extractTo = staging) }
            val document = (content as? ArchiveContent.Valid)?.document ?: return@runCatching false
            replaceDatabase(document)
            attachmentDirectory.deleteRecursively()
            val moved = !staging.exists() || staging.renameTo(attachmentDirectory)
            if (!moved) staging.copyRecursively(attachmentDirectory, overwrite = true)
            true
        }.onFailure { logger.warn("backup_import_failed", LogField.Success(false), error = it) }
        staging.deleteRecursively()
        result.getOrDefault(false)
    }

    private suspend fun replaceDatabase(document: BackupDocument) {
        val now = clock.instant()
        database.withTransaction {
            backupDao.deleteAllVehicles()
            backupDao.insertVehicleData(
                vehicles = document.vehicles.map { it.toEntity(now) },
                mileage = document.mileage.map { it.toEntity() },
                rules = document.maintenanceRules.map { it.toEntity() },
                inspections = document.inspectionSchedules.map { it.toEntity() },
            )
            backupDao.insertRecords(
                maintenance = document.maintenanceRecords.map { it.toEntity(now) },
                fuel = document.fuelRecords.map { it.toEntity(now) },
                checks = document.checkRecords.map { it.toEntity(now) },
                attachments = document.attachments.map { it.toEntity() },
            )
            backupDao.insertAlbumPhotos(document.albumPhotos.map { it.toEntity(now) })
        }
    }

    private fun openInput(uri: String): InputStream = checkNotNull(context.contentResolver.openInputStream(uri.toUri()))

    private fun BackupDocument.summary() = LocalDataSummary(
        vehicles = vehicles.size,
        records = maintenanceRecords.size + fuelRecords.size + checkRecords.size,
        photos = attachments.size + albumPhotos.size,
    )

    internal suspend fun snapshot(): BackupDocument = database.withTransaction {
        BackupDocument(
            exportedAt = clock.instant().toString(),
            vehicles = backupDao.vehicles().map { it.toDto() },
            mileage = backupDao.mileage().map { it.toDto() },
            maintenanceRules = backupDao.rules().map { it.toDto() },
            maintenanceRecords = backupDao.maintenanceRecords().map { it.toDto() },
            fuelRecords = backupDao.fuelRecords().map { it.toDto() },
            checkRecords = backupDao.checkRecords().map { it.toDto() },
            attachments = backupDao.attachments().map { it.toDto() },
            inspectionSchedules = backupDao.inspectionSchedules().map { it.toDto() },
            albumPhotos = backupDao.albumPhotos().map { it.toDto() },
        )
    }

    private companion object {
        const val STAGING_DIRECTORY = "attachments-import"
    }
}
