package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.history.AttachResult
import com.naury.chageun.core.domain.history.AttachmentRepository
import com.naury.chageun.core.model.Attachment
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

    override suspend fun delete(vehicleId: VehicleId, attachmentId: String) {
        attachments.update { list -> list.filterNot { it.id == attachmentId } }
    }

    override suspend fun deleteAllFor(vehicleId: VehicleId, owner: RecordRef) {
        attachments.update { list -> list.filterNot { it.owner == owner } }
    }
}
