package com.naury.chageun.feature.manage.record

import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import java.time.LocalDate

enum class RecordServiceField { Date, Mileage, Cost }

enum class RecordServiceError { Required, FutureDate, InvalidNumber }

data class RecordServiceUiState(
    val item: MaintenanceItem,
    val date: LocalDate,
    val mileage: String = "",
    val cost: String = "",
    val shopName: String = "",
    val memo: String = "",
    val errors: Map<RecordServiceField, RecordServiceError> = emptyMap(),
    val lowerMileageWarning: Kilometers? = null,
    val isSaving: Boolean = false,
    val hasSaveFailed: Boolean = false,
    val savedResult: SavedResult? = null,
)

data class SavedResult(val nextDistanceDue: Kilometers?, val nextDateDue: LocalDate?)
