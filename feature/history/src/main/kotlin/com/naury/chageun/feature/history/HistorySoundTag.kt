package com.naury.chageun.feature.history

import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.ui.sound.RecordedSound

/**
 * 녹음 편집기 결과가 어느 동작에서 왔는지 태그로 남긴다. 녹음 도중 프로세스가 다시 만들어져도
 * 태그는 저장되므로 결과를 맞는 기록·소리에 돌려줄 수 있다.
 */
internal object HistorySoundTag {
    private const val REPLACE = "sound-replace:"
    private const val ADD = "sound-add:"

    fun replace(attachmentId: String) = "$REPLACE$attachmentId"

    fun add(ref: RecordRef) = "$ADD${ref.type.name}:${ref.id}"

    /** 소리 하나를 다시 편집했으면 그 자리에 바꿔 넣고, 새로 녹음했거나 골랐으면 기록에 붙인다. */
    fun apply(recorded: RecordedSound, actions: SoundAttachmentActions) {
        val tag = recorded.tag ?: return
        when {
            tag.startsWith(REPLACE) -> actions.replace(tag.removePrefix(REPLACE), recorded.uri)
            tag.startsWith(ADD) -> tag.removePrefix(ADD).toRecordRef()?.let { actions.attach(it, recorded.uri) }
        }
    }
}
