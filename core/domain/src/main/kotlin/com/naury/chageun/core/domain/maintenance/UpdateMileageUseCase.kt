package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

sealed interface UpdateMileageResult {
    data object Saved : UpdateMileageResult

    /** Lower than the latest reading; save again with confirmation to record it as an odometer correction. */
    data class NeedsConfirmation(val previous: Kilometers) : UpdateMileageResult
}

class UpdateMileageUseCase @Inject constructor(
    private val repository: MaintenanceRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        vehicleId: VehicleId,
        mileage: Kilometers,
        isCorrectionConfirmed: Boolean = false,
    ): UpdateMileageResult {
        val previous = repository.findCurrentMileage(vehicleId)?.mileage
        val isLower = previous != null && mileage < previous
        if (previous != null &&
            isLower &&
            !isCorrectionConfirmed
        ) {
            return UpdateMileageResult.NeedsConfirmation(previous)
        }
        repository.addMileageReading(vehicleId, MileageReading(LocalDate.now(clock), mileage), isCorrection = isLower)
        return UpdateMileageResult.Saved
    }
}

/** Home asks for a fresh reading when the latest one is at least this old. */
const val MILEAGE_PROMPT_AFTER_DAYS = 30L
