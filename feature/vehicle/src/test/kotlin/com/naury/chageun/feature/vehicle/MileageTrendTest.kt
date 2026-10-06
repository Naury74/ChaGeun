package com.naury.chageun.feature.vehicle

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.MileageSource
import java.time.LocalDate
import org.junit.Test

class MileageTrendTest {

    private fun entry(date: LocalDate, km: Long) = MileageEntry("$date-$km", date, Kilometers(km), MileageSource.User)

    @Test
    fun needsTwoDifferentDays() {
        val day = LocalDate.of(2026, 10, 1)

        assertThat(MileageTrend.from(emptyList())).isNull()
        assertThat(MileageTrend.from(listOf(entry(day, 1_000), entry(day, 1_200)))).isNull()
    }

    @Test
    fun sortsByDate_andKeepsTheLastValueOfADay() {
        val trend = MileageTrend.from(
            listOf(
                entry(LocalDate.of(2026, 10, 1), 42_800),
                entry(LocalDate.of(2026, 1, 1), 30_000),
                entry(LocalDate.of(2026, 10, 1), 42_850),
            ),
        )!!

        assertThat(trend.points.map { it.km }).containsExactly(30_000L, 42_850L).inOrder()
        assertThat(trend.minKm).isEqualTo(30_000)
        assertThat(trend.maxKm).isEqualTo(42_850)
    }

    @Test
    fun monthlyAverage_overTheWholePeriod() {
        val trend = MileageTrend.from(
            listOf(entry(LocalDate.of(2026, 1, 1), 30_000), entry(LocalDate.of(2026, 7, 2), 36_000)),
        )!!

        // 182일에 6,000km → 한 달(30.4일) 약 1,002km
        assertThat(trend.monthlyAverageKm).isEqualTo(1_002)
    }

    @Test
    fun noMonthlyAverage_forLessThanTwoWeeks() {
        val trend = MileageTrend.from(
            listOf(entry(LocalDate.of(2026, 10, 1), 1_000), entry(LocalDate.of(2026, 10, 5), 1_300)),
        )!!

        assertThat(trend.monthlyAverageKm).isNull()
    }

    @Test
    fun fractionFollowsTime_notTheNumberOfRecords() {
        val trend = MileageTrend.from(
            listOf(
                entry(LocalDate.of(2026, 1, 1), 30_000),
                entry(LocalDate.of(2026, 1, 11), 30_400),
                entry(LocalDate.of(2026, 4, 11), 33_000),
            ),
        )!!

        assertThat(trend.fractionOf(LocalDate.of(2026, 1, 11))).isWithin(0.001f).of(0.1f)
    }

    @Test
    fun startsFromTheLastDashboardCorrection() {
        val trend = MileageTrend.from(
            listOf(
                entry(LocalDate.of(2026, 8, 1), 42_891),
                MileageEntry("fix", LocalDate.of(2026, 9, 1), Kilometers(1_000), MileageSource.Correction),
                entry(LocalDate.of(2026, 10, 1), 2_200),
            ),
        )!!

        assertThat(trend.points.map { it.km }).containsExactly(1_000L, 2_200L).inOrder()
        assertThat(trend.drivenKm).isEqualTo(1_200)
    }

    @Test
    fun correctionAsTheLatestRecord_leavesNothingToDraw() {
        val trend = MileageTrend.from(
            listOf(
                entry(LocalDate.of(2026, 9, 1), 42_891),
                MileageEntry("fix", LocalDate.of(2026, 10, 1), Kilometers(1_200), MileageSource.Correction),
            ),
        )

        assertThat(trend).isNull()
    }

    @Test
    fun decreasingWithoutCorrection_hasNoAverageOrChange() {
        val trend = MileageTrend.from(
            listOf(entry(LocalDate.of(2026, 1, 1), 5_000), entry(LocalDate.of(2026, 6, 1), 4_000)),
        )!!

        assertThat(trend.monthlyAverageKm).isNull()
        assertThat(trend.drivenKm).isNull()
    }
}
