package com.naury.chageun.core.domain.settings

import com.naury.chageun.core.model.ThemeMode
import com.naury.chageun.core.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setMaintenanceReminderEnabled(enabled: Boolean)

    suspend fun setMileageReminderEnabled(enabled: Boolean)
}
