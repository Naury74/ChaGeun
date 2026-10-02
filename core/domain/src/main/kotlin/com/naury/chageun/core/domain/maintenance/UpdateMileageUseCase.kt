package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

sealed interface UpdateMileageResult {
    data object Saved : UpdateMileageResult

    /** 최근 기록보다 낮은 값이다. 확인 후 다시 저장하면 주행거리 보정으로 기록한다. */
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

/** 최근 기록이 이 기간 이상 지났으면 Home에서 새 주행거리 입력을 요청한다. */
const val MILEAGE_PROMPT_AFTER_DAYS = 30L
