package com.naury.chageun.core.domain.album

import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.VehicleId
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * @property addedIds 새로 넣은 사진·동영상 ID. 한 장이면 바로 코멘트를 입력받는 데 쓴다.
 * @property failed 읽지 못해 넣지 못한 수. [tooLarge]는 따로 센다.
 * @property tooLarge [MAX_ALBUM_VIDEO_DURATION_MS]보다 길거나 [MAX_ALBUM_VIDEO_BYTES]보다 커서 넣지 않은 동영상 수.
 */
data class AlbumAddResult(val addedIds: List<String>, val failed: Int, val tooLarge: Int = 0)

interface AlbumRepository {
    /** 찍은 날짜 최신순. */
    fun observe(vehicleId: VehicleId): Flow<List<AlbumPhoto>>

    /** 찍은 날짜가 [from]~[to](양 끝 포함)인 사진. 날짜순이고, 같은 날은 먼저 넣은 사진이 앞에 온다. */
    fun observeBetween(vehicleId: VehicleId, from: LocalDate, to: LocalDate): Flow<List<AlbumPhoto>>

    /**
     * 사진은 위치·카메라 정보 없이 다시 인코딩해 저장한다. 동영상은 다시 인코딩하지 않고 영상·소리 트랙만 새 파일로
     * 옮겨 위치 정보 같은 메타데이터를 남기지 않는다. 찍은 날짜는 원본에서 읽고, 없으면 [fallbackDate]를 쓴다.
     */
    suspend fun add(
        vehicleId: VehicleId,
        sourceUris: List<String>,
        fallbackDate: LocalDate,
        highQuality: Boolean = false,
    ): AlbumAddResult

    /**
     * 앨범 항목 하나의 파일을 [sourceUri]로 바꾼다. 편집한 사진이나 동영상을 그 자리에 넣을 때 쓴다.
     * id·찍은 날짜·메모·순서는 그대로이고, [add]와 같은 방식으로 가져와 썸네일도 새로 만든다.
     *
     * @return 가져오지 못했거나 항목이 없으면 false이며 기존 파일은 그대로 남는다.
     */
    suspend fun replace(vehicleId: VehicleId, id: String, sourceUri: String): Boolean

    suspend fun updateDetails(vehicleId: VehicleId, id: String, takenOn: LocalDate, comment: String?)

    suspend fun delete(vehicleId: VehicleId, id: String)

    /**
     * 되돌릴 수 있게 지운다. [observe]·[observeBetween]과 백업에서는 바로 빠지지만 파일은 남아 있어
     * [restoreDeleted]로 되살릴 수 있다. 끝내 지우려면 [purgeDeleted]를 부른다.
     */
    suspend fun markDeleted(vehicleId: VehicleId, ids: List<String>)

    suspend fun restoreDeleted(vehicleId: VehicleId, ids: List<String>)

    /** [markDeleted]로 뺀 항목의 행과 파일을 지운다. 그사이 되살린 항목은 그대로 둔다. */
    suspend fun purgeDeleted(vehicleId: VehicleId, ids: List<String>)

    /**
     * 지금까지 [markDeleted]로 빼 두고 마무리하지 못한 항목을 모두 지운다. 되돌리기를 기다리던 중에 앱이 끝났을 때를
     * 위해 앱을 시작할 때 부른다.
     */
    suspend fun purgeStaleDeleted()
}

/** 앨범 한 곳에 너무 많은 사진이 쌓여 저장 공간을 예측할 수 없게 되지 않도록 한 번에 넣는 수를 제한한다. */
const val MAX_ALBUM_PHOTOS_PER_ADD = 20

/** 앨범에 넣을 수 있는 동영상 길이. 차의 한 장면을 남기기에 넉넉하고 저장 공간은 크게 쓰지 않는다. */
const val MAX_ALBUM_VIDEO_DURATION_MS = 3 * 60 * 1_000L

/** 앨범에 넣을 수 있는 동영상 크기. 3분 안팎의 4K 영상도 대부분 들어간다. */
const val MAX_ALBUM_VIDEO_BYTES = 500L * 1_024 * 1_024
