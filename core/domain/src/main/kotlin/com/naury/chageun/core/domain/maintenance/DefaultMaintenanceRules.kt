package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.RuleSource

/**
 * Generic intervals used until manufacturer schedules are available. They are always tagged
 * [RuleSource.Generic] so the UI labels them as general guidance, never as official values.
 */
object DefaultMaintenanceRules {

    private val GENERIC = listOf(
        MaintenanceRule(MaintenanceItem.EngineOil, intervalKm = 10_000, intervalMonths = 12),
        MaintenanceRule(MaintenanceItem.OilFilter, intervalKm = 10_000, intervalMonths = 12),
        MaintenanceRule(MaintenanceItem.AirFilter, intervalKm = 20_000, intervalMonths = 24),
        MaintenanceRule(MaintenanceItem.SparkPlug, intervalKm = 40_000, intervalMonths = 48),
        MaintenanceRule(MaintenanceItem.CabinFilter, intervalKm = 15_000, intervalMonths = 12),
        MaintenanceRule(MaintenanceItem.BrakePad, intervalKm = 40_000, intervalMonths = 36),
        MaintenanceRule(MaintenanceItem.BrakeFluid, intervalKm = 40_000, intervalMonths = 24),
        MaintenanceRule(MaintenanceItem.Coolant, intervalKm = 40_000, intervalMonths = 48),
        MaintenanceRule(MaintenanceItem.TransmissionOil, intervalKm = 60_000, intervalMonths = 48),
        MaintenanceRule(MaintenanceItem.Battery, intervalKm = null, intervalMonths = 48),
        MaintenanceRule(MaintenanceItem.Tire, intervalKm = 50_000, intervalMonths = 48),
        MaintenanceRule(MaintenanceItem.Wiper, intervalKm = null, intervalMonths = 12),
    )

    private val NOT_APPLICABLE = mapOf(
        FuelType.Electric to setOf(
            MaintenanceItem.EngineOil,
            MaintenanceItem.OilFilter,
            MaintenanceItem.AirFilter,
            MaintenanceItem.SparkPlug,
        ),
        FuelType.Hydrogen to setOf(MaintenanceItem.EngineOil, MaintenanceItem.OilFilter, MaintenanceItem.SparkPlug),
        FuelType.Diesel to setOf(MaintenanceItem.SparkPlug),
    )

    fun forFuel(fuelType: FuelType): List<MaintenanceRule> {
        val excluded = NOT_APPLICABLE[fuelType].orEmpty()
        return GENERIC.filterNot { it.item in excluded }
    }
}
