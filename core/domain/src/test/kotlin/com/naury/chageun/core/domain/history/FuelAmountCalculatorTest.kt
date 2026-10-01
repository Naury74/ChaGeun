package com.naury.chageun.core.domain.history

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelField
import org.junit.Test

class FuelAmountCalculatorTest {

    @Test
    fun computesVolume_fromTotalAndUnitPrice() {
        assertThat(FuelAmountCalculator.complete(70_000, null, 1_700))
            .isEqualTo(FuelAmounts(70_000, 41_176, 1_700, FuelField.Volume))
    }

    @Test
    fun computesTotal_fromVolumeAndUnitPrice_roundingHalfUp() {
        assertThat(FuelAmountCalculator.complete(null, 40_250, 1_689))
            .isEqualTo(FuelAmounts(67_982, 40_250, 1_689, FuelField.Total))
    }

    @Test
    fun computesUnitPrice_fromTotalAndVolume() {
        assertThat(FuelAmountCalculator.complete(50_000, 30_000, null))
            .isEqualTo(FuelAmounts(50_000, 30_000, 1_667, FuelField.UnitPrice))
    }

    @Test
    fun keepsAllThreeEnteredValues_asIs() {
        assertThat(FuelAmountCalculator.complete(70_000, 41_000, 1_700))
            .isEqualTo(FuelAmounts(70_000, 41_000, 1_700, computedField = null))
    }

    @Test
    fun returnsNull_withFewerThanTwoValuesOrNonPositive() {
        assertThat(FuelAmountCalculator.complete(70_000, null, null)).isNull()
        assertThat(FuelAmountCalculator.complete(0, 41_000, null)).isNull()
    }
}
