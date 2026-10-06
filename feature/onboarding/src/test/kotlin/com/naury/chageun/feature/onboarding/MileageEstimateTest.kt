package com.naury.chageun.feature.onboarding

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class MileageEstimateTest {

    @Test
    fun estimatesFromYearsSinceModelYear_roundedToThousand() {
        assertThat(MileageEstimate.fromModelYear(2023, LocalDate.of(2026, 10, 1))).isEqualTo(45_000)
    }

    @Test
    fun noEstimate_forUnknownOrCurrentYearAtStart() {
        assertThat(MileageEstimate.fromModelYear(null, LocalDate.of(2026, 10, 1))).isNull()
        assertThat(MileageEstimate.fromModelYear(2027, LocalDate.of(2026, 10, 1))).isNull()
        assertThat(MileageEstimate.fromModelYear(2026, LocalDate.of(2026, 1, 1))).isNull()
    }

    @Test
    fun mileageAt_usesOwnPace() {
        // 2024-01-01부터 731일 동안 73,100 km → 하루 100 km
        val mileage = MileageEstimate.mileageAt(
            serviceDate = LocalDate.of(2025, 12, 2),
            currentMileage = 73_100,
            modelYear = 2024,
            today = LocalDate.of(2026, 1, 1),
        )

        assertThat(mileage).isEqualTo(70_100)
    }

    @Test
    fun mileageAt_fallsBackToNationalAverage_forNewCar_andNeverNegative() {
        val today = LocalDate.of(2026, 3, 1)

        assertThat(MileageEstimate.mileageAt(today.minusDays(30), 5_000, 2026, today)).isEqualTo(4_014)
        assertThat(MileageEstimate.mileageAt(today.minusDays(365), 3_000, 2026, today)).isEqualTo(0)
    }
}
