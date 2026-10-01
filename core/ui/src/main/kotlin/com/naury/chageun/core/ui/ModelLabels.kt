package com.naury.chageun.core.ui

import androidx.annotation.StringRes
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.MaintenanceItem

@get:StringRes
val MaintenanceItem.labelRes: Int
    get() = when (this) {
        MaintenanceItem.EngineOil -> R.string.maintenance_item_engine_oil
        MaintenanceItem.OilFilter -> R.string.maintenance_item_oil_filter
        MaintenanceItem.AirFilter -> R.string.maintenance_item_air_filter
        MaintenanceItem.SparkPlug -> R.string.maintenance_item_spark_plug
        MaintenanceItem.CabinFilter -> R.string.maintenance_item_cabin_filter
        MaintenanceItem.BrakePad -> R.string.maintenance_item_brake_pad
        MaintenanceItem.BrakeFluid -> R.string.maintenance_item_brake_fluid
        MaintenanceItem.Coolant -> R.string.maintenance_item_coolant
        MaintenanceItem.TransmissionOil -> R.string.maintenance_item_transmission_oil
        MaintenanceItem.Battery -> R.string.maintenance_item_battery
        MaintenanceItem.Tire -> R.string.maintenance_item_tire
        MaintenanceItem.Wiper -> R.string.maintenance_item_wiper
    }

@get:StringRes
val FuelType.labelRes: Int
    get() = when (this) {
        FuelType.Gasoline -> R.string.fuel_gasoline
        FuelType.Diesel -> R.string.fuel_diesel
        FuelType.Lpg -> R.string.fuel_lpg
        FuelType.Hybrid -> R.string.fuel_hybrid
        FuelType.PlugInHybrid -> R.string.fuel_plug_in_hybrid
        FuelType.Electric -> R.string.fuel_electric
        FuelType.Hydrogen -> R.string.fuel_hydrogen
    }
