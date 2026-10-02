package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

enum class ServiceEntryError { FutureDate, NegativeCost }

sealed interface RecordServiceResult {
    data class Saved(val nextDistanceDue: Kilometers?, val nextDateDue: LocalDate?) : RecordServiceResult

    data class Rejected(val errors: Set<ServiceEntryError>) : RecordServiceResult

    /** 같은 항목의 이전 정비보다 낮은 값이다. 유지하려면 확인 후 다시 저장한다. */
    data class NeedsConfirmation(val previousMileage: Kilometers) : RecordServiceResult
}

class RecordServiceUseCase @Inject constructor(
    private val repository: MaintenanceRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        vehicleId: VehicleId,
        entry: ServiceEntry,
        isLowerMileageConfirmed: Boolean = false,
    ): RecordServiceResult {
        val errors = buildSet {
            if (entry.date.isAfter(LocalDate.now(clock))) add(ServiceEntryError.FutureDate)
            if ((entry.costWon ?: 0) < 0) add(ServiceEntryError.NegativeCost)
        }
        if (errors.isNotEmpty()) return RecordServiceResult.Rejected(errors)

        val previousMileage = repository.findLatestService(vehicleId, entry.item)?.mileage
        if (!isLowerMileageConfirmed && previousMileage != null && entry.mileage < previousMileage) {
            return RecordServiceResult.NeedsConfirmation(previousMileage)
        }

        val currentMileage = repository.findCurrentMileage(vehicleId)?.mileage
        val advancesOdometer = currentMileage == null || entry.mileage > currentMileage
        repository.recordService(vehicleId, entry, advancesOdometer)

        val rule = repository.findRule(vehicleId, entry.item)
        return RecordServiceResult.Saved(
            nextDistanceDue = rule?.intervalKm?.let { entry.mileage + Kilometers(it) },
            nextDateDue = rule?.intervalMonths?.let { entry.date.plusMonths(it) },
        )
    }
}
