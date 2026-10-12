package com.naury.chageun.core.domain.history

import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.RecordPhoto
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.VehicleId
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class AttachResult(val added: Int, val failed: Int)

/** 소리 하나를 붙이거나 바꾼 결과. */
enum class SoundAttachResult {
    Added,

    /** [MAX_SOUND_DURATION_MS]보다 길다. 파일은 남기지 않는다. */
    TooLong,

    /** 기록에 이미 [MAX_SOUNDS_PER_RECORD]개가 있다. */
    LimitReached,

    /** 읽을 수 없거나 소리 파일이 아니다. */
    Failed,
}

interface AttachmentRepository {
    /** 기록에 붙인 사진과 소리. 첨부한 순서로 온다. */
    fun observe(vehicleId: VehicleId, owner: RecordRef): Flow<List<Attachment>>

    /**
     * 기록 날짜가 [from]~[to](양 끝 포함)인 기록의 사진 첨부. 기록 날짜, 첨부한 순서로 온다.
     * 날짜 없는 기록과 소리는 빠진다.
     */
    fun observeBetween(vehicleId: VehicleId, from: LocalDate, to: LocalDate): Flow<List<RecordPhoto>>

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

    /**
     * 첨부 하나의 사진을 [sourceUri]로 바꾼다. 첨부 id·소유자·순서는 그대로이고, [attach]와 같은 방식으로 다시
     * 인코딩해 썸네일도 새로 만든다. 편집한 사진이 원래보다 커지지는 않으므로 고화질 상한으로 가져온다.
     *
     * @return 가져오지 못했거나 첨부가 없으면 false이며 기존 사진은 그대로 남는다.
     */
    suspend fun replace(vehicleId: VehicleId, attachmentId: String, sourceUri: String): Boolean

    /**
     * 녹음 편집기가 만든 소리 파일([sourceUri], m4a)을 앱 저장소로 복사해 기록에 붙인다.
     * 길이는 [MAX_SOUND_DURATION_MS]까지, 기록당 [MAX_SOUNDS_PER_RECORD]개까지다.
     */
    suspend fun attachSound(vehicleId: VehicleId, owner: RecordRef, sourceUri: String): SoundAttachResult

    /** 소리 하나를 다시 편집한 파일로 바꾼다. id·이름·순서는 그대로다. 실패하면 기존 소리가 그대로 남는다. */
    suspend fun replaceSound(vehicleId: VehicleId, attachmentId: String, sourceUri: String): SoundAttachResult

    /** 소리에 "엔진 소리"처럼 이름을 붙인다. 빈 값이면 이름을 지운다. */
    suspend fun setLabel(vehicleId: VehicleId, attachmentId: String, label: String?)

    /** 사진·소리 하나와 그 파일을 지운다. */
    suspend fun delete(vehicleId: VehicleId, attachmentId: String)

    suspend fun deleteAllFor(vehicleId: VehicleId, owner: RecordRef)
}

/** 기록당 이미지를 이 개수까지만 허용해 로컬 저장 용량을 예측 가능하게 유지한다. */
const val MAX_ATTACHMENTS_PER_RECORD = 10

/**
 * 기록당 소리 수. 사진과 따로 센다. 정비소에 들려줄 증상 소리는 몇 개면 충분하고, 사진 자리를 줄이지 않게 한다.
 * 3분짜리도 2MB 남짓이라 다 채워도 저장 공간 부담이 작다.
 */
const val MAX_SOUNDS_PER_RECORD = 5

/** 소리 하나의 최대 길이. 녹음 화면도 이 길이에 닿으면 저절로 멈춘다. */
const val MAX_SOUND_DURATION_MS = 3 * 60 * 1_000L

/** 소리 이름 최대 글자 수. 목록 한 줄에 들어가는 정도다. */
const val MAX_SOUND_LABEL_LENGTH = 30
