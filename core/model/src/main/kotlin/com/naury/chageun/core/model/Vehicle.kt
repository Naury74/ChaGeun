package com.naury.chageun.core.model

import java.time.LocalDate

@JvmInline
value class VehicleId(val value: String)

enum class FuelType { Gasoline, Diesel, Lpg, Hybrid, PlugInHybrid, Electric, Hydrogen }

enum class RegistrationMode { Auto, Manual }

data class Vehicle(
    val id: VehicleId,
    val maker: String,
    val model: String,
    val modelYear: Int?,
    val trim: String?,
    val fuelType: FuelType?,
    val firstRegistrationDate: LocalDate?,
    val plateMasked: String?,
    val registrationMode: RegistrationMode,
    val isPrimary: Boolean,
)

data class VehicleRegistration(
    val maker: String,
    val model: String,
    val modelYear: Int,
    val fuelType: FuelType,
    val currentMileage: Kilometers,
    val plate: PlateNumber? = null,
    val trim: String? = null,
    val firstRegistrationDate: LocalDate? = null,
    val knownServices: Map<MaintenanceItem, ServiceRecord> = emptyMap(),
)
