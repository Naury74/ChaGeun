package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.VehicleHealth
import com.naury.chageun.core.model.VehicleHealthLevel
import javax.inject.Inject

data class VehicleHealthInput(
    val maintenance: List<MaintenanceStatus>,
    val hasActiveSafetyRecall: Boolean,
    val inspection: InspectionState,
)

enum class InspectionState { Ok, DueSoon, Overdue, Unknown }

/**
 * Derives the overall vehicle status from its most important active finding rather than an average.
 *
 * A safety-critical item that cannot be evaluated outranks GOOD, so missing tire or brake data
 * always surfaces as insufficient data.
 */
class VehicleHealthAggregator @Inject constructor() {

    fun aggregate(input: VehicleHealthInput): VehicleHealth {
        val reasons = buildList {
            if (input.hasActiveSafetyRecall) add(HealthReason.ActiveSafetyRecall)
            if (input.inspection == InspectionState.Overdue) add(HealthReason.InspectionOverdue)

            val byState = input.maintenance.groupBy { it.state }
            byState.itemsIn(MaintenanceState.Overdue).let { overdue ->
                overdue.filter { it.isSafetyCritical }.forEach { add(HealthReason.SafetyItemOverdue(it)) }
                overdue.filterNot { it.isSafetyCritical }.forEach { add(HealthReason.ItemOverdue(it)) }
            }

            if (input.inspection == InspectionState.DueSoon) add(HealthReason.InspectionDueSoon)
            (byState.itemsIn(MaintenanceState.Due) + byState.itemsIn(MaintenanceState.Upcoming))
                .forEach { add(HealthReason.ItemDueSoon(it)) }

            byState.itemsIn(MaintenanceState.Unknown).let { unknown ->
                unknown.filter { it.isSafetyCritical }.forEach { add(HealthReason.SafetyItemUnknown(it)) }
                unknown.filterNot { it.isSafetyCritical }.forEach { add(HealthReason.ItemUnknown(it)) }
            }
        }
        return VehicleHealth(level = levelFor(reasons, input.maintenance), reasons = reasons)
    }

    private fun levelFor(reasons: List<HealthReason>, maintenance: List<MaintenanceStatus>): VehicleHealthLevel {
        val hasAnyEvaluatedItem = maintenance.any { it.state != MaintenanceState.Unknown }
        val headline = reasons.firstOrNull()
            ?: return if (hasAnyEvaluatedItem) VehicleHealthLevel.Good else VehicleHealthLevel.InsufficientData
        return when (headline) {
            HealthReason.ActiveSafetyRecall,
            HealthReason.InspectionOverdue,
            is HealthReason.SafetyItemOverdue,
            is HealthReason.ItemOverdue,
            -> VehicleHealthLevel.NeedsAttention

            HealthReason.InspectionDueSoon,
            is HealthReason.ItemDueSoon,
            -> VehicleHealthLevel.Upcoming

            is HealthReason.SafetyItemUnknown -> VehicleHealthLevel.InsufficientData

            // Unknown non-safety items alone are prompted on Home, but GOOD still needs at least one evaluated item.
            is HealthReason.ItemUnknown ->
                if (hasAnyEvaluatedItem) VehicleHealthLevel.Good else VehicleHealthLevel.InsufficientData
        }
    }

    private fun Map<MaintenanceState, List<MaintenanceStatus>>.itemsIn(state: MaintenanceState) =
        get(state).orEmpty().map { it.item }
}
