package com.naury.chageun.data.vehicle

import com.naury.chageun.core.database.entity.MaintenanceRuleEntity
import com.naury.chageun.core.database.entity.VehicleEntity
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.RegistrationMode
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleId

internal fun VehicleEntity.asExternalModel() = Vehicle(
    id = VehicleId(id),
    maker = maker,
    model = model,
    modelYear = modelYear,
    trim = trim,
    fuelType = fuelType?.let { stored -> FuelType.entries.firstOrNull { it.name == stored } },
    firstRegistrationDate = firstRegistrationDate,
    displacementCc = displacementCc,
    plateMasked = plateMasked,
    registrationMode = RegistrationMode.valueOf(registrationMode),
    isPrimary = isPrimary,
)

internal fun MaintenanceRule.asEntity(id: String, vehicleId: String) = MaintenanceRuleEntity(
    id = id,
    vehicleId = vehicleId,
    itemType = item.name,
    intervalKm = intervalKm,
    intervalMonths = intervalMonths,
    dueSoonKm = thresholds.dueSoonKm,
    dueSoonDays = thresholds.dueSoonDays,
    upcomingKm = thresholds.upcomingKm,
    upcomingDays = thresholds.upcomingDays,
    ruleSource = source.name,
    sourceTitle = sourceTitle,
    sourceUrl = sourceUrl,
    isEnabled = isEnabled,
)
