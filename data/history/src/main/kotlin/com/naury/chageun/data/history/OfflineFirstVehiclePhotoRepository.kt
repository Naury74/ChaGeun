package com.naury.chageun.data.history

import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.storage.AttachmentDirectory
import com.naury.chageun.core.database.dao.AttachmentDao
import com.naury.chageun.core.database.entity.AttachmentEntity
import com.naury.chageun.core.domain.vehicle.VehiclePhotoRepository
import com.naury.chageun.core.model.VehicleId
import java.io.File
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 대표 사진을 attachment 테이블에 owner_type [OWNER_TYPE]으로 저장한다.
 * 가져오기·삭제·백업 경로를 기록 첨부와 공유하고, Timeline 조회는 이 owner_type을 무시한다.
 *
 * 가져올 때 차만 잘라 낸 투명 PNG를 원본 옆에 함께 만든다. 원본은 항상 JPEG이므로, 관찰하는 쪽은
 * 확장자가 PNG면 배경을 잘라 낸 사진으로 본다. 잘라 낸 파일은 기기에서 다시 만들 수 있어 백업하지 않는다.
 */
internal class OfflineFirstVehiclePhotoRepository @Inject constructor(
    private val attachmentDao: AttachmentDao,
    private val images: VehiclePhotoImages,
    @param:AttachmentDirectory private val directory: File,
    private val clock: Clock,
    private val logger: AppLogger,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : VehiclePhotoRepository {

    override fun observe(vehicleId: VehicleId): Flow<String?> =
        attachmentDao.observe(vehicleId.value, OWNER_TYPE, vehicleId.value).map { rows ->
            rows.maxByOrNull { it.createdAt }?.let { row ->
                val cutout = row.cutoutFile()
                (if (cutout.exists()) cutout else File(directory, row.fileName)).absolutePath
            }
        }

    override suspend fun replace(vehicleId: VehicleId, sourceUri: String): Boolean = withContext(ioDispatcher) {
        val id = UUID.randomUUID().toString()
        val imported = runCatching {
            images.importer.import(
                sourceUri,
                File(directory, "$id.jpg"),
                File(directory, "${id}_thumb.jpg"),
                ImageImporter.DEFAULT_EDGE_PX,
            )
        }.onFailure { logger.warn("vehicle_photo_import_failed", error = it) }.getOrNull()
            ?: return@withContext false
        runCatching { images.cutter.cutout(imported.file, File(directory, "$id$CUTOUT_SUFFIX")) }
            .onFailure { logger.warn("vehicle_photo_cutout_failed", error = it) }
        val previous = attachmentDao.findFor(vehicleId.value, OWNER_TYPE, vehicleId.value)
        attachmentDao.insert(
            AttachmentEntity(
                id = id,
                vehicleId = vehicleId.value,
                ownerType = OWNER_TYPE,
                ownerId = vehicleId.value,
                fileName = imported.file.name,
                thumbnailName = imported.thumbnail.name,
                mimeType = MIME_JPEG,
                sizeBytes = imported.sizeBytes,
                createdAt = clock.instant(),
            ),
        )
        previous.forEach { row ->
            attachmentDao.delete(vehicleId.value, row.id)
            row.deleteFiles()
        }
        true
    }

    override suspend fun clear(vehicleId: VehicleId) = withContext(ioDispatcher) {
        val rows = attachmentDao.findFor(vehicleId.value, OWNER_TYPE, vehicleId.value)
        attachmentDao.deleteFor(vehicleId.value, OWNER_TYPE, vehicleId.value)
        rows.forEach { it.deleteFiles() }
    }

    private fun AttachmentEntity.deleteFiles() {
        File(directory, fileName).delete()
        File(directory, thumbnailName).delete()
        cutoutFile().delete()
    }

    private fun AttachmentEntity.cutoutFile() = File(directory, "${fileName.substringBeforeLast('.')}$CUTOUT_SUFFIX")

    private companion object {
        const val OWNER_TYPE = "VehiclePhoto"
        const val CUTOUT_SUFFIX = "_cutout.png"
    }
}

/** 차량 사진을 가져와 배경을 잘라 내는 두 단계를 묶는다. */
internal class VehiclePhotoImages @Inject constructor(val importer: ImageImporter, val cutter: SubjectCutter)
