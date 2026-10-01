package com.naury.chageun.core.domain.ai

import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.VehicleHealthLevel
import java.time.LocalDate

/**
 * Everything that may be shared with an external AI. There is deliberately no field for the plate number,
 * VIN, owner name, location, notes or attachments, so they cannot leak through a rendering mistake.
 */
data class AiContextFacts(
    val asOf: LocalDate,
    val maker: String,
    val model: String,
    val modelYear: Int?,
    val fuelType: FuelType?,
    val mileage: MileageReading?,
    val health: VehicleHealthLevel,
    val maintenance: List<MaintenanceStatus>,
    val missingInfo: List<MaintenanceItem>,
    val recentRecords: List<SharedRecord>,
)

/**
 * A history entry reduced to what helps answer questions. Memos and fuel station names are never included:
 * the first is free text and the second reveals places the user visits.
 */
data class SharedRecord(
    val type: TimelineEventType,
    val date: LocalDate?,
    val maintenanceItem: MaintenanceItem?,
    val title: String?,
    val mileage: Kilometers?,
    val costWon: Long?,
)

data class AiContextOptions(
    val includeMaintenance: Boolean = true,
    val includeRecords: Boolean = true,
    val includeCosts: Boolean = false,
    /** When set, only this item's maintenance status is shared. */
    val focusItem: MaintenanceItem? = null,
)
