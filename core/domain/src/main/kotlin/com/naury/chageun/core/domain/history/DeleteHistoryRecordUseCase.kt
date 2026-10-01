package com.naury.chageun.core.domain.history

import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.VehicleId
import javax.inject.Inject

/** Attachments have no foreign key to their polymorphic owner, so they are removed explicitly. */
class DeleteHistoryRecordUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val attachmentRepository: AttachmentRepository,
) {
    suspend operator fun invoke(vehicleId: VehicleId, ref: RecordRef) {
        historyRepository.delete(vehicleId, ref)
        attachmentRepository.deleteAllFor(vehicleId, ref)
    }
}
