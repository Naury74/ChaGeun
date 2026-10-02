package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.InspectionState
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

/**
 * 차량 전체 상태는 평균이 아니라 현재 가장 중요한 결과를 기준으로 정한다.
 *
 * 평가할 수 없는 안전 항목은 GOOD보다 우선하므로, 타이어나 브레이크 데이터가 없으면
 * 항상 데이터 부족으로 드러난다.
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

            // 안전 항목이 아닌 Unknown 항목만 있으면 Home에서 입력을 유도하지만, GOOD이 되려면 평가된 항목이 하나 이상 있어야 한다.
            is HealthReason.ItemUnknown ->
                if (hasAnyEvaluatedItem) VehicleHealthLevel.Good else VehicleHealthLevel.InsufficientData
        }
    }

    private fun Map<MaintenanceState, List<MaintenanceStatus>>.itemsIn(state: MaintenanceState) =
        get(state).orEmpty().map { it.item }
}
