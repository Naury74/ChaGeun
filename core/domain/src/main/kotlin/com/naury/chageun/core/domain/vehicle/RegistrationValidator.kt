package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.model.VehicleRegistration
import java.time.Clock
import java.time.Year
import javax.inject.Inject

enum class RegistrationError { MakerMissing, ModelMissing, ModelYearOutOfRange }

class RegistrationValidator @Inject constructor(private val clock: Clock) {

    fun validate(registration: VehicleRegistration): Set<RegistrationError> =
        validate(registration.maker, registration.model, registration.modelYear)

    fun validate(maker: String, model: String, modelYear: Int): Set<RegistrationError> = buildSet {
        if (maker.isBlank()) add(RegistrationError.MakerMissing)
        if (model.isBlank()) add(RegistrationError.ModelMissing)
        // 다음 연식 모델은 가을부터 판매되므로 1년 뒤 연식까지 허용한다.
        val latestYear = Year.now(clock).value + 1
        if (modelYear !in OLDEST_MODEL_YEAR..latestYear) add(RegistrationError.ModelYearOutOfRange)
    }

    private companion object {
        const val OLDEST_MODEL_YEAR = 1980
    }
}
