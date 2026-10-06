package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.model.CutoutStatus
import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.flow.Flow

/** 홈·내 차 Hero에 쓰는 대표 사진. 차량당 한 장만 둔다. */
interface VehiclePhotoRepository {
    /**
     * 대표 사진 파일의 절대 경로. 배경 지우기를 켜 두었고 지운 사진이 있으면 그 PNG 경로, 아니면 원본 JPEG다.
     * 사진이 없으면 null이다.
     */
    fun observe(vehicleId: VehicleId): Flow<String?>

    /** 배경 지우기 진행 상태. 앱을 다시 켜면 [CutoutStatus.Idle]부터 시작한다. */
    fun observeCutout(vehicleId: VehicleId): Flow<CutoutStatus>

    /**
     * [sourceUri]의 이미지를 첨부와 같은 방식(크기 축소, EXIF 제거)으로 저장하고 기존 사진을 지운다.
     * 저장하면 바로 원본을 보여 주고, 배경 지우기는 뒤에서 이어서 진행한다.
     * @return 가져오지 못하면 false이며 기존 사진은 그대로 남는다.
     */
    suspend fun replace(vehicleId: VehicleId, sourceUri: String, removeBackground: Boolean = true): Boolean

    /**
     * [replace]를 화면 수명과 상관없이 이어서 한다. 온보딩처럼 저장하자마자 화면이 바뀌어
     * 호출한 쪽의 코루틴이 끊길 수 있을 때 쓴다. 실패하면 사진 없이 남고 나중에 다시 넣을 수 있다.
     */
    fun replaceInBackground(vehicleId: VehicleId, sourceUri: String, removeBackground: Boolean = true)

    fun observeBackgroundRemoval(vehicleId: VehicleId): Flow<Boolean>

    /** 배경을 지운 모습으로 볼지. 켤 때 아직 지운 사진이 없으면 지우기를 시작한다. 지운 파일은 꺼도 남겨 둔다. */
    suspend fun setBackgroundRemoval(vehicleId: VehicleId, enabled: Boolean)

    /** 배경을 지우지 못한 대표 사진에 다시 시도한다. 이미 진행 중이면 처음부터 다시 한다. */
    fun removeBackground(vehicleId: VehicleId)

    suspend fun clear(vehicleId: VehicleId)
}
