package com.naury.chageun.core.domain.history

import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.flow.Flow

data class AttachResult(val added: Int, val failed: Int)

interface AttachmentRepository {
    fun observe(vehicleId: VehicleId, owner: RecordRef): Flow<List<Attachment>>

    /**
     * 선택한 이미지를 메타데이터 없이 다시 인코딩해 앱 저장소로 복사한다.
     * [sourceUris]는 photo picker나 사진 편집기가 준 URI다. [highQuality]면 긴 변을 더 크게 남긴다.
     */
    suspend fun attach(
        vehicleId: VehicleId,
        owner: RecordRef,
        sourceUris: List<String>,
        highQuality: Boolean = false,
    ): AttachResult

    suspend fun delete(vehicleId: VehicleId, attachmentId: String)

    suspend fun deleteAllFor(vehicleId: VehicleId, owner: RecordRef)
}

/** 기록당 이미지를 이 개수까지만 허용해 로컬 저장 용량을 예측 가능하게 유지한다. */
const val MAX_ATTACHMENTS_PER_RECORD = 10
