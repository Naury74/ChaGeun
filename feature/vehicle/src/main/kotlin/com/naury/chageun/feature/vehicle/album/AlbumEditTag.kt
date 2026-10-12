package com.naury.chageun.feature.vehicle.album

import com.naury.chageun.core.ui.photo.EditedPhotos

/**
 * 편집기 결과가 어느 동작에서 왔는지 태그로 남긴다. 편집 도중 프로세스가 다시 만들어져도 태그는 저장되므로
 * 결과를 맞는 앨범 항목에 돌려줄 수 있다.
 */
internal object AlbumEditTag {
    /** 새로 골라 편집한 사진·동영상을 앨범에 넣는다. */
    const val ADD = "album-add"
    private const val REPLACE = "album-replace:"

    /** 앨범 항목 [id]를 편집해 그 자리에 바꿔 넣는다. */
    fun replace(id: String) = "$REPLACE$id"

    fun apply(edited: EditedPhotos, viewModel: AlbumViewModel) {
        val tag = edited.tag ?: return
        when {
            tag == ADD -> viewModel.add(edited.uris, highQuality = false)
            tag.startsWith(REPLACE) ->
                edited.uris.firstOrNull()?.let { viewModel.replaceMedia(tag.removePrefix(REPLACE), it) }
        }
    }
}
