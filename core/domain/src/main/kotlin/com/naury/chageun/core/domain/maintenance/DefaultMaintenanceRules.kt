package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.RuleSource

/**
 * 제조사 정비 주기를 쓸 수 있을 때까지 사용하는 일반 주기. 항상 [RuleSource.Generic]으로 표시해
 * UI가 공식 값이 아닌 일반 가이드로 안내하게 한다.
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

    fun isApplicable(item: MaintenanceItem, fuelType: FuelType): Boolean = item !in NOT_APPLICABLE[fuelType].orEmpty()

    fun forFuel(fuelType: FuelType): List<MaintenanceRule> {
        val excluded = NOT_APPLICABLE[fuelType].orEmpty()
        return GENERIC.filterNot { it.item in excluded }
    }

    /** [item]의 일반 규칙. 연료 유형에 해당하지 않는 항목이면 규칙을 빼지 않고 비활성 상태로 둔다. */
    fun genericFor(item: MaintenanceItem, fuelType: FuelType): MaintenanceRule? {
        val rule = GENERIC.firstOrNull { it.item == item } ?: return null
        return rule.copy(isEnabled = item !in NOT_APPLICABLE[fuelType].orEmpty())
    }
}
