package com.naury.chageun.core.domain.history

import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** 저장된 기록을 고친다. 검증 규칙은 추가할 때와 같다. */
class EditHistoryRecordUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val clock: Clock,
) {
    /** 고칠 기록을 폼에 채우기 위해 읽는다. 지워졌으면 null이다. */
    suspend fun load(vehicleId: VehicleId, ref: RecordRef): RecordDetail? =
        historyRepository.observeRecord(vehicleId, ref).first()

    suspend fun updateFuel(vehicleId: VehicleId, id: String, entry: FuelEntry): Set<HistoryEntryError> {
        val errors = dateErrors(entry.date)
        if (errors.isEmpty()) historyRepository.updateFuel(vehicleId, id, entry)
        return errors
    }

    suspend fun updateCheck(vehicleId: VehicleId, id: String, entry: CheckEntry): Set<HistoryEntryError> {
        val errors = dateErrors(entry.date) + costErrors(entry.costWon) + buildSet {
            if (entry.title.isBlank()) add(HistoryEntryError.MissingTitle)
        }
        if (errors.isEmpty()) historyRepository.updateCheck(vehicleId, id, entry.copy(title = entry.title.trim()))
        return errors
    }

    suspend fun updateService(vehicleId: VehicleId, id: String, entry: ServiceEntry): Set<HistoryEntryError> {
        val errors = dateErrors(entry.date) + costErrors(entry.costWon)
        if (errors.isEmpty()) historyRepository.updateMaintenance(vehicleId, id, entry)
        return errors
    }

    private fun dateErrors(date: LocalDate) =
        if (date.isAfter(LocalDate.now(clock))) setOf(HistoryEntryError.FutureDate) else emptySet()

    private fun costErrors(costWon: Long?) =
        if ((costWon ?: 0) < 0) setOf(HistoryEntryError.NegativeCost) else emptySet()
}
