package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.flow.Flow

/** 홈·내 차 Hero에 쓰는 대표 사진. 차량당 한 장만 둔다. */
interface VehiclePhotoRepository {
    /** 대표 사진 파일의 절대 경로. 없으면 null이다. */
    fun observe(vehicleId: VehicleId): Flow<String?>

    /**
     * [sourceUri]의 이미지를 첨부와 같은 방식(크기 축소, EXIF 제거)으로 저장하고 기존 사진을 지운다.
     * @return 가져오지 못하면 false이며 기존 사진은 그대로 남는다.
     */
    suspend fun replace(vehicleId: VehicleId, sourceUri: String): Boolean

    suspend fun clear(vehicleId: VehicleId)
}
