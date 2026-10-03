package com.naury.chageun.feature.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.ads.LocalAdConsent
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.ThemeMode
import com.naury.chageun.core.model.UserSettings
import com.naury.chageun.core.ui.launchExternal
import com.naury.chageun.core.ui.openUriSafely
import java.time.LocalDate

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenPrivacy: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val adConsent = LocalAdConsent.current
    val isAdPrivacyRequired by adConsent.isPrivacyOptionsRequired.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val dataState by backupViewModel.dataState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember { context.versionName() }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ZIP_MIME_TYPE)) { uri ->
            uri?.let { backupViewModel.export(it.toString()) }
        }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { backupViewModel.previewImport(it.toString()) }
    }
    SettingsScreen(
        settings = settings,
        versionName = version,
        onBack = onBack,
        onThemeSelected = viewModel::setThemeMode,
        onRemindersChanged = viewModel::setMaintenanceReminderEnabled,
        onMileageRemindersChanged = viewModel::setMileageReminderEnabled,
        onUsageStatsChanged = viewModel::setUsageStatsEnabled,
        onOpenSystemNotifications = { context.launchExternal { context.openNotificationSettings() } },
        onOpenLicenses = onOpenLicenses,
        onOpenPrivacy = onOpenPrivacy,
        onOpenPrivacyPolicy = { uriHandler.openUriSafely(context, PRIVACY_POLICY_URL) },
        isAdPrivacyRequired = isAdPrivacyRequired,
        onOpenAdPrivacy = { activity?.let(adConsent::showPrivacyOptions) },
        dataSection = {
            DataSection(
                state = dataState,
                onExport = {
                    context.launchExternal { exportLauncher.launch("chageun-backup-${LocalDate.now()}.zip") }
                },
                onImport = { context.launchExternal { importLauncher.launch(arrayOf(ZIP_MIME_TYPE)) } },
                onRequestDelete = backupViewModel::requestDeleteAll,
            )
        },
    )
    dataState.pendingDeletion?.let { summary ->
        DeleteAllDialog(
            summary,
            onConfirm = backupViewModel::confirmDeleteAll,
            onDismiss = backupViewModel::cancelDeleteAll,
        )
    }
    dataState.pendingImport?.let { pending ->
        ImportDialog(
            pending.preview,
            onConfirm = backupViewModel::confirmImport,
            onDismiss = backupViewModel::cancelImport,
        )
    }
}

@Composable
fun SettingsScreen(
    settings: UserSettings,
    versionName: String,
    onBack: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onRemindersChanged: (Boolean) -> Unit,
    onOpenSystemNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    onMileageRemindersChanged: (Boolean) -> Unit = {},
    onUsageStatsChanged: (Boolean) -> Unit = {},
    onOpenLicenses: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    isAdPrivacyRequired: Boolean = false,
    onOpenAdPrivacy: () -> Unit = {},
    dataSection: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ChageunTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
    ) {
        SettingsTopBar(R.string.settings_title, onBack)
        Column(
            Modifier.widthIn(max = CONTENT_MAX_WIDTH),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.lg),
        ) {
            Section(R.string.settings_section_theme) {
                Column(Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = settings.themeMode == mode, role = Role.RadioButton) {
                                    onThemeSelected(mode)
                                }
                                .padding(vertical = ChageunTheme.spacing.xxs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = settings.themeMode == mode, onClick = null)
                            Text(
                                stringResource(mode.labelRes),
                                modifier = Modifier.padding(start = ChageunTheme.spacing.xs),
                            )
                        }
                    }
                }
            }
            Section(R.string.settings_section_notifications) {
                ToggleRow(
                    titleRes = R.string.settings_maintenance_reminders,
                    bodyRes = R.string.settings_maintenance_reminders_body,
                    checked = settings.isMaintenanceReminderEnabled,
                    onCheckedChange = onRemindersChanged,
                )
                ToggleRow(
                    titleRes = R.string.settings_mileage_reminders,
                    bodyRes = R.string.settings_mileage_reminders_body,
                    checked = settings.isMileageReminderEnabled,
                    onCheckedChange = onMileageRemindersChanged,
                )
                TextButton(onClick = onOpenSystemNotifications) {
                    Text(stringResource(R.string.settings_system_notifications))
                }
            }
            Section(R.string.settings_section_data) { dataSection() }
            Section(R.string.settings_section_privacy) {
                ToggleRow(
                    titleRes = R.string.settings_usage_stats,
                    bodyRes = R.string.settings_usage_stats_body,
                    checked = settings.isUsageStatsEnabled,
                    onCheckedChange = onUsageStatsChanged,
                )
            }
            Section(R.string.settings_section_sources) {
                listOf(
                    R.string.settings_source_user,
                    R.string.settings_source_derived,
                    R.string.settings_source_generic,
                    R.string.settings_source_official,
                ).forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
            }
            Section(R.string.settings_section_about) {
                Text(
                    stringResource(R.string.settings_version, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onOpenPrivacy) { Text(stringResource(R.string.settings_privacy)) }
                TextButton(onClick = onOpenPrivacyPolicy) { Text(stringResource(R.string.settings_privacy_policy)) }
                if (isAdPrivacyRequired) {
                    TextButton(onClick = onOpenAdPrivacy) { Text(stringResource(R.string.settings_ad_privacy)) }
                }
                TextButton(onClick = onOpenLicenses) { Text(stringResource(R.string.settings_licenses)) }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    // 제목을 눌러도 바뀌고, TalkBack에서는 제목·설명·상태를 한 번에 읽도록 행 전체를 스위치로 만든다.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = ChageunTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun Section(@StringRes titleRes: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics {
                heading()
            },
        )
        content()
    }
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.System -> R.string.settings_theme_system
        ThemeMode.Light -> R.string.settings_theme_light
        ThemeMode.Dark -> R.string.settings_theme_dark
    }

private fun Context.versionName(): String = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

private fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private val CONTENT_MAX_WIDTH = 640.dp
private const val ZIP_MIME_TYPE = "application/zip"

// Play 스토어에 등록한 주소와 같아야 한다.
private const val PRIVACY_POLICY_URL = "https://naury74.github.io/chageun/privacy/"
