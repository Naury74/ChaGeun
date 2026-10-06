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
            // 예전에는 정비 스위치 하나로 검사 알림까지 껐다. 따로 저장한 적이 없으면 그 값을 따른다.
            isInspectionReminderEnabled = prefs[INSPECTION_REMINDER] ?: prefs[MAINTENANCE_REMINDER] ?: true,
            isMileageReminderEnabled = prefs[MILEAGE_REMINDER] ?: false,
            isUsageStatsEnabled = prefs[USAGE_STATS] ?: true,
            isCloudAutoBackupEnabled = prefs[CLOUD_AUTO_BACKUP] ?: false,
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    override suspend fun setMaintenanceReminderEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            // 검사 알림을 따로 저장한 적이 없으면 지금까지 따르던 값을 먼저 굳혀, 정비만 바꿔도 검사가 같이 바뀌지 않게 한다.
            if (prefs[INSPECTION_REMINDER] == null) prefs[INSPECTION_REMINDER] = prefs[MAINTENANCE_REMINDER] ?: true
            prefs[MAINTENANCE_REMINDER] = enabled
        }
    }

    override suspend fun setInspectionReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[INSPECTION_REMINDER] = enabled }
    }

    override suspend fun setMileageReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[MILEAGE_REMINDER] = enabled }
    }

    override suspend fun setUsageStatsEnabled(enabled: Boolean) {
        dataStore.edit { it[USAGE_STATS] = enabled }
    }

    override suspend fun setCloudAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[CLOUD_AUTO_BACKUP] = enabled }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val MAINTENANCE_REMINDER = booleanPreferencesKey("maintenance_reminder_enabled")
        val INSPECTION_REMINDER = booleanPreferencesKey("inspection_reminder_enabled")
        val MILEAGE_REMINDER = booleanPreferencesKey("mileage_reminder_enabled")
        val USAGE_STATS = booleanPreferencesKey("usage_stats_enabled")
        val CLOUD_AUTO_BACKUP = booleanPreferencesKey("cloud_auto_backup_enabled")
    }
}
