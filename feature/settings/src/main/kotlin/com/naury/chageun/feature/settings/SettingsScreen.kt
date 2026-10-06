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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.ads.LocalAdConsent
import com.naury.chageun.core.designsystem.component.SegmentedControl
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.ThemeMode
import com.naury.chageun.core.model.UserSettings
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.ChageunLinks
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.ToggleListRow
import com.naury.chageun.core.ui.launchExternal
import com.naury.chageun.core.ui.openUriSafely
import java.time.LocalDate

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAccount: () -> Unit,
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
        onOpenAccount = onOpenAccount,
        onOpenPrivacyPolicy = { uriHandler.openUriSafely(context, ChageunLinks.PRIVACY_POLICY) },
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
    onOpenAccount: () -> Unit = {},
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
            CardGroup(title = null) {
                ListRow(
                    icon = Icons.Filled.AccountCircle,
                    title = stringResource(R.string.settings_account),
                    body = stringResource(R.string.settings_account_body),
                    tone = ChageunTheme.colors.good,
                    onClick = onOpenAccount,
                )
            }
            CardlessSection(R.string.settings_section_theme) {
                SegmentedControl(
                    options = ThemeMode.entries.map { stringResource(it.labelRes) },
                    selectedIndex = ThemeMode.entries.indexOf(settings.themeMode),
                    onSelect = { onThemeSelected(ThemeMode.entries[it]) },
                )
            }
            CardGroup(stringResource(R.string.settings_section_notifications)) {
                ToggleListRow(
                    icon = Icons.Filled.NotificationsActive,
                    title = stringResource(R.string.settings_maintenance_reminders),
                    body = stringResource(R.string.settings_maintenance_reminders_body),
                    checked = settings.isMaintenanceReminderEnabled,
                    onCheckedChange = onRemindersChanged,
                    tone = ChageunTheme.colors.upcoming,
                )
                GroupDivider()
                ToggleListRow(
                    icon = Icons.Filled.Speed,
                    title = stringResource(R.string.settings_mileage_reminders),
                    body = stringResource(R.string.settings_mileage_reminders_body),
                    checked = settings.isMileageReminderEnabled,
                    onCheckedChange = onMileageRemindersChanged,
                )
                GroupDivider()
                ListRow(
                    icon = Icons.Filled.Tune,
                    title = stringResource(R.string.settings_system_notifications),
                    onClick = onOpenSystemNotifications,
                    trailing = { ExternalIcon() },
                )
            }
            dataSection()
            CardGroup(stringResource(R.string.settings_section_privacy)) {
                ToggleListRow(
                    icon = Icons.Filled.BarChart,
                    title = stringResource(R.string.settings_usage_stats),
                    body = stringResource(R.string.settings_usage_stats_body),
                    checked = settings.isUsageStatsEnabled,
                    onCheckedChange = onUsageStatsChanged,
                    tone = ChageunTheme.colors.ai,
                )
            }
            CardGroup(stringResource(R.string.settings_section_sources)) {
                SOURCES.forEachIndexed { index, (icon, res) ->
                    if (index > 0) GroupDivider()
                    ListRow(icon = icon, title = stringResource(res))
                }
            }
            CardGroup(stringResource(R.string.settings_section_about)) {
                ListRow(Icons.Filled.Info, stringResource(R.string.settings_version, versionName))
                GroupDivider()
                ListRow(Icons.Filled.Shield, stringResource(R.string.settings_privacy), onClick = onOpenPrivacy)
                GroupDivider()
                ListRow(
                    Icons.Filled.Policy,
                    stringResource(R.string.settings_privacy_policy),
                    onClick = onOpenPrivacyPolicy,
                    trailing = { ExternalIcon() },
                )
                if (isAdPrivacyRequired) {
                    GroupDivider()
                    ListRow(
                        Icons.Filled.AdsClick,
                        stringResource(R.string.settings_ad_privacy),
                        onClick = onOpenAdPrivacy,
                    )
                }
                GroupDivider()
                ListRow(Icons.Filled.Code, stringResource(R.string.settings_licenses), onClick = onOpenLicenses)
            }
        }
    }
}

/** 앱 밖(시스템 설정, 웹)으로 나간다는 표시. */
@Composable
private fun ExternalIcon() {
    Icon(
        Icons.AutoMirrored.Filled.OpenInNew,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun CardlessSection(@StringRes titleRes: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(top = ChageunTheme.spacing.xs)
                .semantics { heading() },
        )
        content()
    }
}

private val SOURCES = listOf(
    Icons.Filled.Person to R.string.settings_source_user,
    Icons.Filled.Calculate to R.string.settings_source_derived,
    Icons.AutoMirrored.Filled.MenuBook to R.string.settings_source_generic,
    Icons.Filled.Verified to R.string.settings_source_official,
)

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
