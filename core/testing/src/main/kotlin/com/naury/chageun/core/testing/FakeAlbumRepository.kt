package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.album.AlbumAddResult
import com.naury.chageun.core.domain.album.AlbumRepository
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.VehicleId
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** "broken"이 들어간 URI는 넣지 못한다. 넣은 사진은 [fallbackDate]로 저장한다. */
class FakeAlbumRepository : AlbumRepository {
    val photos = MutableStateFlow<List<AlbumPhoto>>(emptyList())

    override fun observe(vehicleId: VehicleId): Flow<List<AlbumPhoto>> = photos

    override suspend fun add(
        vehicleId: VehicleId,
        sourceUris: List<String>,
        fallbackDate: LocalDate,
        highQuality: Boolean,
    ): AlbumAddResult {
        val added = sourceUris.filterNot { "broken" in it }.mapIndexed { index, uri ->
            AlbumPhoto("p${photos.value.size + index}", uri, uri, fallbackDate, null, Instant.EPOCH)
        }
        photos.update { added + it }
        return AlbumAddResult(added.map { it.id }, sourceUris.size - added.size)
    }

    override suspend fun updateDetails(vehicleId: VehicleId, id: String, takenOn: LocalDate, comment: String?) {
        photos.update { list -> list.map { if (it.id == id) it.copy(takenOn = takenOn, comment = comment) else it } }
    }

    override suspend fun delete(vehicleId: VehicleId, id: String) {
        photos.update { list -> list.filterNot { it.id == id } }
    }
}
