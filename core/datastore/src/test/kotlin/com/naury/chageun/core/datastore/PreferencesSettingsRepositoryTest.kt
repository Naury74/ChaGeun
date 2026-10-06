package com.naury.chageun.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.ThemeMode
import com.naury.chageun.core.model.UserSettings
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class PreferencesSettingsRepositoryTest {

    private val directory: File = Files.createTempDirectory("prefs").toFile()
    private val scope = TestScope(UnconfinedTestDispatcher())
    private val dataStore =
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { File(directory, "test.preferences_pb") }
    private val repository = PreferencesSettingsRepository(dataStore)

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun defaultsToSystemThemeAndRemindersOn() = scope.runTest {
        assertThat(repository.settings.first()).isEqualTo(UserSettings())
    }

    @Test
    fun persistsChanges() = scope.runTest {
        repository.setThemeMode(ThemeMode.Dark)
        repository.setMaintenanceReminderEnabled(false)
        repository.setCloudAutoBackupEnabled(true)

        assertThat(
            repository.settings.first(),
        ).isEqualTo(
            UserSettings(ThemeMode.Dark, isMaintenanceReminderEnabled = false, isCloudAutoBackupEnabled = true),
        )
    }

    @Test
    fun turningOffMaintenance_keepsInspectionRemindersOn() = scope.runTest {
        repository.setMaintenanceReminderEnabled(false)

        val settings = repository.settings.first()
        assertThat(settings.isMaintenanceReminderEnabled).isFalse()
        assertThat(settings.isInspectionReminderEnabled).isTrue()
    }

    @Test
    fun usersWhoTurnedOffTheOldCombinedSwitch_keepInspectionOff() = scope.runTest {
        // 정비·검사를 한 스위치로 끄던 버전에서 저장된 값.
        dataStore.edit { it[booleanPreferencesKey("maintenance_reminder_enabled")] = false }

        assertThat(repository.settings.first().isInspectionReminderEnabled).isFalse()

        repository.setMaintenanceReminderEnabled(true)

        assertThat(repository.settings.first().isInspectionReminderEnabled).isFalse()
    }
}
