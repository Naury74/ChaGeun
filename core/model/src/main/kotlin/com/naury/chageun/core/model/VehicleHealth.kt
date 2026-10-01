package com.naury.chageun.core.model

enum class VehicleHealthLevel { NeedsAttention, Upcoming, InsufficientData, Good }

/** Ordered by display priority; the first reason drives the headline message. */
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
