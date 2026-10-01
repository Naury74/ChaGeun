package com.naury.chageun.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.ThemeMode
import com.naury.chageun.core.model.UserSettings
import com.naury.chageun.core.testing.FakeSettingsRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2000dp")
class SettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun viewModel_persistsThemeAndReminderChoice() = runTest {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.setThemeMode(ThemeMode.Dark)
        viewModel.setMaintenanceReminderEnabled(false)

        assertThat(
            repository.settings.first(),
        ).isEqualTo(UserSettings(ThemeMode.Dark, isMaintenanceReminderEnabled = false))
    }

    @Test
    fun screen_selectsThemeAndTogglesReminders() {
        var settings by mutableStateOf(UserSettings())
        composeRule.setContent {
            ChageunTheme {
                SettingsScreen(
                    settings = settings,
                    versionName = "0.1.0",
                    onBack = {},
                    onThemeSelected = { settings = settings.copy(themeMode = it) },
                    onRemindersChanged = { settings = settings.copy(isMaintenanceReminderEnabled = it) },
                    onOpenSystemNotifications = {},
                )
            }
        }

        composeRule.onNodeWithText("Dark").performClick()
        composeRule.onNodeWithText("Dark").assertIsSelected()
        composeRule.onNode(isToggleable()).performClick()
        composeRule.onNode(isToggleable()).assertIsOff()
        composeRule.onNodeWithText("Version 0.1.0").assertExists()
    }
}
