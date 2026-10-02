package com.naury.chageun.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.naury.chageun.core.domain.settings.SettingsRepository
import com.naury.chageun.core.model.ThemeMode
import com.naury.chageun.core.model.UserSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class PreferencesSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            themeMode =
            prefs[THEME_MODE]?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                ?: ThemeMode.System,
            isMaintenanceReminderEnabled = prefs[MAINTENANCE_REMINDER] ?: true,
            isMileageReminderEnabled = prefs[MILEAGE_REMINDER] ?: false,
            isUsageStatsEnabled = prefs[USAGE_STATS] ?: true,
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    override suspend fun setMaintenanceReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[MAINTENANCE_REMINDER] = enabled }
    }

    override suspend fun setMileageReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[MILEAGE_REMINDER] = enabled }
    }

    override suspend fun setUsageStatsEnabled(enabled: Boolean) {
        dataStore.edit { it[USAGE_STATS] = enabled }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MAINTENANCE_REMINDER = booleanPreferencesKey("maintenance_reminder_enabled")
        val MILEAGE_REMINDER = booleanPreferencesKey("mileage_reminder_enabled")
        val USAGE_STATS = booleanPreferencesKey("usage_stats_enabled")
    }
}
