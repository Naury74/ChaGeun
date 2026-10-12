package com.naury.chageun.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * 내 차 앨범 항목. 사진이거나 동영상이다. [takenOn]은 찍은 날짜이고, 모르면 추가한 날이다.
 *
 * @property thumbnailPath 그리드에 쓰는 작은 이미지. 동영상이면 앞부분에서 뽑은 장면(포스터)이다.
 * @property durationMs 동영상 길이. 사진이면 null.
 * @property width 화면에 보이는 방향 기준 가로 픽셀. 예전에 넣은 사진처럼 모르면 null.
 * @property height 화면에 보이는 방향 기준 세로 픽셀. 모르면 null.
 */
data class AlbumPhoto(
    val id: String,
    val filePath: String,
    val thumbnailPath: String,
    val takenOn: LocalDate,
    val comment: String?,
    val createdAt: Instant,
    val mediaType: AlbumMediaType = AlbumMediaType.Photo,
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
) {
    val isVideo: Boolean get() = mediaType == AlbumMediaType.Video
}

enum class AlbumMediaType { Photo, Video }
