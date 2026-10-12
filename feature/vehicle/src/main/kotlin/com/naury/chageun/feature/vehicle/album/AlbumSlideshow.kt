package com.naury.chageun.feature.vehicle.album

import com.naury.chageun.core.model.AlbumPhoto
import java.time.LocalDate

/** 슬라이드쇼는 사진 두 장 이상으로 만든다. */
internal const val MIN_SLIDESHOW_PHOTOS = 2

/**
 * 여러 장 고르기에서 슬라이드쇼로 만들 사진. 고른 순서를 지키고 동영상은 뺀다.
 * 고른 순서대로 넘어가야 사용자가 생각한 이야기 순서가 된다.
 */
internal fun AlbumUiState.slideshowPhotos(): List<AlbumPhoto> {
    val byId = photos.associateBy { it.id }
    return picked.mapNotNull { byId[it] }.filterNot { it.isVideo }
}

/** 고른 것 중 동영상이 있어 슬라이드쇼에서 빠지는지. */
internal val AlbumUiState.hasPickedVideos: Boolean
    get() = photos.any { it.id in picked && it.isVideo }

/** 슬라이드쇼를 앨범의 어느 날에 둘지. 고른 사진 중 가장 늦게 찍은 날이라 그 이야기의 마지막 날에 놓인다. */
internal fun slideshowDate(photos: List<AlbumPhoto>, today: LocalDate): LocalDate =
    photos.maxOfOrNull { it.takenOn } ?: today
