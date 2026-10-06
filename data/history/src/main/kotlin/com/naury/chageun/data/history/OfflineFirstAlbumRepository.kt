package com.naury.chageun.data.history

import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.storage.AttachmentDirectory
import com.naury.chageun.core.database.dao.AlbumDao
import com.naury.chageun.core.database.entity.AlbumPhotoEntity
import com.naury.chageun.core.domain.album.AlbumAddResult
import com.naury.chageun.core.domain.album.AlbumRepository
import com.naury.chageun.core.domain.album.MAX_ALBUM_PHOTOS_PER_ADD
import com.naury.chageun.core.model.AlbumPhoto
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

internal class OfflineFirstAlbumRepository @Inject constructor(
    private val albumDao: AlbumDao,
    private val importer: ImageImporter,
    @param:AttachmentDirectory private val directory: File,
    private val clock: Clock,
    private val logger: AppLogger,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AlbumRepository {

    override fun observe(vehicleId: VehicleId): Flow<List<AlbumPhoto>> =
        albumDao.observe(vehicleId.value).map { rows -> rows.map { it.asExternalModel() } }

    override suspend fun add(
        vehicleId: VehicleId,
        sourceUris: List<String>,
        fallbackDate: LocalDate,
        highQuality: Boolean,
    ): AlbumAddResult = withContext(ioDispatcher) {
        val maxEdge = if (highQuality) ImageImporter.HIGH_QUALITY_EDGE_PX else ImageImporter.DEFAULT_EDGE_PX
        val added = sourceUris.take(MAX_ALBUM_PHOTOS_PER_ADD).mapNotNull { uri ->
            val id = UUID.randomUUID().toString()
            val imported = runCatching {
                importer.import(
                    uri,
                    File(directory, "album_$id.jpg"),
                    File(directory, "album_${id}_thumb.jpg"),
                    maxEdge,
                )
            }.onFailure { logger.warn("album_import_failed", error = it) }.getOrNull() ?: return@mapNotNull null
            val now = clock.instant()
            albumDao.insert(
                AlbumPhotoEntity(
                    id = id,
                    vehicleId = vehicleId.value,
                    fileName = imported.file.name,
                    thumbnailName = imported.thumbnail.name,
                    sizeBytes = imported.sizeBytes,
                    takenOn = imported.takenOn ?: fallbackDate,
                    comment = null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            id
        }
        AlbumAddResult(addedIds = added, failed = sourceUris.size - added.size)
    }

    override suspend fun updateDetails(vehicleId: VehicleId, id: String, takenOn: LocalDate, comment: String?) =
        albumDao.updateDetails(vehicleId.value, id, takenOn, comment?.trim()?.ifEmpty { null }, clock.instant())

    override suspend fun delete(vehicleId: VehicleId, id: String) = withContext(ioDispatcher) {
        val photo = albumDao.find(vehicleId.value, id) ?: return@withContext
        albumDao.delete(vehicleId.value, id)
        File(directory, photo.fileName).delete()
        File(directory, photo.thumbnailName).delete()
        Unit
    }

    private fun AlbumPhotoEntity.asExternalModel() = AlbumPhoto(
        id = id,
        filePath = File(directory, fileName).path,
        thumbnailPath = File(directory, thumbnailName).path,
        takenOn = takenOn,
        comment = comment,
        createdAt = createdAt,
    )
}
