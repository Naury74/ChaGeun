package com.naury.chageun.data.history

import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.common.storage.AttachmentDirectory
import com.naury.chageun.core.database.dao.AttachmentDao
import com.naury.chageun.core.database.entity.AttachmentEntity
import com.naury.chageun.core.domain.history.AttachResult
import com.naury.chageun.core.domain.history.AttachmentRepository
import com.naury.chageun.core.domain.history.MAX_ATTACHMENTS_PER_RECORD
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.RecordPhoto
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.VehicleId
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class OfflineFirstAttachmentRepository @Inject constructor(
    private val attachmentDao: AttachmentDao,
    private val importer: ImageImporter,
    @param:AttachmentDirectory private val directory: File,
    private val clock: Clock,
    private val logger: AppLogger,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AttachmentRepository {

    override fun observe(vehicleId: VehicleId, owner: RecordRef): Flow<List<Attachment>> =
        attachmentDao.observe(vehicleId.value, owner.type.name, owner.id).map { rows ->
            rows.mapNotNull { it.asExternalModel() }
        }

    override fun observeBetween(vehicleId: VehicleId, from: LocalDate, to: LocalDate): Flow<List<RecordPhoto>> =
        attachmentDao.observeBetween(vehicleId.value, from, to).map { rows ->
            rows.mapNotNull { row -> row.attachment.asExternalModel()?.let { RecordPhoto(it, row.occurredOn) } }
        }

    override suspend fun attach(
        vehicleId: VehicleId,
        owner: RecordRef,
        sourceUris: List<String>,
        highQuality: Boolean,
    ): AttachResult = withContext(ioDispatcher) {
        val room = MAX_ATTACHMENTS_PER_RECORD - attachmentDao.count(vehicleId.value, owner.type.name, owner.id)
        var added = 0
        sourceUris.take(room.coerceAtLeast(0)).forEach { uri ->
            val id = UUID.randomUUID().toString()
            val imported = runCatching {
                importer.import(
                    uri,
                    File(directory, "$id.jpg"),
                    File(directory, "${id}_thumb.jpg"),
                    if (highQuality) ImageImporter.HIGH_QUALITY_EDGE_PX else ImageImporter.DEFAULT_EDGE_PX,
                )
            }.onFailure { logger.warn("attachment_import_failed", error = it) }.getOrNull()
            if (imported != null) {
                attachmentDao.insert(
                    AttachmentEntity(
                        id = id,
                        vehicleId = vehicleId.value,
                        ownerType = owner.type.name,
                        ownerId = owner.id,
                        fileName = imported.file.name,
                        thumbnailName = imported.thumbnail.name,
                        mimeType = MIME_JPEG,
                        sizeBytes = imported.sizeBytes,
                        createdAt = clock.instant(),
                    ),
                )
                added++
            }
        }
        AttachResult(added = added, failed = sourceUris.size - added).also {
            logger.debug("attachment_added", LogField.Success(it.failed == 0))
        }
    }

    override suspend fun replace(vehicleId: VehicleId, attachmentId: String, sourceUri: String): Boolean =
        withContext(ioDispatcher) {
            val row = attachmentDao.find(vehicleId.value, attachmentId) ?: return@withContext false
            // 이름을 새로 지어 이미지 캐시가 예전 사진을 보여 주지 않게 하고, 다 쓴 뒤에 옛 파일을 지운다.
            val name = UUID.randomUUID().toString()
            val imported = runCatching {
                importer.import(
                    sourceUri,
                    File(directory, "$name.jpg"),
                    File(directory, "${name}_thumb.jpg"),
                    ImageImporter.HIGH_QUALITY_EDGE_PX,
                )
            }.onFailure { logger.warn("attachment_replace_failed", error = it) }.getOrNull()
            if (imported != null) {
                attachmentDao.update(
                    row.copy(
                        fileName = imported.file.name,
                        thumbnailName = imported.thumbnail.name,
                        sizeBytes = imported.sizeBytes,
                    ),
                )
                row.deleteFiles()
            }
            logger.debug("attachment_replaced", LogField.Success(imported != null))
            imported != null
        }

    override suspend fun delete(vehicleId: VehicleId, attachmentId: String) = withContext(ioDispatcher) {
        attachmentDao.find(vehicleId.value, attachmentId)?.let { row ->
            attachmentDao.delete(vehicleId.value, attachmentId)
            row.deleteFiles()
        }
        Unit
    }

    override suspend fun deleteAllFor(vehicleId: VehicleId, owner: RecordRef) = withContext(ioDispatcher) {
        val rows = attachmentDao.findFor(vehicleId.value, owner.type.name, owner.id)
        attachmentDao.deleteFor(vehicleId.value, owner.type.name, owner.id)
        rows.forEach { it.deleteFiles() }
    }

    private fun AttachmentEntity.deleteFiles() {
        File(directory, fileName).delete()
        File(directory, thumbnailName).delete()
    }

    private fun AttachmentEntity.asExternalModel(): Attachment? {
        val type = TimelineEventType.entries.firstOrNull { it.name == ownerType } ?: return null
        return Attachment(
            id = id,
            owner = RecordRef(type, ownerId),
            filePath = File(directory, fileName).absolutePath,
            thumbnailPath = File(directory, thumbnailName).absolutePath,
            createdAt = createdAt,
        )
    }
}
