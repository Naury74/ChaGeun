package com.naury.chageun.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * 앱 전용 저장소에 보관한 사진·영수증이나 녹음한 소리다. 경로는 절대 경로이며 기기 밖으로 나가지 않는다.
 *
 * 소리([AttachmentKind.Sound])는 썸네일이 없어 [thumbnailPath]가 빈 값이고, [durationMs]에 길이를,
 * [label]에 "엔진 소리"처럼 사용자가 붙인 이름을 둔다. 사진은 둘 다 null이다.
 */
data class Attachment(
    val id: String,
    val owner: RecordRef,
    val filePath: String,
    val thumbnailPath: String,
    val createdAt: Instant,
    val kind: AttachmentKind = AttachmentKind.Photo,
    val durationMs: Long? = null,
    val label: String? = null,
) {
    val isSound: Boolean get() = kind == AttachmentKind.Sound
}

enum class AttachmentKind { Photo, Sound }

/** 기록에 붙인 사진(영수증 등)과 그 기록의 날짜. 달력에서는 첨부한 날이 아니라 기록 날짜에 놓는다. */
data class RecordPhoto(val attachment: Attachment, val date: LocalDate)
