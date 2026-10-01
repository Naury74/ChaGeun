package com.naury.chageun.feature.home

import com.naury.chageun.core.model.MaintenanceOverview
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.Vehicle
import java.time.LocalDate

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(val vehicle: Vehicle, val overview: MaintenanceOverview, val today: LocalDate) : HomeUiState {
        val needsAttention: List<MaintenanceStatus> =
            overview.statuses.filter { it.state == MaintenanceState.Overdue || it.state == MaintenanceState.Due }
        val upcoming: List<MaintenanceStatus> = overview.statuses.filter { it.state == MaintenanceState.Upcoming }
        val missingInfo: List<MaintenanceStatus> = overview.statuses.filter { it.state == MaintenanceState.Unknown }
        val goodCount: Int = overview.statuses.count { it.state == MaintenanceState.Good }
    }
}
