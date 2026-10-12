package com.naury.chageun.data.history

import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.common.storage.AttachmentDirectory
import com.naury.chageun.core.database.dao.AlbumDao
import com.naury.chageun.core.database.entity.AlbumPhotoEntity
import com.naury.chageun.core.domain.album.AlbumAddResult
import com.naury.chageun.core.domain.album.AlbumRepository
import com.naury.chageun.core.domain.album.MAX_ALBUM_PHOTOS_PER_ADD
import com.naury.chageun.core.domain.album.MAX_ALBUM_VIDEO_BYTES
import com.naury.chageun.core.domain.album.MAX_ALBUM_VIDEO_DURATION_MS
import com.naury.chageun.core.model.AlbumMediaType
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.VehicleId
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class OfflineFirstAlbumRepository @Inject constructor(
    private val albumDao: AlbumDao,
    private val importers: AlbumImporters,
    @param:AttachmentDirectory private val directory: File,
    private val clock: Clock,
    private val logger: AppLogger,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AlbumRepository {

    override fun observe(vehicleId: VehicleId): Flow<List<AlbumPhoto>> =
        albumDao.observe(vehicleId.value).map { rows -> rows.map { it.asExternalModel() } }

    override fun observeBetween(vehicleId: VehicleId, from: LocalDate, to: LocalDate): Flow<List<AlbumPhoto>> =
        albumDao.observeBetween(vehicleId.value, from, to).map { rows -> rows.map { it.asExternalModel() } }

    override suspend fun add(
        vehicleId: VehicleId,
        sourceUris: List<String>,
        fallbackDate: LocalDate,
        highQuality: Boolean,
    ): AlbumAddResult = withContext(ioDispatcher) {
        val maxEdge = if (highQuality) ImageImporter.HIGH_QUALITY_EDGE_PX else ImageImporter.DEFAULT_EDGE_PX
        val outcomes = sourceUris.take(MAX_ALBUM_PHOTOS_PER_ADD).map { uri ->
            val id = UUID.randomUUID().toString()
            val outcome = store(uri, "album_$id", maxEdge)
            if (outcome is StoreOutcome.Stored) {
                val now = clock.instant()
                val row = AlbumPhotoEntity(
                    id = id,
                    vehicleId = vehicleId.value,
                    fileName = "",
                    thumbnailName = "",
                    sizeBytes = 0,
                    takenOn = outcome.media.takenOn ?: fallbackDate,
                    comment = null,
                    createdAt = now,
                    updatedAt = now,
                )
                albumDao.insert(outcome.media.applyTo(row))
            }
            id to outcome
        }
        val added = outcomes.filter { it.second is StoreOutcome.Stored }.map { it.first }
        val tooLarge = outcomes.count { it.second == StoreOutcome.TooLarge }
        AlbumAddResult(addedIds = added, failed = sourceUris.size - added.size - tooLarge, tooLarge = tooLarge)
    }

    override suspend fun replace(vehicleId: VehicleId, id: String, sourceUri: String): Boolean =
        withContext(ioDispatcher) {
            val row = albumDao.find(vehicleId.value, id) ?: return@withContext false
            // 이름을 새로 지어 이미지 캐시가 예전 사진을 보여 주지 않게 하고, 다 쓴 뒤에 옛 파일을 지운다.
            // 편집한 사진은 원래보다 커지지 않으므로 고화질 상한으로 가져온다.
            val outcome = store(sourceUri, "album_${id}_${UUID.randomUUID()}", ImageImporter.HIGH_QUALITY_EDGE_PX)
            val stored = (outcome as? StoreOutcome.Stored)?.media
            if (stored != null) {
                // 찍은 날짜·메모는 사용자가 정한 값이라 편집한 파일의 날짜로 바꾸지 않는다.
                albumDao.update(stored.applyTo(row).copy(updatedAt = clock.instant()))
                row.deleteFiles()
            }
            logger.debug("album_replaced", LogField.Success(stored != null))
            stored != null
        }

    override suspend fun updateDetails(vehicleId: VehicleId, id: String, takenOn: LocalDate, comment: String?) =
        albumDao.updateDetails(vehicleId.value, id, takenOn, comment?.trim()?.ifEmpty { null }, clock.instant())

    override suspend fun delete(vehicleId: VehicleId, id: String) = withContext(ioDispatcher) {
        val photo = albumDao.find(vehicleId.value, id) ?: return@withContext
        albumDao.delete(vehicleId.value, id)
        photo.deleteFiles()
    }

    override suspend fun markDeleted(vehicleId: VehicleId, ids: List<String>) {
        if (ids.isNotEmpty()) albumDao.setDeletedAt(vehicleId.value, ids, clock.instant())
    }

    override suspend fun restoreDeleted(vehicleId: VehicleId, ids: List<String>) {
        if (ids.isNotEmpty()) albumDao.setDeletedAt(vehicleId.value, ids, deletedAt = null)
    }

    // 화면을 떠나며 부르면 부른 쪽 코루틴이 곧 취소되므로, 시작한 뒤에는 끝까지 지운다.
    override suspend fun purgeDeleted(vehicleId: VehicleId, ids: List<String>) =
        withContext(ioDispatcher + NonCancellable) {
            if (ids.isEmpty()) return@withContext
            purge(albumDao.deletedRows().filter { it.vehicleId == vehicleId.value && it.id in ids })
        }

    override suspend fun purgeStaleDeleted() = withContext(ioDispatcher + NonCancellable) {
        // 이 앱이 시작한 뒤에 새로 지운 사진은 아직 되돌릴 수 있으므로 건드리지 않는다.
        val now = clock.instant()
        purge(albumDao.deletedRows().filter { it.deletedAt?.isAfter(now) == false })
    }

    private suspend fun purge(rows: List<AlbumPhotoEntity>) {
        if (rows.isEmpty()) return
        // 행을 먼저 지운다. 파일만 남으면 다음 정리 때 찾을 수 없지만, 행만 남아 깨진 사진이 보이는 것보다 낫다.
        albumDao.deleteRows(rows.map { it.id })
        rows.forEach { it.deleteFiles() }
        logger.debug("album_purged")
    }

    /** [uri]를 사진이나 동영상으로 가져와 `[name].jpg`·`[name].mp4`와 `[name]_thumb.jpg`로 저장한다. */
    private fun store(uri: String, name: String, maxEdge: Int): StoreOutcome = if (importers.video.isVideo(uri)) {
        storeVideo(uri, name)
    } else {
        val imported = runCatching {
            importers.image.import(uri, File(directory, "$name.jpg"), File(directory, "${name}_thumb.jpg"), maxEdge)
        }.onFailure { logger.warn("album_import_failed", error = it) }.getOrNull()
        imported?.let { StoreOutcome.Stored(StoredMedia.photo(it)) } ?: StoreOutcome.Failed
    }

    private fun storeVideo(uri: String, name: String): StoreOutcome {
        val result = runCatching {
            importers.video.import(
                uri,
                File(directory, "$name.mp4"),
                File(directory, "${name}_thumb.jpg"),
                MAX_ALBUM_VIDEO_DURATION_MS,
                MAX_ALBUM_VIDEO_BYTES,
            )
        }.onFailure { logger.warn("album_video_import_failed", error = it) }.getOrDefault(VideoImport.Failed)
        return when (result) {
            is VideoImport.Imported -> StoreOutcome.Stored(StoredMedia.video(result.video))
            VideoImport.TooLarge -> StoreOutcome.TooLarge
            VideoImport.Failed -> StoreOutcome.Failed
        }
    }

    private fun AlbumPhotoEntity.deleteFiles() {
        File(directory, fileName).delete()
        File(directory, thumbnailName).delete()
    }

    private fun AlbumPhotoEntity.asExternalModel() = AlbumPhoto(
        id = id,
        filePath = File(directory, fileName).path,
        thumbnailPath = File(directory, thumbnailName).path,
        takenOn = takenOn,
        comment = comment,
        createdAt = createdAt,
        mediaType = if (mediaType == AlbumPhotoEntity.MEDIA_VIDEO) AlbumMediaType.Video else AlbumMediaType.Photo,
        durationMs = durationMs,
        width = width,
        height = height,
    )
}

private sealed interface StoreOutcome {
    data class Stored(val media: StoredMedia) : StoreOutcome

    data object TooLarge : StoreOutcome

    data object Failed : StoreOutcome
}

/** 저장을 마친 파일. 행을 새로 만들 때와 편집해서 바꿀 때 같은 칸을 채운다. */
private data class StoredMedia(
    val file: File,
    val thumbnail: File,
    val sizeBytes: Long,
    val takenOn: LocalDate?,
    val mediaType: String,
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
) {
    fun applyTo(row: AlbumPhotoEntity) = row.copy(
        fileName = file.name,
        thumbnailName = thumbnail.name,
        sizeBytes = sizeBytes,
        mediaType = mediaType,
        durationMs = durationMs,
        width = width,
        height = height,
    )

    companion object {
        fun photo(image: ImportedImage) =
            StoredMedia(image.file, image.thumbnail, image.sizeBytes, image.takenOn, AlbumPhotoEntity.MEDIA_PHOTO)

        fun video(video: ImportedVideo) = StoredMedia(
            video.file,
            video.poster,
            video.sizeBytes,
            video.takenOn,
            AlbumPhotoEntity.MEDIA_VIDEO,
            video.durationMs,
            video.width,
            video.height,
        )
    }
}
