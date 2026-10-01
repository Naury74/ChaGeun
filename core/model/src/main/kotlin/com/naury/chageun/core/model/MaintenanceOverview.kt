package com.naury.chageun.core.model

data class MaintenanceOverview(
    val statuses: List<MaintenanceStatus>,
    val health: VehicleHealth,
    val currentMileage: MileageReading?,
)
