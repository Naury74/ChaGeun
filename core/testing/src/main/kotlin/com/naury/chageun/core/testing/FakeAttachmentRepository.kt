package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.history.AttachResult
import com.naury.chageun.core.domain.history.AttachmentRepository
import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.VehicleId
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeAttachmentRepository : AttachmentRepository {
    val attachments = MutableStateFlow<List<Attachment>>(emptyList())

    override fun observe(vehicleId: VehicleId, owner: RecordRef): Flow<List<Attachment>> =
        attachments.map { list -> list.filter { it.owner == owner } }

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
