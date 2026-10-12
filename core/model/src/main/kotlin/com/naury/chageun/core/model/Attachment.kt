package com.naury.chageun.core.model

import java.time.Instant
import java.time.LocalDate

/** 앱 전용 저장소에 보관한 사진이나 영수증이다. 경로는 절대 경로이며 기기 밖으로 나가지 않는다. */
data class Attachment(
    val id: String,
    val owner: RecordRef,
    val filePath: String,
    val thumbnailPath: String,
    val createdAt: Instant,
)

/** 기록에 붙인 사진(영수증 등)과 그 기록의 날짜. 달력에서는 첨부한 날이 아니라 기록 날짜에 놓는다. */
data class RecordPhoto(val attachment: Attachment, val date: LocalDate)
