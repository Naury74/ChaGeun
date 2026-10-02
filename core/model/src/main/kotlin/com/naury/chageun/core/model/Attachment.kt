package com.naury.chageun.core.model

import java.time.Instant

/** 앱 전용 저장소에 보관한 사진이나 영수증이다. 경로는 절대 경로이며 기기 밖으로 나가지 않는다. */
data class Attachment(
    val id: String,
    val owner: RecordRef,
    val filePath: String,
    val thumbnailPath: String,
    val createdAt: Instant,
)
