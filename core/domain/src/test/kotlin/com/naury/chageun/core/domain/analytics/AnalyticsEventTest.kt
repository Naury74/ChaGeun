package com.naury.chageun.core.domain.analytics

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.HistoryView
import org.junit.Test

class AnalyticsEventTest {

    private val allEvents: List<AnalyticsEvent> = listOf(
        AnalyticsEvent.OnboardingStarted,
        AnalyticsEvent.ManualRegistrationUsed,
        AnalyticsEvent.OnboardingCompleted(withPlate = true, knownServiceCount = 2),
        AnalyticsEvent.MaintenanceRecordAdded(withCost = false),
        AnalyticsEvent.FuelRecordAdded,
        AnalyticsEvent.CheckRecordAdded,
        AnalyticsEvent.MileageUpdated(isCorrection = true),
        AnalyticsEvent.RecallOpened,
        AnalyticsEvent.AiQuestionStarted,
    ) + HomeAction.entries.map(AnalyticsEvent::HomeActionOpened) +
        ReminderKind.entries.map(AnalyticsEvent::ReminderEnabled) +
        AiTarget.entries.map(AnalyticsEvent::AiShareCompleted) +
        HistoryView.entries.map(AnalyticsEvent::HistoryViewChanged) +
        CalendarItemKind.entries.map(AnalyticsEvent::CalendarItemOpened)

    @Test
    fun namesAndKeys_areSnakeCase_withinFirebaseLimits() {
        val snakeCase = Regex("^[a-z][a-z0-9_]{0,39}$")
        allEvents.forEach { event ->
            assertThat(event.name).matches(snakeCase.toPattern())
            event.params.keys.forEach { assertThat(it).matches(snakeCase.toPattern()) }
        }
    }

    @Test
    fun calendarEvents_carryOnlyEnumKeys() {
        assertThat(AnalyticsEvent.HistoryViewChanged(HistoryView.Calendar).params).containsExactly("view", "calendar")
        assertThat(AnalyticsEvent.CalendarItemOpened(CalendarItemKind.MaintenanceDue).params)
            .containsExactly("kind", "maintenance_due")
    }

    @Test
    fun paramValues_areOnlyFixedTokens() {
        // 24.4 금지값은 자유 문자열이므로, 값이 소문자 토큰·Boolean·정수뿐이면 섞일 수 없다.
        val token = Regex("^([a-z_]+|true|false|\\d+)$")
        allEvents.flatMap { it.params.values }.forEach { assertThat(it).matches(token.toPattern()) }
    }
}
