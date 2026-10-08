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
    /** 엔진 배기량(cc). 전기·수소차이거나 입력하지 않았으면 null이다. */
    val displacementCc: Int?,
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

/** 등록한 뒤 고칠 수 있는 차량 정보. 주행거리는 주행거리 기록으로 따로 관리한다. */
data class VehicleProfileUpdate(
    val maker: String,
    val model: String,
    val modelYear: Int,
    val fuelType: FuelType,
    val trim: String?,
    val plate: PlateChange,
    /** 자동차등록증의 최초 등록일. null이면 비운다. */
    val firstRegistrationDate: LocalDate?,
    /** 엔진 배기량(cc). null이면 비운다. */
    val displacementCc: Int?,
)

sealed interface PlateChange {
    data object Keep : PlateChange

    data object Remove : PlateChange

    data class Replace(val plate: PlateNumber) : PlateChange
}

enum class MileageSource { User, Maintenance, Fuel, Check, Correction, Inspection }

data class MileageEntry(val id: String, val date: LocalDate, val mileage: Kilometers, val source: MileageSource)
