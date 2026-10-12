package com.naury.chageun.feature.home

import com.naury.chageun.core.domain.greeting.Greeting
import com.naury.chageun.core.domain.greeting.GreetingSituation
import com.naury.chageun.core.domain.maintenance.MILEAGE_PROMPT_AFTER_DAYS
import com.naury.chageun.core.domain.weather.WeatherTip
import com.naury.chageun.core.model.CutoutStatus
import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.MaintenanceOverview
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.WeatherLocation
import com.naury.chageun.core.model.WeatherSnapshot
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
        val isBackgroundRemovalEnabled: Boolean = true,
        /** 로그인한 사용자를 부를 이름. 로그인하지 않았으면 이름 없이 인사한다. */
        val greetingName: String? = null,
        /** 상황에 맞춰 고른 인사말. 문구는 화면이 리소스에서 고른다. */
        val greeting: Greeting = Greeting(GreetingSituation.Morning, seed = 0),
        val weather: HomeWeather = HomeWeather.Off,
        /** 오늘부터 2주 안의 예정 일정. 가까운 것부터 최대 3개다. */
        val upcomingEvents: List<UpcomingEvent> = emptyList(),
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

/** 홈 인사말 아래 날씨 카드의 상태. */
sealed interface HomeWeather {
    /** 설정에서 날씨를 껐다. 아무것도 보이지 않는다. */
    data object Off : HomeWeather

    /** 아직 위치를 고르지 않았다. "날씨 보기"를 눌러야 위치를 묻는다. */
    data object Setup : HomeWeather

    /** "현재 위치"를 골랐지만 위치 권한이 없다. */
    data object NeedsPermission : HomeWeather

    data object Loading : HomeWeather

    data class Shown(val snapshot: WeatherSnapshot, val location: WeatherLocation, val tip: WeatherTip) : HomeWeather

    /** 위치나 날씨를 구하지 못했다. 오류를 띄우지 않고 카드를 숨긴다. */
    data object Unavailable : HomeWeather
}
