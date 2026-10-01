package com.naury.chageun.core.model

data class MaintenanceOverview(
    val statuses: List<MaintenanceStatus>,
    val rules: Map<MaintenanceItem, MaintenanceRule>,
    val disabledItems: List<MaintenanceItem>,
    val health: VehicleHealth,
    val currentMileage: MileageReading?,
    val inspection: InspectionStatus = InspectionStatus.Unknown,
)
