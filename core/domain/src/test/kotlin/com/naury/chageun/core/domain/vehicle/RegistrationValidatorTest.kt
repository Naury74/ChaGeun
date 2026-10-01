package com.naury.chageun.core.domain.vehicle

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.VehicleRegistration
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test

class RegistrationValidatorTest {

    private val validator = RegistrationValidator(Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC))

    private fun registration(maker: String = "Maker", model: String = "Model", year: Int = 2023) =
        VehicleRegistration(maker, model, year, FuelType.Gasoline, Kilometers(10_000))

    @Test
    fun acceptsCompleteRegistration() {
        assertThat(validator.validate(registration())).isEmpty()
    }

    @Test
    fun reportsBlankMakerAndModel() {
        assertThat(validator.validate(registration(maker = " ", model = "")))
            .containsExactly(RegistrationError.MakerMissing, RegistrationError.ModelMissing)
    }

    @Test
    fun allowsNextModelYear_butNotBeyond() {
        assertThat(validator.validate(registration(year = 2027))).isEmpty()
        assertThat(validator.validate(registration(year = 2028))).containsExactly(RegistrationError.ModelYearOutOfRange)
        assertThat(validator.validate(registration(year = 1979))).containsExactly(RegistrationError.ModelYearOutOfRange)
    }
}
