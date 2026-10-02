package com.naury.chageun.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.analytics.ReminderKind
import com.naury.chageun.core.domain.settings.SettingsRepository
import com.naury.chageun.core.model.ThemeMode
import com.naury.chageun.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UserSettings())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setMaintenanceReminderEnabled(enabled: Boolean) {
        if (enabled) analytics.track(AnalyticsEvent.ReminderEnabled(ReminderKind.MaintenanceAndInspection))
        viewModelScope.launch { settingsRepository.setMaintenanceReminderEnabled(enabled) }
    }

    fun setMileageReminderEnabled(enabled: Boolean) {
        if (enabled) analytics.track(AnalyticsEvent.ReminderEnabled(ReminderKind.Mileage))
        viewModelScope.launch { settingsRepository.setMileageReminderEnabled(enabled) }
    }

    fun setUsageStatsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUsageStatsEnabled(enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
