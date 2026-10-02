package com.naury.chageun.core.domain.history

import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.VehicleId
import javax.inject.Inject

/** 첨부는 다형 소유자에 대한 foreign key가 없으므로 명시적으로 지운다. */
class DeleteHistoryRecordUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val attachmentRepository: AttachmentRepository,
) {
    suspend operator fun invoke(vehicleId: VehicleId, ref: RecordRef) {
        historyRepository.delete(vehicleId, ref)
        attachmentRepository.deleteAllFor(vehicleId, ref)
    }
}
