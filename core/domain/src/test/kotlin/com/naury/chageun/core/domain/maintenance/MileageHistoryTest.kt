package com.naury.chageun.core.domain.maintenance

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import java.time.LocalDate
import org.junit.Test

class MileageHistoryTest {

    private val today = LocalDate.of(2026, 10, 1)

    @Test
    fun picksLatestDate_andLastEntryOnThatDay() {
        val history = listOf(
            MileageReading(today.minusDays(5), Kilometers(42_000)),
            MileageReading(today, Kilometers(45_000)),
            MileageReading(today, Kilometers(1_200)),
        )

        assertThat(history.currentAsOf(today)?.mileage).isEqualTo(Kilometers(1_200))
    }

    @Test
    fun ignoresFutureReadings_andEmptyHistory() {
        assertThat(listOf(MileageReading(today.plusDays(1), Kilometers(1))).currentAsOf(today)).isNull()
        assertThat(emptyList<MileageReading>().currentAsOf(today)).isNull()
    }
}
