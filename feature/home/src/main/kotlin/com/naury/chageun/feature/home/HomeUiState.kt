package com.naury.chageun.feature.home

import com.naury.chageun.core.domain.maintenance.MILEAGE_PROMPT_AFTER_DAYS
import com.naury.chageun.core.model.CutoutStatus
import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.MaintenanceOverview
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.Vehicle
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(
        val vehicle: Vehicle,
        val overview: MaintenanceOverview,
        val today: LocalDate,
        val recentRecords: List<TimelineItem> = emptyList(),
        val photoPath: String? = null,
        val cutout: CutoutStatus = CutoutStatus.Idle,
        /** 로그인한 사용자를 부를 이름. 로그인하지 않았으면 이름 없이 인사한다. */
        val greetingName: String? = null,
        val dayPart: DayPart = DayPart.Morning,
    ) : HomeUiState {
        val needsAttention: List<MaintenanceStatus> =
            overview.statuses.filter { it.state == MaintenanceState.Overdue || it.state == MaintenanceState.Due }
        val upcoming: List<MaintenanceStatus> = overview.statuses.filter { it.state == MaintenanceState.Upcoming }
        val missingInfo: List<MaintenanceStatus> = overview.statuses.filter { it.state == MaintenanceState.Unknown }
        val goodCount: Int = overview.statuses.count { it.state == MaintenanceState.Good }
        val attentionCount: Int = needsAttention.size +
            overview.health.reasons.count {
                it == HealthReason.ActiveSafetyRecall || it == HealthReason.InspectionOverdue
            }
        val needsMileageUpdate: Boolean = overview.currentMileage
            ?.let { ChronoUnit.DAYS.between(it.date, today) >= MILEAGE_PROMPT_AFTER_DAYS }
            ?: true
    }
}
