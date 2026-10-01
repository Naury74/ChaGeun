package com.naury.chageun.core.domain.maintenance

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RuleSource
import org.junit.Test

class DefaultMaintenanceRulesTest {

    @Test
    fun coversAllItems_forGasoline() {
        val items = DefaultMaintenanceRules.forFuel(FuelType.Gasoline).map { it.item }

        assertThat(items).containsExactlyElementsIn(MaintenanceItem.entries)
    }

    @Test
    fun dropsCombustionItems_forElectric() {
        val items = DefaultMaintenanceRules.forFuel(FuelType.Electric).map { it.item }

        assertThat(
            items,
        ).containsNoneOf(MaintenanceItem.EngineOil, MaintenanceItem.SparkPlug, MaintenanceItem.AirFilter)
        assertThat(items).containsAtLeast(MaintenanceItem.Tire, MaintenanceItem.BrakeFluid, MaintenanceItem.Battery)
    }

    @Test
    fun dropsSparkPlug_forDiesel() {
        assertThat(DefaultMaintenanceRules.forFuel(FuelType.Diesel).map { it.item })
            .doesNotContain(MaintenanceItem.SparkPlug)
    }

    @Test
    fun marksEveryDefaultAsGeneric() {
        FuelType.entries.flatMap(DefaultMaintenanceRules::forFuel).forEach {
            assertThat(it.source).isEqualTo(RuleSource.Generic)
        }
    }
}
