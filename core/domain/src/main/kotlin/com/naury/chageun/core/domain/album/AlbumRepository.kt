package com.naury.chageun.core.domain.album

import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.VehicleId
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** @property addedIds 새로 넣은 사진 ID. 한 장이면 바로 코멘트를 입력받는 데 쓴다. */
data class AlbumAddResult(val addedIds: List<String>, val failed: Int)

interface AlbumRepository {
    /** 찍은 날짜 최신순. */
    fun observe(vehicleId: VehicleId): Flow<List<AlbumPhoto>>

    /**
     * 사진을 위치·카메라 정보 없이 다시 인코딩해 저장한다. 찍은 날짜는 원본 EXIF에서 읽고, 없으면 [fallbackDate]를 쓴다.
     */
    suspend fun add(
        vehicleId: VehicleId,
        sourceUris: List<String>,
        fallbackDate: LocalDate,
        highQuality: Boolean = false,
    ): AlbumAddResult

    suspend fun updateDetails(vehicleId: VehicleId, id: String, takenOn: LocalDate, comment: String?)

    suspend fun delete(vehicleId: VehicleId, id: String)
}

/** 앨범 한 곳에 너무 많은 사진이 쌓여 저장 공간을 예측할 수 없게 되지 않도록 한 번에 넣는 수를 제한한다. */
const val MAX_ALBUM_PHOTOS_PER_ADD = 20
