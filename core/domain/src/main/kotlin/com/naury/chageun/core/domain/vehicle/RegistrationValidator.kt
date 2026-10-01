package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.model.VehicleRegistration
import java.time.Clock
import java.time.Year
import javax.inject.Inject

enum class RegistrationError { MakerMissing, ModelMissing, ModelYearOutOfRange }

class RegistrationValidator @Inject constructor(private val clock: Clock) {

    fun validate(registration: VehicleRegistration): Set<RegistrationError> = buildSet {
        if (registration.maker.isBlank()) add(RegistrationError.MakerMissing)
        if (registration.model.isBlank()) add(RegistrationError.ModelMissing)
        // Next year's models are sold from autumn, so allow one year ahead.
        val latestYear = Year.now(clock).value + 1
        if (registration.modelYear !in OLDEST_MODEL_YEAR..latestYear) add(RegistrationError.ModelYearOutOfRange)
    }

    private companion object {
        const val OLDEST_MODEL_YEAR = 1980
    }
}
