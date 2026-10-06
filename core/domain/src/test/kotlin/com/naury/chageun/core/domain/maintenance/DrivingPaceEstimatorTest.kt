package com.naury.chageun.core.domain.maintenance

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.Confidence
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import java.time.LocalDate
import org.junit.Test

class DrivingPaceEstimatorTest {

    private val estimator = DrivingPaceEstimator()
    private val today = LocalDate.of(2026, 10, 1)

    private fun reading(daysAgo: Long, km: Long) = MileageReading(today.minusDays(daysAgo), Kilometers(km))

    @Test
    fun returnsNull_withSingleReading() {
        assertThat(estimator.estimate(listOf(reading(0, 10_000)), today)).isNull()
    }

    @Test
    fun keepsHighestValue_forSameDayDuplicates() {
        val pace = estimator.estimate(listOf(reading(10, 1_000), reading(0, 1_200), reading(0, 1_300)), today)

        assertThat(pace?.kmPerDay).isEqualTo(30.0)
    }

    @Test
    fun excludesNegativeSegments_andLowersConfidence() {
        val history = listOf(reading(120, 10_000), reading(60, 9_000), reading(0, 12_000))

        val pace = estimator.estimate(history, today)

        assertThat(pace?.kmPerDay).isEqualTo(50.0)
        assertThat(pace?.confidence).isEqualTo(Confidence.Low)
    }

    @Test
    fun excludesImplausibleDailyDistance() {
        val history = listOf(reading(100, 10_000), reading(50, 12_000), reading(49, 20_000))

        val pace = estimator.estimate(history, today)

        assertThat(pace?.kmPerDay).isEqualTo(40.0)
        assertThat(pace?.confidence).isEqualTo(Confidence.Low)
    }

    @Test
    fun ignoresReadingsOutsideWindow() {
        val history = listOf(reading(400, 0), reading(90, 10_000), reading(0, 13_600))

        assertThat(estimator.estimate(history, today)?.kmPerDay).isEqualTo(40.0)
    }

    @Test
    fun assignsMediumConfidence_forShortObservation() {
        val pace = estimator.estimate(listOf(reading(40, 10_000), reading(0, 11_200)), today)

        assertThat(pace?.confidence).isEqualTo(Confidence.Medium)
    }

    @Test
    fun lifetimeEstimate_usesAverageSinceModelYear_withLowConfidence() {
        val pace = estimator.lifetimeEstimate(Kilometers(36_500), 2025, LocalDate.of(2026, 1, 1))

        assertThat(pace).isEqualTo(DrivingPace(100.0, Confidence.Low))
    }

    @Test
    fun lifetimeEstimate_skipsNewCarsAndMissingInputs() {
        val today = LocalDate.of(2026, 3, 1)

        assertThat(estimator.lifetimeEstimate(Kilometers(3_000), 2026, today)).isNull()
        assertThat(estimator.lifetimeEstimate(null, 2020, today)).isNull()
        assertThat(estimator.lifetimeEstimate(Kilometers(3_000), null, today)).isNull()
    }
}
