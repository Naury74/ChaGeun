package com.naury.chageun.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
    private val repository = PreferencesSettingsRepository(
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { File(directory, "test.preferences_pb") },
    )

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
}
