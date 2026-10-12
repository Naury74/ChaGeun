package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.history.AttachResult
import com.naury.chageun.core.domain.history.AttachmentRepository
import com.naury.chageun.core.domain.history.MAX_SOUNDS_PER_RECORD
import com.naury.chageun.core.domain.history.SoundAttachResult
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.AttachmentKind
import com.naury.chageun.core.model.RecordPhoto
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.VehicleId
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeAttachmentRepository : AttachmentRepository {
    val attachments = MutableStateFlow<List<Attachment>>(emptyList())

    override fun observe(vehicleId: VehicleId, owner: RecordRef): Flow<List<Attachment>> =
        attachments.map { list -> list.filter { it.owner == owner } }

    /** 첨부가 달린 기록의 날짜. [observeBetween]은 날짜가 있는 기록의 첨부만 돌려준다. */
    val recordDates = MutableStateFlow<Map<RecordRef, LocalDate>>(emptyMap())

    override fun observeBetween(vehicleId: VehicleId, from: LocalDate, to: LocalDate): Flow<List<RecordPhoto>> =
        combine(attachments, recordDates) { list, dates ->
            list.mapNotNull { attachment -> dates[attachment.owner]?.let { RecordPhoto(attachment, it) } }
                .filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
                .sortedBy { it.date }
        }

    var lastHighQuality: Boolean? = null

    override suspend fun attach(
        vehicleId: VehicleId,
        owner: RecordRef,
        sourceUris: List<String>,
        highQuality: Boolean,
    ): AttachResult {
        lastHighQuality = highQuality
        attachments.update { current ->
            current + sourceUris.mapIndexed { index, uri ->
                Attachment("${owner.id}-${current.size + index}", owner, uri, uri, Instant.EPOCH)
            }
        }
        return AttachResult(added = sourceUris.size, failed = 0)
    }

    var replaceSucceeds = true

    override suspend fun replace(vehicleId: VehicleId, attachmentId: String, sourceUri: String): Boolean {
        if (!replaceSucceeds || attachments.value.none { it.id == attachmentId }) return false
        attachments.update { list ->
            list.map { if (it.id == attachmentId) it.copy(filePath = sourceUri, thumbnailPath = sourceUri) else it }
        }
        return true
    }

    /** 다음 소리 붙이기·바꾸기가 돌려줄 결과. Added면 실제로 목록에 더한다. */
    var nextSoundResult = SoundAttachResult.Added

    /** 붙인 소리의 길이. */
    var soundDurationMs = 12_000L

    override suspend fun attachSound(vehicleId: VehicleId, owner: RecordRef, sourceUri: String): SoundAttachResult {
        val count = attachments.value.count { it.owner == owner && it.isSound }
        if (count >= MAX_SOUNDS_PER_RECORD) return SoundAttachResult.LimitReached
        if (nextSoundResult == SoundAttachResult.Added) {
            attachments.update { current ->
                current + Attachment(
                    id = "${owner.id}-sound-${current.size}",
                    owner = owner,
                    filePath = sourceUri,
                    thumbnailPath = "",
                    createdAt = Instant.EPOCH,
                    kind = AttachmentKind.Sound,
                    durationMs = soundDurationMs,
                )
            }
        }
        return nextSoundResult
    }

    override suspend fun replaceSound(
        vehicleId: VehicleId,
        attachmentId: String,
        sourceUri: String,
    ): SoundAttachResult {
        if (attachments.value.none { it.id == attachmentId && it.isSound }) return SoundAttachResult.Failed
        if (nextSoundResult == SoundAttachResult.Added) {
            attachments.update { list ->
                list.map {
                    if (it.id ==
                        attachmentId
                    ) {
                        it.copy(filePath = sourceUri, durationMs = soundDurationMs)
                    } else {
                        it
                    }
                }
            }
        }
        return nextSoundResult
    }

    override suspend fun setLabel(vehicleId: VehicleId, attachmentId: String, label: String?) {
        attachments.update { list ->
            list.map { if (it.id == attachmentId) it.copy(label = label?.trim()?.ifEmpty { null }) else it }
        }
    }

    override suspend fun delete(vehicleId: VehicleId, attachmentId: String) {
        attachments.update { list -> list.filterNot { it.id == attachmentId } }
    }

    override suspend fun deleteAllFor(vehicleId: VehicleId, owner: RecordRef) {
        attachments.update { list -> list.filterNot { it.owner == owner } }
    }
}
