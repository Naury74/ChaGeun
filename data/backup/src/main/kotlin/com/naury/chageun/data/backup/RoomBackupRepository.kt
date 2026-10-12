package com.naury.chageun.data.backup

import android.content.Context
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.common.storage.AttachmentDirectory
import com.naury.chageun.core.common.storage.PROFILE_PHOTO_DIRECTORY
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.AlbumPhotoEntity
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

    override suspend fun export(destinationUri: String, includeVideos: Boolean): Boolean = withContext(ioDispatcher) {
        runCatching {
            val document = snapshot(includeVideos)
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
        leftoverPhotoDirectories().forEach(File::deleteRecursively)
        Unit
    }

    // 모두 지운다고 안내하므로 사진 편집기(FrameKit)의 세션·결과 파일과 가져오기 전 임시 사진도 함께 지운다.
    // 받아 둔 계정 프로필 사진도 지운다. 로그인이 남아 있으면 홈을 다시 열 때 새로 받는다.
    private fun leftoverPhotoDirectories() = listOf(
        File(context.filesDir, FRAMEKIT_DIRECTORY),
        File(context.filesDir, PROFILE_PHOTO_DIRECTORY),
        File(context.cacheDir, FRAMEKIT_DIRECTORY),
        File(context.cacheDir, PHOTO_INPUT_DIRECTORY),
    )

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
            val keptVideos = localVideosMissingFrom(document)
            replaceDatabase(document, keptVideos)
            keepVideoFiles(keptVideos, staging)
            attachmentDirectory.deleteRecursively()
            val moved = !staging.exists() || staging.renameTo(attachmentDirectory)
            if (!moved) staging.copyRecursively(attachmentDirectory, overwrite = true)
            true
        }.onFailure { logger.warn("backup_import_failed", LogField.Success(false), error = it) }
        staging.deleteRecursively()
        result.getOrDefault(false)
    }

    /**
     * 드라이브 백업은 크기 때문에 앨범 동영상을 넣지 않는다. 전체 교체로 복원하면 이 기기의 동영상이 사라지므로,
     * 백업에 없는 동영상 중 그 차가 백업에 있는 것은 그대로 남긴다.
     */
    private suspend fun localVideosMissingFrom(document: BackupDocument): List<AlbumPhotoEntity> {
        val vehicleIds = document.vehicles.mapTo(HashSet()) { it.id }
        val restoredIds = document.albumPhotos.mapTo(HashSet()) { it.id }
        return backupDao.albumPhotos().filter {
            it.mediaType == AlbumPhotoEntity.MEDIA_VIDEO && it.vehicleId in vehicleIds && it.id !in restoredIds
        }
    }

    /** DB가 바뀐 뒤에 옮기므로, 그 전에 실패하면 기존 첨부 폴더가 그대로 남는다. 같은 저장소 안이라 이름만 바뀐다. */
    private fun keepVideoFiles(videos: List<AlbumPhotoEntity>, staging: File) {
        if (videos.isEmpty()) return
        staging.mkdirs()
        videos.flatMap { listOf(it.fileName, it.thumbnailName) }.forEach { name ->
            val source = File(attachmentDirectory, name)
            val target = File(staging, name)
            if (source.exists() && !source.renameTo(target)) source.copyTo(target, overwrite = true)
        }
    }

    private suspend fun replaceDatabase(document: BackupDocument, keptVideos: List<AlbumPhotoEntity> = emptyList()) {
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
            backupDao.insertAlbumPhotos(document.albumPhotos.map { it.toEntity(now) } + keptVideos)
        }
    }

    private fun openInput(uri: String): InputStream = checkNotNull(context.contentResolver.openInputStream(uri.toUri()))

    private fun BackupDocument.summary() = LocalDataSummary(
        vehicles = vehicles.size,
        records = maintenanceRecords.size + fuelRecords.size + checkRecords.size,
        photos = attachments.size + albumPhotos.size,
    )

    /** 동영상 파일은 ZIP에 한 덩어리씩 흘려 쓰므로 크기와 상관없이 메모리에 올리지 않는다. */
    internal suspend fun snapshot(includeVideos: Boolean = true): BackupDocument = database.withTransaction {
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
            albumPhotos = backupDao.albumPhotos()
                .filter { includeVideos || it.mediaType != AlbumPhotoEntity.MEDIA_VIDEO }
                .map { it.toDto() },
        )
    }

    private companion object {
        const val STAGING_DIRECTORY = "attachments-import"

        // FrameKit이 files·cache 아래에 만드는 폴더, core:ui PhotoFiles의 임시 폴더와 이름이 같아야 한다.
        const val FRAMEKIT_DIRECTORY = "framekit"
        const val PHOTO_INPUT_DIRECTORY = "photo_input"
    }
}
