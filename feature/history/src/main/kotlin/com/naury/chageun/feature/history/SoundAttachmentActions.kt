package com.naury.chageun.feature.history

import com.naury.chageun.core.domain.history.AttachmentRepository
import com.naury.chageun.core.domain.history.SoundAttachResult
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 소리를 붙이지 못한 까닭. 상세의 소리 칸에 짧게 알린다. */
enum class SoundNotice { TooLong, LimitReached, Failed }

/**
 * 기록에 붙인 소리를 더하고, 다시 편집한 것으로 바꾸고, 이름을 붙이는 동작. 사진 첨부와 상한·안내가 따로라 묶어 둔다.
 * 붙이지 못하면 [notice]로 까닭을 알리고, 다음에 성공하거나 닫으면 지운다.
 */
class SoundAttachmentActions internal constructor(
    private val scope: CoroutineScope,
    private val repository: AttachmentRepository,
    private val vehicleId: suspend () -> VehicleId,
) {
    private val mutableNotice = MutableStateFlow<SoundNotice?>(null)
    val notice: StateFlow<SoundNotice?> = mutableNotice.asStateFlow()

    /** [sourceUri]는 녹음 편집기 결과를 캐시에 옮긴 m4a다. */
    fun attach(ref: RecordRef, sourceUri: String) {
        scope.launch { report(repository.attachSound(vehicleId(), ref, sourceUri)) }
    }

    fun replace(attachmentId: String, sourceUri: String) {
        scope.launch { report(repository.replaceSound(vehicleId(), attachmentId, sourceUri)) }
    }

    fun rename(attachmentId: String, label: String) {
        scope.launch { repository.setLabel(vehicleId(), attachmentId, label) }
    }

    fun delete(attachmentId: String) {
        scope.launch { repository.delete(vehicleId(), attachmentId) }
    }

    fun dismissNotice() {
        mutableNotice.value = null
    }

    private fun report(result: SoundAttachResult) {
        mutableNotice.value = when (result) {
            SoundAttachResult.Added -> null
            SoundAttachResult.TooLong -> SoundNotice.TooLong
            SoundAttachResult.LimitReached -> SoundNotice.LimitReached
            SoundAttachResult.Failed -> SoundNotice.Failed
        }
    }
}
