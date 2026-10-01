package com.naury.chageun.core.domain.history

import com.naury.chageun.core.domain.maintenance.MaintenanceRepository
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

enum class HistoryEntryError { FutureDate, MissingTitle, NegativeCost }

class AddHistoryRecordUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val clock: Clock,
) {
    suspend fun addFuel(vehicleId: VehicleId, entry: FuelEntry): Set<HistoryEntryError> {
        val errors = dateErrors(entry.date)
        if (errors.isEmpty()) historyRepository.addFuel(vehicleId, entry, advancesOdometer(vehicleId, entry.mileage))
        return errors
    }

    suspend fun addCheck(vehicleId: VehicleId, entry: CheckEntry): Set<HistoryEntryError> {
        val errors = dateErrors(entry.date) + buildSet {
            if (entry.title.isBlank()) add(HistoryEntryError.MissingTitle)
            if ((entry.costWon ?: 0) < 0) add(HistoryEntryError.NegativeCost)
        }
        if (errors.isEmpty()) {
            val advances = entry.mileage?.let { advancesOdometer(vehicleId, it) } ?: false
            historyRepository.addCheck(vehicleId, entry.copy(title = entry.title.trim()), advances)
        }
        return errors
    }

    private fun dateErrors(date: LocalDate) =
        if (date.isAfter(LocalDate.now(clock))) setOf(HistoryEntryError.FutureDate) else emptySet()

    private suspend fun advancesOdometer(vehicleId: VehicleId, mileage: Kilometers): Boolean {
        val current = maintenanceRepository.findCurrentMileage(vehicleId)?.mileage
        return current == null || mileage > current
    }
}
