package com.naury.chageun.core.ui

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.RuleSource
import org.junit.Test

class MaintenanceProgressTest {

    private val rule = MaintenanceRule(MaintenanceItem.EngineOil, intervalKm = 10_000, intervalMonths = 12)

    private fun status(remainingKm: Long?, remainingDays: Long?) = MaintenanceStatus(
        item = MaintenanceItem.EngineOil,
        state = MaintenanceState.Good,
        remainingKm = remainingKm,
        remainingDays = remainingDays,
        distanceDue = null,
        dateDue = null,
        estimatedDue = null,
        missingInputs = emptySet(),
        ruleSource = RuleSource.Generic,
    )

    @Test
    fun usesTheDimensionFurtherAlong() {
        assertThat(usedFraction(status(remainingKm = 8_000, remainingDays = 30), rule)).isWithin(0.01f).of(0.92f)
    }

    @Test
    fun clampsOverdueToFull() {
        assertThat(usedFraction(status(remainingKm = -500, remainingDays = 200), rule)).isEqualTo(1f)
    }

    @Test
    fun returnsNull_whenNothingWasEvaluated() {
        assertThat(usedFraction(status(remainingKm = null, remainingDays = null), rule)).isNull()
    }
}
