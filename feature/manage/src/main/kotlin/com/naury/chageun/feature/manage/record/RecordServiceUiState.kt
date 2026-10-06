package com.naury.chageun.feature.manage.record

import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import java.time.LocalDate

/** 교체 기록 시트가 다루는 항목. [editingRecordId]가 있으면 그 기록을 고친다. */
data class RecordServiceTarget(val item: MaintenanceItem, val editingRecordId: String? = null)

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
    /** 저장된 기록을 고치는 중이다. 저장하면 다음 교체 안내 없이 바로 닫는다. */
    val isEditing: Boolean = false,
    val isEditSaved: Boolean = false,
)

data class SavedResult(val nextDistanceDue: Kilometers?, val nextDateDue: LocalDate?)
