@file:Suppress("TooManyFunctions")

package com.naury.chageun.data.backup

import com.naury.chageun.core.database.entity.AttachmentEntity
import com.naury.chageun.core.database.entity.CheckRecordEntity
import com.naury.chageun.core.database.entity.FuelRecordEntity
import com.naury.chageun.core.database.entity.InspectionScheduleEntity
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MaintenanceRuleEntity
import com.naury.chageun.core.database.entity.MileageRecordEntity
import com.naury.chageun.core.database.entity.VehicleEntity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal fun VehicleEntity.toDto() = VehicleDto(
    id = id,
    maker = maker,
    model = model,
    modelYear = modelYear,
    trim = trim,
    fuelType = fuelType,
    firstRegistrationDate = firstRegistrationDate?.toString(),
    plateMasked = plateMasked,
    registrationMode = registrationMode,
    isPrimary = isPrimary,
    createdAt = createdAt.toString(),
)

/** The encrypted plate is not restored: it was sealed with the exporting device's Keystore key. */
internal fun VehicleDto.toEntity(now: Instant) = VehicleEntity(
    id = id,
    plateNumberEncrypted = null,
    plateMasked = plateMasked,
    maker = maker,
    model = model,
    modelYear = modelYear,
    trim = trim,
    fuelType = fuelType,
    firstRegistrationDate = firstRegistrationDate?.let(LocalDate::parse),
    vinEncrypted = null,
    registrationMode = registrationMode,
    isPrimary = isPrimary,
    createdAt = Instant.parse(createdAt),
    updatedAt = now,
)

internal fun MileageRecordEntity.toDto() =
    MileageDto(id, vehicleId, mileageKm, recordedOn.toString(), sourceType, relatedRecordId, createdAt.toString())

internal fun MileageDto.toEntity() = MileageRecordEntity(
    id,
    vehicleId,
    mileageKm,
    LocalDate.parse(recordedOn),
    source,
    relatedRecordId,
    Instant.parse(createdAt),
)

internal fun MaintenanceRuleEntity.toDto() = RuleDto(
    vehicleId = vehicleId,
    item = itemType,
    intervalKm = intervalKm,
    intervalMonths = intervalMonths,
    dueSoonKm = dueSoonKm,
    dueSoonDays = dueSoonDays,
    upcomingKm = upcomingKm,
    upcomingDays = upcomingDays,
    source = ruleSource,
    enabled = isEnabled,
)

internal fun RuleDto.toEntity() = MaintenanceRuleEntity(
    id = UUID.randomUUID().toString(),
    vehicleId = vehicleId,
    itemType = item,
    intervalKm = intervalKm,
    intervalMonths = intervalMonths,
    dueSoonKm = dueSoonKm,
    dueSoonDays = dueSoonDays,
    upcomingKm = upcomingKm,
    upcomingDays = upcomingDays,
    ruleSource = source,
    sourceTitle = null,
    sourceUrl = null,
    isEnabled = enabled,
)

internal fun MaintenanceRecordEntity.toDto() = MaintenanceDto(
    id = id,
    vehicleId = vehicleId,
    item = itemType,
    date = serviceDate?.toString(),
    mileageKm = mileageKm,
    costWon = costWon,
    shop = shopName,
    memo = memo,
    createdAt = createdAt.toString(),
)

internal fun MaintenanceDto.toEntity(now: Instant) = MaintenanceRecordEntity(
    id = id,
    vehicleId = vehicleId,
    itemType = item,
    serviceDate = date?.let(LocalDate::parse),
    mileageKm = mileageKm,
    costWon = costWon,
    shopName = shop,
    memo = memo,
    sourceType = SOURCE_USER,
    createdAt = Instant.parse(createdAt),
    updatedAt = now,
)

internal fun FuelRecordEntity.toDto() = FuelDto(
    id = id,
    vehicleId = vehicleId,
    date = fuelDate.toString(),
    mileageKm = mileageKm,
    totalWon = totalPriceWon,
    volumeMl = volumeMl,
    unitPriceWon = unitPriceWon,
    computedField = computedField,
    fullTank = isFullTank,
    station = stationName,
    memo = memo,
    createdAt = createdAt.toString(),
)

internal fun FuelDto.toEntity(now: Instant) = FuelRecordEntity(
    id = id,
    vehicleId = vehicleId,
    fuelDate = LocalDate.parse(date),
    mileageKm = mileageKm,
    totalPriceWon = totalWon,
    volumeMl = volumeMl,
    unitPriceWon = unitPriceWon,
    computedField = computedField,
    isFullTank = fullTank,
    stationName = station,
    memo = memo,
    createdAt = Instant.parse(createdAt),
    updatedAt = now,
)

internal fun CheckRecordEntity.toDto() =
    CheckDto(id, vehicleId, kind, checkDate.toString(), title, mileageKm, costWon, memo, createdAt.toString())

internal fun CheckDto.toEntity(now: Instant) = CheckRecordEntity(
    id = id,
    vehicleId = vehicleId,
    kind = kind,
    checkDate = LocalDate.parse(date),
    title = title,
    mileageKm = mileageKm,
    costWon = costWon,
    memo = memo,
    createdAt = Instant.parse(createdAt),
    updatedAt = now,
)

internal fun AttachmentEntity.toDto() =
    AttachmentDto(id, vehicleId, ownerType, ownerId, fileName, thumbnailName, sizeBytes, createdAt.toString())

internal fun AttachmentDto.toEntity() =
    AttachmentEntity(id, vehicleId, ownerType, ownerId, file, thumbnail, MIME_JPEG, sizeBytes, Instant.parse(createdAt))

private const val SOURCE_USER = "USER"
private const val MIME_JPEG = "image/jpeg"

internal fun InspectionScheduleEntity.toDto() =
    InspectionDto(vehicleId, nextDueDate.toString(), source, updatedAt.toString())

/** Reminder progress is not exported; a restored date notifies again from its current stage. */
internal fun InspectionDto.toEntity() = InspectionScheduleEntity(
    vehicleId = vehicleId,
    nextDueDate = LocalDate.parse(nextDueDate),
    source = source,
    notifiedStage = null,
    updatedAt = Instant.parse(updatedAt),
)
