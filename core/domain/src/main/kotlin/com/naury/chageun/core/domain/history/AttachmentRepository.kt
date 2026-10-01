package com.naury.chageun.core.domain.history

import com.naury.chageun.core.model.Attachment
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.flow.Flow

data class AttachResult(val added: Int, val failed: Int)

interface AttachmentRepository {
    fun observe(vehicleId: VehicleId, owner: RecordRef): Flow<List<Attachment>>

    /**
     * Copies the picked images into app storage, re-encoded without metadata.
     * [sourceUris] are content URIs from the system photo picker.
     */
    suspend fun attach(vehicleId: VehicleId, owner: RecordRef, sourceUris: List<String>): AttachResult

    suspend fun delete(vehicleId: VehicleId, attachmentId: String)

    suspend fun deleteAllFor(vehicleId: VehicleId, owner: RecordRef)
}

/** At most this many images per record keeps local storage predictable. */
const val MAX_ATTACHMENTS_PER_RECORD = 10
