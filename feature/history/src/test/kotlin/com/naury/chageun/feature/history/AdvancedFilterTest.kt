package com.naury.chageun.feature.history

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.history.TimelineQuery
import java.time.LocalDate
import org.junit.Test

class AdvancedFilterTest {

    private val today = LocalDate.of(2026, 10, 6)

    @Test
    fun presetsResolveFromToday() {
        val thisYear = AdvancedFilter(period = HistoryPeriod.ThisYear).applyTo(TimelineQuery(), today)
        val month = AdvancedFilter(period = HistoryPeriod.LastMonth).applyTo(TimelineQuery(), today)

        assertThat(thisYear.dateFrom).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(month.dateFrom).isEqualTo(LocalDate.of(2026, 9, 6))
        assertThat(month.dateTo).isEqualTo(today)
    }

    @Test
    fun roundTripsThroughSavedState() {
        val filter = AdvancedFilter(
            period = HistoryPeriod.Custom,
            customFrom = LocalDate.of(2026, 1, 1),
            customTo = LocalDate.of(2026, 2, 1),
            maxCostWon = 30_000,
            withAttachmentsOnly = true,
        )

        assertThat(AdvancedFilter.decode(filter.encode())).isEqualTo(filter)
        assertThat(AdvancedFilter.decode("broken")).isEqualTo(AdvancedFilter())
        assertThat(filter.activeCount).isEqualTo(3)
    }
}
