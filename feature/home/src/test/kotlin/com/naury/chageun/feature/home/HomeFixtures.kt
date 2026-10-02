package com.naury.chageun.feature.home

import com.naury.chageun.core.domain.maintenance.DefaultMaintenanceRules
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceOverview
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.MissingInput
import com.naury.chageun.core.model.RegistrationMode
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleHealth
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.model.VehicleId
import java.time.LocalDate

internal val TODAY: LocalDate = LocalDate.of(2026, 10, 1)

internal val VEHICLE = Vehicle(
    id = VehicleId("v1"),
    maker = "KG Mobility",
    model = "Torres",
    modelYear = 2023,
    trim = null,
    fuelType = FuelType.Gasoline,
    firstRegistrationDate = null,
    plateMasked = null,
    registrationMode = RegistrationMode.Manual,
    isPrimary = true,
)

internal fun status(
    item: MaintenanceItem,
    state: MaintenanceState,
    remainingKm: Long? = null,
    remainingDays: Long? = null,
    missing: Set<MissingInput> = emptySet(),
) = MaintenanceStatus(
    item = item,
    state = state,
    remainingKm = remainingKm,
    remainingDays = remainingDays,
    distanceDue = null,
    dateDue = null,
    estimatedDue = null,
    missingInputs = missing,
    ruleSource = RuleSource.Generic,
)

internal fun content(level: VehicleHealthLevel, reasons: List<HealthReason>, vararg statuses: MaintenanceStatus) =
    HomeUiState.Content(
        vehicle = VEHICLE,
        overview = MaintenanceOverview(
            statuses = statuses.toList(),
            rules = statuses.mapNotNull {
                DefaultMaintenanceRules.genericFor(it.item, FuelType.Gasoline)
            }.associateBy { it.item },
            disabledItems = emptyList(),
            health = VehicleHealth(level, reasons),
            currentMileage = MileageReading(TODAY, Kilometers(42_180)),
        ),
        today = TODAY,
    )
