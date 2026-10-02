package com.naury.chageun.core.model

enum class VehicleHealthLevel { NeedsAttention, Upcoming, InsufficientData, Good }

/** 표시 우선순위대로 정렬한다. 첫 번째 사유가 헤드라인 메시지를 정한다. */
sealed interface HealthReason {
    data object ActiveSafetyRecall : HealthReason
    data object InspectionOverdue : HealthReason
    data class SafetyItemOverdue(val item: MaintenanceItem) : HealthReason
    data class ItemOverdue(val item: MaintenanceItem) : HealthReason
    data object InspectionDueSoon : HealthReason
    data class ItemDueSoon(val item: MaintenanceItem) : HealthReason
    data class SafetyItemUnknown(val item: MaintenanceItem) : HealthReason
    data class ItemUnknown(val item: MaintenanceItem) : HealthReason
}

data class VehicleHealth(val level: VehicleHealthLevel, val reasons: List<HealthReason>)
