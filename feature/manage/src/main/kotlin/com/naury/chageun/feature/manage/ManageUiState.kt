package com.naury.chageun.feature.manage

import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceHistoryEntry

enum class ManageFilter {
    All,
    NeedsAttention,
    Upcoming,
    ;

    fun accepts(status: MaintenanceStatus): Boolean = when (this) {
        All -> true
        NeedsAttention -> status.state == MaintenanceState.Overdue || status.state == MaintenanceState.Due
        Upcoming -> status.state == MaintenanceState.Upcoming
    }
}

data class ManageDetail(
    val status: MaintenanceStatus,
    val rule: MaintenanceRule?,
    val history: List<ServiceHistoryEntry>,
    val currentMileage: MileageReading?,
)

data class ManageUiState(
    val isLoading: Boolean = true,
    val filter: ManageFilter = ManageFilter.All,
    val items: List<MaintenanceStatus> = emptyList(),
    val counts: Map<ManageFilter, Int> = emptyMap(),
    val rules: Map<MaintenanceItem, MaintenanceRule> = emptyMap(),
    val disabledItems: List<MaintenanceItem> = emptyList(),
    val selectedItem: MaintenanceItem? = null,
    val detail: ManageDetail? = null,
)
