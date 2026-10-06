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
}
