package com.naury.chageun.feature.onboarding

import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.ServiceRecord
import java.time.LocalDate

enum class QuickServiceMode { Unknown, DateAndMileage, DateOnly, MileageOnly }

data class QuickServiceInput(
    val mode: QuickServiceMode = QuickServiceMode.Unknown,
    val date: LocalDate? = null,
    val mileage: String = "",
) {
    val needsDate: Boolean get() = mode == QuickServiceMode.DateAndMileage || mode == QuickServiceMode.DateOnly
    val needsMileage: Boolean get() = mode == QuickServiceMode.DateAndMileage || mode == QuickServiceMode.MileageOnly
}

/** The three items asked during onboarding; the rest are filled in later from Home. */
val QUICK_SERVICE_ITEMS = listOf(MaintenanceItem.EngineOil, MaintenanceItem.Tire, MaintenanceItem.Battery)

internal object QuickServiceForm {

    fun validate(
        inputs: Map<MaintenanceItem, QuickServiceInput>,
        currentMileage: Long,
        today: LocalDate,
    ): Map<MaintenanceItem, FieldError> = buildMap {
        inputs.forEach { (item, input) ->
            val date = input.date
            val mileage = input.mileage.toLongOrNull()
            val error = when {
                input.needsDate && date == null -> FieldError.Required
                input.needsDate && date != null && date.isAfter(today) -> FieldError.FutureDate
                input.needsMileage && mileage == null -> FieldError.Required
                input.needsMileage && mileage != null && mileage > currentMileage -> FieldError.ExceedsCurrentMileage
                else -> null
            }
            error?.let { put(item, it) }
        }
    }

    fun toServiceRecords(inputs: Map<MaintenanceItem, QuickServiceInput>): Map<MaintenanceItem, ServiceRecord> = inputs
        .filterValues { it.mode != QuickServiceMode.Unknown }
        .mapValues { (_, input) ->
            ServiceRecord(
                date = input.date.takeIf { input.needsDate },
                mileage = input.mileage.toLongOrNull()?.takeIf { input.needsMileage }?.let(::Kilometers),
            )
        }
}
