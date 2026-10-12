package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.album.AlbumAddResult
import com.naury.chageun.core.domain.album.AlbumRepository
import com.naury.chageun.core.model.AlbumMediaType
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.VehicleId
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * "broken"이 들어간 URI는 넣지 못하고, "huge"가 들어간 URI는 너무 큰 동영상으로 센다. "video"나 ".mp4"가 들어간
 * URI는 42초짜리 동영상으로 넣는다. 넣은 항목은 [fallbackDate]로 저장한다.
 *
 * 되돌릴 수 있게 지운 항목은 [pendingDeleted]에 두고 목록에서만 뺀다. 끝내 지운 id는 [purged]에 남는다.
 */
class FakeAlbumRepository : AlbumRepository {
    val photos = MutableStateFlow<List<AlbumPhoto>>(emptyList())
    val pendingDeleted = MutableStateFlow<Set<String>>(emptySet())
    val purged = mutableListOf<String>()

    private val visible = combine(photos, pendingDeleted) { list, hidden -> list.filterNot { it.id in hidden } }

    override fun observe(vehicleId: VehicleId): Flow<List<AlbumPhoto>> = visible

    override fun observeBetween(vehicleId: VehicleId, from: LocalDate, to: LocalDate): Flow<List<AlbumPhoto>> =
        visible.map { list ->
            list.filter { it.takenOn in from..to }.sortedWith(compareBy({ it.takenOn }, { it.createdAt }))
        }

    override suspend fun add(
        vehicleId: VehicleId,
        sourceUris: List<String>,
        fallbackDate: LocalDate,
        highQuality: Boolean,
    ): AlbumAddResult {
        val tooLarge = sourceUris.count { "huge" in it }
        val added = sourceUris.filterNot { "broken" in it || "huge" in it }.mapIndexed { index, uri ->
            AlbumPhoto("p${photos.value.size + index}", uri, uri, fallbackDate, null, Instant.EPOCH).withMedia(uri)
        }
        photos.update { added + it }
        return AlbumAddResult(added.map { it.id }, sourceUris.size - added.size - tooLarge, tooLarge)
    }

    /** 바꾼 횟수. 편집해서 바꿔 넣을 때 id가 그대로인지 확인하는 데 쓴다. */
    val replaced = mutableListOf<Pair<String, String>>()

    override suspend fun replace(vehicleId: VehicleId, id: String, sourceUri: String): Boolean {
        if ("broken" in sourceUri || photos.value.none { it.id == id }) return false
        replaced += id to sourceUri
        photos.update { list ->
            list.map {
                if (it.id ==
                    id
                ) {
                    it.copy(filePath = sourceUri, thumbnailPath = sourceUri).withMedia(sourceUri)
                } else {
                    it
                }
            }
        }
        return true
    }

    private fun AlbumPhoto.withMedia(uri: String): AlbumPhoto = if ("video" in uri || uri.endsWith(".mp4")) {
        copy(mediaType = AlbumMediaType.Video, durationMs = FAKE_VIDEO_MS)
    } else {
        copy(mediaType = AlbumMediaType.Photo, durationMs = null)
    }

    override suspend fun updateDetails(vehicleId: VehicleId, id: String, takenOn: LocalDate, comment: String?) {
        photos.update { list -> list.map { if (it.id == id) it.copy(takenOn = takenOn, comment = comment) else it } }
    }

    override suspend fun delete(vehicleId: VehicleId, id: String) {
        photos.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun markDeleted(vehicleId: VehicleId, ids: List<String>) {
        pendingDeleted.update { it + ids }
    }

    override suspend fun restoreDeleted(vehicleId: VehicleId, ids: List<String>) {
        pendingDeleted.update { it - ids.toSet() }
    }

    override suspend fun purgeDeleted(vehicleId: VehicleId, ids: List<String>) =
        purge(ids.filter { it in pendingDeleted.value })

    override suspend fun purgeStaleDeleted() = purge(pendingDeleted.value.toList())

    private fun purge(ids: List<String>) {
        purged += ids
        photos.update { list -> list.filterNot { it.id in ids } }
        pendingDeleted.update { it - ids.toSet() }
    }
}

private const val FAKE_VIDEO_MS = 42_000L
