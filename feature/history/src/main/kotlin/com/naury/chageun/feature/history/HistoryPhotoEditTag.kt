package com.naury.chageun.feature.history

import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.ui.photo.EditedPhotos

/**
 * 편집기 결과가 어느 동작에서 왔는지 태그로 남긴다. 편집 도중 프로세스가 다시 만들어져도
 * 태그는 저장되므로 결과를 맞는 기록·첨부에 돌려줄 수 있다.
 */
internal object HistoryPhotoEditTag {
    private const val REPLACE = "replace:"
    private const val ADD = "add:"

    fun replace(attachmentId: String) = "$REPLACE$attachmentId"

    fun add(ref: RecordRef) = "$ADD${ref.type.name}:${ref.id}"

    /**
     * 첨부 하나를 고쳤으면 그 자리에 바꿔 넣고, 새로 골라 편집했으면 기록에 붙인다.
     * 편집해서 붙이는 사진은 영수증 글씨가 남도록 고화질 상한으로 가져온다. 잘라 냈으면 원래보다 작아진다.
     */
    fun apply(edited: EditedPhotos, viewModel: HistoryViewModel) {
        val tag = edited.tag ?: return
        when {
            tag.startsWith(REPLACE) ->
                edited.uris.firstOrNull()?.let { viewModel.replaceAttachment(tag.removePrefix(REPLACE), it) }

            tag.startsWith(ADD) ->
                tag.removePrefix(ADD).toRecordRef()?.let { viewModel.attach(it, edited.uris, highQuality = true) }
        }
    }
}
