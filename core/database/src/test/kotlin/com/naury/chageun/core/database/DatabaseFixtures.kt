package com.naury.chageun.core.database

import com.naury.chageun.core.database.entity.CheckRecordEntity
import com.naury.chageun.core.database.entity.FuelRecordEntity
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MileageRecordEntity
import com.naury.chageun.core.database.entity.VehicleEntity
import java.time.Instant
import java.time.LocalDate

internal val FIXED_NOW: Instant = Instant.parse("2026-10-01T00:00:00Z")

internal fun vehicle(id: String = "vehicle-1", isPrimary: Boolean = true) = VehicleEntity(
    id = id,
    plateNumberEncrypted = null,
    plateMasked = "12가 **34",
    maker = "Maker",
    model = "Model",
    modelYear = 2023,
    trim = null,
    fuelType = "GASOLINE",
    firstRegistrationDate = LocalDate.of(2023, 3, 1),
    vinEncrypted = null,
    registrationMode = "MANUAL",
    isPrimary = isPrimary,
    createdAt = FIXED_NOW,
    updatedAt = FIXED_NOW,
)

internal fun mileage(id: String, km: Long, on: LocalDate, vehicleId: String = "vehicle-1") = MileageRecordEntity(
    id = id,
    vehicleId = vehicleId,
    mileageKm = km,
    recordedOn = on,
    sourceType = "USER",
    relatedRecordId = null,
    createdAt = FIXED_NOW,
)

internal fun service(id: String, item: String, on: LocalDate?, createdAt: Instant = FIXED_NOW, costWon: Long? = null) =
    MaintenanceRecordEntity(
        id = id,
        vehicleId = "vehicle-1",
        itemType = item,
        serviceDate = on,
        mileageKm = 40_000,
        costWon = costWon,
        shopName = null,
        memo = null,
        sourceType = "USER",
        createdAt = createdAt,
        updatedAt = createdAt,
    )

internal object TimelineEventTypes {
    val ALL = listOf("Maintenance", "Fuel", "Inspection", "Repair", "Note")
}

internal fun fuel(id: String, on: LocalDate, station: String? = null) = FuelRecordEntity(
    id = id,
    vehicleId = "vehicle-1",
    fuelDate = on,
    mileageKm = 43_000,
    totalPriceWon = 70_000,
    volumeMl = 41_176,
    unitPriceWon = 1_700,
    computedField = "Volume",
    isFullTank = true,
    stationName = station,
    memo = null,
    createdAt = FIXED_NOW,
    updatedAt = FIXED_NOW,
)

internal fun check(id: String, kind: String, on: LocalDate, title: String) = CheckRecordEntity(
    id = id,
    vehicleId = "vehicle-1",
    kind = kind,
    checkDate = on,
    title = title,
    mileageKm = null,
    costWon = 120_000,
    memo = null,
    createdAt = FIXED_NOW,
    updatedAt = FIXED_NOW,
)
