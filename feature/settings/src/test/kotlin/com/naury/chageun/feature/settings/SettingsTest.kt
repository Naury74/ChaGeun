package com.naury.chageun.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.backup.ImportPreview
import com.naury.chageun.core.domain.backup.LocalDataSummary
import com.naury.chageun.core.model.ThemeMode
import com.naury.chageun.core.model.UserSettings
import com.naury.chageun.core.testing.FakeBackupRepository
import com.naury.chageun.core.testing.FakeSettingsRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import com.naury.chageun.core.uitesting.assertNoClippedText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
        val viewModel = SettingsViewModel(repository, FakeBackupRepository())

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

    @Test
    fun deleteAll_showsSummaryFirst_thenDeletesOnConfirm() = runTest {
        val backup = FakeBackupRepository().apply { summary = LocalDataSummary(1, 12, 3) }
        val viewModel = SettingsViewModel(FakeSettingsRepository(), backup)

        viewModel.requestDeleteAll()
        assertThat(viewModel.dataState.value.pendingDeletion?.records).isEqualTo(12)
        assertThat(backup.deleteCount).isEqualTo(0)

        viewModel.confirmDeleteAll()

        assertThat(backup.deleteCount).isEqualTo(1)
        assertThat(viewModel.dataState.value.pendingDeletion).isNull()
    }

    @Test
    fun export_reportsFailure() = runTest {
        val backup = FakeBackupRepository().apply { exportSucceeds = false }
        val viewModel = SettingsViewModel(FakeSettingsRepository(), backup)

        viewModel.export("content://downloads/backup.zip")

        assertThat(backup.exportedTo).containsExactly("content://downloads/backup.zip")
        assertThat(viewModel.dataState.value.message).isEqualTo(DataMessage.ExportFailed)
    }

    @Test
    fun deleteDialog_listsWhatWillBeRemoved() {
        composeRule.setContent {
            ChageunTheme {
                DeleteAllDialog(LocalDataSummary(1, 12, 3), onConfirm = {}, onDismiss = {})
            }
        }

        composeRule.onNodeWithText("12 records").assertExists()
        composeRule.onNodeWithText("3 photos and receipts").assertExists()
    }

    @Test
    fun import_previewsFirst_thenReplacesOnConfirm() = runTest {
        val preview = ImportPreview.Ready(incoming = LocalDataSummary(1, 20, 4), current = LocalDataSummary(1, 12, 3))
        val backup = FakeBackupRepository().apply { importPreview = preview }
        val viewModel = SettingsViewModel(FakeSettingsRepository(), backup)

        viewModel.previewImport("content://downloads/backup.zip")
        assertThat(viewModel.dataState.value.pendingImport?.preview).isEqualTo(preview)
        assertThat(backup.importedFrom).isEmpty()

        viewModel.confirmImport()

        assertThat(backup.importedFrom).containsExactly("content://downloads/backup.zip")
        assertThat(viewModel.dataState.value.pendingImport).isNull()
        assertThat(viewModel.dataState.value.message).isEqualTo(DataMessage.ImportDone)
    }

    @Test
    fun import_unsupportedVersion_neverReplaces() = runTest {
        val backup = FakeBackupRepository().apply { importPreview = ImportPreview.UnsupportedVersion(2) }
        val viewModel = SettingsViewModel(FakeSettingsRepository(), backup)

        viewModel.previewImport("content://downloads/backup.zip")
        viewModel.confirmImport()

        assertThat(viewModel.dataState.value.message).isEqualTo(DataMessage.ImportUnsupported)
        assertThat(backup.importedFrom).isEmpty()
    }

    @Test
    fun importDialog_showsIncomingAndCurrentCounts() {
        composeRule.setContent {
            ChageunTheme {
                ImportDialog(
                    ImportPreview.Ready(incoming = LocalDataSummary(1, 20, 4), current = LocalDataSummary(1, 12, 3)),
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("20 records").assertExists()
        composeRule.onNodeWithText("12 records").assertExists()
        composeRule.onNodeWithText("Replace").assertExists()
    }

    @Test
    fun licenses_parsesGeneratedMetadata_sortedByName() {
        val json = """
            {
              "libraries": [
                {"uniqueId": "b:zeta", "artifactVersion": "2.0", "name": "Zeta", "licenses": ["Apache-2.0"]},
                {"uniqueId": "a:alpha", "artifactVersion": "1.0", "name": "alpha", "licenses": ["Apache-2.0"]}
              ],
              "licenses": {
                "Apache-2.0": {"name": "Apache License 2.0", "hash": "Apache-2.0", "content": "Licensed under..."}
              }
            }
        """.trimIndent()

        val libraries = parseOpenSourceLibraries(json)

        assertThat(libraries.map { it.name }).containsExactly("alpha", "Zeta").inOrder()
        assertThat(libraries.first().licenses).containsExactly("Apache License 2.0")
        assertThat(libraries.first().licenseText).isEqualTo("Licensed under...")
    }

    @Test
    fun licensesScreen_opensLicenseText_andBackReturnsToList() {
        var closed = false
        composeRule.setContent {
            ChageunTheme {
                OpenSourceLicensesScreen(
                    libraries = listOf(OpenSourceLibrary("a:alpha", "Alpha", "1.0", listOf("MIT License"), "MIT text")),
                    onBack = { closed = true },
                )
            }
        }

        composeRule.onNodeWithText("1.0 · MIT License").assertExists()
        composeRule.onNodeWithText("Alpha").performClick()
        composeRule.onNodeWithText("MIT text").assertExists()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("1.0 · MIT License").assertExists()
        assertThat(closed).isFalse()
    }

    @Test
    fun privacyNotice_statesNothingIsSentAutomatically() {
        composeRule.setContent { ChageunTheme { PrivacyNoticeScreen(onBack = {}) } }

        composeRule.onNodeWithText("What leaves the device").assertExists()
        composeRule.onNodeWithText("Nothing is sent automatically", substring = true).assertExists()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun largeFont_keepsSettingsAndDialogsVisible() {
        var dialog by mutableStateOf(false)
        composeRule.setContent {
            ChageunTheme {
                SettingsScreen(
                    settings = UserSettings(),
                    versionName = "0.1.0",
                    onBack = {},
                    onThemeSelected = {},
                    onRemindersChanged = {},
                    onOpenSystemNotifications = {},
                    dataSection = { DataSection(DataUiState(), onExport = {}, onImport = {}, onRequestDelete = {}) },
                )
                if (dialog) {
                    ImportDialog(
                        ImportPreview.Ready(LocalDataSummary(1, 20, 4), LocalDataSummary(1, 12, 3)),
                        onConfirm = {},
                        onDismiss = {},
                    )
                }
            }
        }
        composeRule.assertNoClippedText()

        dialog = true
        composeRule.waitForIdle()

        composeRule.assertNoClippedText()
    }
}
