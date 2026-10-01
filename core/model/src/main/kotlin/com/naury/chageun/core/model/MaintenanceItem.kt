package com.naury.chageun.core.model

enum class MaintenanceCategory { Engine, Climate, Brake, Drivetrain, Electrical, Chassis, Visibility }

enum class MaintenanceItem(val category: MaintenanceCategory, val isSafetyCritical: Boolean = false) {
    EngineOil(MaintenanceCategory.Engine),
    OilFilter(MaintenanceCategory.Engine),
    AirFilter(MaintenanceCategory.Engine),
    SparkPlug(MaintenanceCategory.Engine),
    CabinFilter(MaintenanceCategory.Climate),
    BrakePad(MaintenanceCategory.Brake, isSafetyCritical = true),
    BrakeFluid(MaintenanceCategory.Brake, isSafetyCritical = true),
    Coolant(MaintenanceCategory.Drivetrain),
    TransmissionOil(MaintenanceCategory.Drivetrain),
    Battery(MaintenanceCategory.Electrical),
    Tire(MaintenanceCategory.Chassis, isSafetyCritical = true),
    Wiper(MaintenanceCategory.Visibility),
}
