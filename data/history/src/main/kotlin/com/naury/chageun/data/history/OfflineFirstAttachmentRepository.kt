package com.naury.chageun.data.history

import android.content.Context
import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.database.dao.AttachmentDao
import com.naury.chageun.core.database.entity.AttachmentEntity
import com.naury.chageun.core.domain.history.AttachResult
import com.naury.chageun.core.domain.history.AttachmentRepository
import com.naury.chageun.core.domain.history.MAX_ATTACHMENTS_PER_RECORD
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.VehicleId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class OfflineFirstAttachmentRepository(
    private val attachmentDao: AttachmentDao,
    private val importer: ImageImporter,
    private val directory: File,
    private val clock: Clock,
    private val logger: AppLogger,
    private val ioDispatcher: CoroutineDispatcher,
) : AttachmentRepository {

    @Inject
    constructor(
        attachmentDao: AttachmentDao,
        importer: ImageImporter,
        @ApplicationContext context: Context,
        clock: Clock,
        logger: AppLogger,
        @Dispatcher(ChageunDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    ) : this(attachmentDao, importer, File(context.filesDir, DIRECTORY), clock, logger, ioDispatcher)

    override fun observe(vehicleId: VehicleId, owner: RecordRef): Flow<List<Attachment>> =
        attachmentDao.observe(vehicleId.value, owner.type.name, owner.id).map { rows ->
            rows.mapNotNull { it.asExternalModel() }
        }

    override suspend fun attach(vehicleId: VehicleId, owner: RecordRef, sourceUris: List<String>): AttachResult =
        withContext(ioDispatcher) {
            val room = MAX_ATTACHMENTS_PER_RECORD - attachmentDao.count(vehicleId.value, owner.type.name, owner.id)
            var added = 0
            sourceUris.take(room.coerceAtLeast(0)).forEach { uri ->
                val id = UUID.randomUUID().toString()
                val imported = runCatching {
                    importer.import(uri, File(directory, "$id.jpg"), File(directory, "${id}_thumb.jpg"))
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

    private companion object {
        const val DIRECTORY = "attachments"
        const val MIME_JPEG = "image/jpeg"
    }
}
