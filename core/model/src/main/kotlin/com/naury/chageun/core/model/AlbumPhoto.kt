package com.naury.chageun.core.model

import java.time.Instant
import java.time.LocalDate

/** 내 차 앨범 사진. [takenOn]은 찍은 날짜이고, 모르면 추가한 날이다. */
data class AlbumPhoto(
    val id: String,
    val filePath: String,
    val thumbnailPath: String,
    val takenOn: LocalDate,
    val comment: String?,
    val createdAt: Instant,
)
