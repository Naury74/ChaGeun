package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.settings.SettingsRepository
import com.naury.chageun.core.model.ThemeMode
import com.naury.chageun.core.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository : SettingsRepository {
    override val settings = MutableStateFlow(UserSettings())

    override suspend fun setThemeMode(mode: ThemeMode) = settings.update { it.copy(themeMode = mode) }

    override suspend fun setMaintenanceReminderEnabled(enabled: Boolean) =
        settings.update { it.copy(isMaintenanceReminderEnabled = enabled) }

    override suspend fun setInspectionReminderEnabled(enabled: Boolean) =
        settings.update { it.copy(isInspectionReminderEnabled = enabled) }

    override suspend fun setMileageReminderEnabled(enabled: Boolean) =
        settings.update { it.copy(isMileageReminderEnabled = enabled) }

    override suspend fun setUsageStatsEnabled(enabled: Boolean) =
        settings.update { it.copy(isUsageStatsEnabled = enabled) }

    override suspend fun setCloudAutoBackupEnabled(enabled: Boolean) =
        settings.update { it.copy(isCloudAutoBackupEnabled = enabled) }
}
