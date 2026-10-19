package com.naury.chageun.feature.account

import android.text.format.Formatter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.ChageunToasts
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.backup.ImportPreview
import com.naury.chageun.core.domain.backup.LocalDataSummary
import com.naury.chageun.core.domain.cloudbackup.CloudBackup
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import com.naury.chageun.core.domain.cloudbackup.CloudBackupRepository
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.ChageunDialog
import com.naury.chageun.core.ui.DialogAction
import com.naury.chageun.core.ui.DialogActionStyle
import com.naury.chageun.core.ui.DialogInfoCard
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.ToggleListRow
import com.naury.kinetica.ui.KLoadingIndicator
import com.naury.kinetica.ui.KLoadingStyle
import com.naury.kinetica.ui.KStatus
import com.naury.kinetica.ui.KStepIndicator
import com.naury.kinetica.ui.KStepStyle
import com.naury.kinetica.ui.KVariant
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class CloudBackupActions(
    val onBackUpNow: () -> Unit = {},
    val onAutoBackupChange: (Boolean) -> Unit = {},
    val onSelect: (CloudBackup?) -> Unit = {},
    val onRestore: (CloudBackup) -> Unit = {},
    val onConfirmRestore: () -> Unit = {},
    val onCloseDialog: () -> Unit = {},
    val onRequestDelete: (CloudBackup) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
)

/** AC05 지금 백업과 AC06 백업 목록. 목록은 최대 다섯 개라 따로 화면을 두지 않는다. */
@Composable
internal fun CloudBackupSection(state: CloudBackupUiState, actions: CloudBackupActions) {
    CardGroup(stringResource(R.string.account_section_backup)) {
        ListRow(
            icon = Icons.Filled.CloudUpload,
            title = stringResource(R.string.account_backup_now),
            body = when {
                state.isBackingUp -> stringResource(R.string.account_backup_in_progress)
                state.lastBackup != null ->
                    stringResource(
                        R.string.account_backup_last,
                        formatDateTime(checkNotNull(state.lastBackup).createdAt),
                    )
                state.isLoaded -> stringResource(R.string.account_backup_never)
                else -> null
            },
            tone = ChageunTheme.colors.good,
            onClick = actions.onBackUpNow,
            enabled = !state.isWorking,
            trailing = if (state.isBackingUp) {
                { KLoadingIndicator(size = ROW_LOADING_SIZE, style = KLoadingStyle.Dots) }
            } else {
                null
            },
        )
        GroupDivider()
        ToggleListRow(
            icon = Icons.Filled.Schedule,
            title = stringResource(R.string.account_auto_backup),
            body = stringResource(R.string.account_auto_backup_body),
            checked = state.isAutoBackupEnabled,
            onCheckedChange = actions.onAutoBackupChange,
        )
    }
    if (state.backups.isNotEmpty()) {
        CardGroup(stringResource(R.string.account_backups_title, CloudBackupRepository.MAX_BACKUPS)) {
            state.backups.forEachIndexed { index, backup ->
                if (index > 0) GroupDivider()
                ListRow(
                    icon = Icons.Filled.History,
                    title = formatDateTime(backup.createdAt),
                    body = backupDetails(backup),
                    onClick = { actions.onSelect(backup) },
                    enabled = !state.isWorking,
                )
            }
        }
    }
}

/**
 * 새 기기 온보딩에서 들어온 복원 모드. 이 기기에는 아직 백업할 데이터가 없으므로 지금 백업은 숨기고,
 * 백업을 누르면 바로 복원 미리보기로 간다.
 */
@Composable
internal fun RestorePicker(state: CloudBackupUiState, actions: CloudBackupActions, onStartFresh: () -> Unit) {
    when {
        !state.isLoaded -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            KLoadingIndicator(size = LOADING_SIZE)
        }
        state.backups.isEmpty() -> {
            CardGroup(title = null) {
                ListRow(
                    icon = Icons.Filled.CloudOff,
                    title = stringResource(R.string.account_restore_empty_title),
                    body = stringResource(R.string.account_restore_empty_body),
                )
            }
            AccountPrimaryButton(
                text = stringResource(R.string.account_restore_start_fresh),
                onClick = onStartFresh,
                variant = KVariant.Tonal,
            )
        }
        else -> CardGroup(stringResource(R.string.account_restore_pick)) {
            state.backups.forEachIndexed { index, backup ->
                if (index > 0) GroupDivider()
                ListRow(
                    icon = Icons.Filled.History,
                    title = formatDateTime(backup.createdAt),
                    body = backupDetails(backup),
                    onClick = { actions.onRestore(backup) },
                    enabled = !state.isWorking,
                )
            }
        }
    }
}

/** 고른 백업의 작업 창, 삭제 확인, 복원 진행·확인 창. 상태는 ViewModel에 있어 폴드를 접고 펴도 그대로다. */
@Composable
internal fun CloudBackupDialogs(state: CloudBackupUiState, actions: CloudBackupActions) {
    state.selected?.let { backup ->
        ChageunDialog(
            title = formatDateTime(backup.createdAt),
            message = backupDetails(backup),
            icon = Icons.Filled.History,
            onDismiss = { actions.onSelect(null) },
            actions = listOf(
                DialogAction(stringResource(R.string.account_backup_restore)) { actions.onRestore(backup) },
                DialogAction(stringResource(R.string.account_backup_delete), DialogActionStyle.Destructive) {
                    actions.onRequestDelete(backup)
                },
                DialogAction(stringResource(R.string.account_cancel), DialogActionStyle.Secondary) {
                    actions.onSelect(null)
                },
            ),
        )
    }
    state.pendingDelete?.let {
        ChageunDialog(
            title = stringResource(R.string.account_backup_delete_title),
            message = stringResource(R.string.account_backup_delete_body),
            icon = Icons.Filled.DeleteOutline,
            tone = ChageunTheme.colors.critical,
            onDismiss = actions.onCloseDialog,
            actions = listOf(
                DialogAction(
                    stringResource(R.string.account_backup_delete_confirm),
                    DialogActionStyle.Destructive,
                    onClick = actions.onConfirmDelete,
                ),
                DialogAction(
                    stringResource(R.string.account_cancel),
                    DialogActionStyle.Secondary,
                    onClick = actions.onCloseDialog,
                ),
            ),
        )
    }
    when (val step = state.restore) {
        is RestoreStep.Downloading -> ProgressDialog(stringResource(R.string.account_restore_downloading), step = 0)
        is RestoreStep.Restoring -> ProgressDialog(stringResource(R.string.account_restore_restoring), step = 1)
        is RestoreStep.Confirming -> RestoreConfirmDialog(
            preview = step.preview,
            onConfirm = actions.onConfirmRestore,
            onDismiss = actions.onCloseDialog,
        )
        null -> Unit
    }
}

/** 백업과 지금 휴대폰의 데이터를 나란히 보여 주고 전체 교체를 확인받는다. 드라이브와 파일 복원이 함께 쓴다. */
@Composable
internal fun RestoreConfirmDialog(preview: ImportPreview.Ready, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ChageunDialog(
        title = stringResource(R.string.account_restore_title),
        message = stringResource(R.string.account_restore_body),
        icon = Icons.Filled.SettingsBackupRestore,
        onDismiss = onDismiss,
        actions = listOf(
            DialogAction(stringResource(R.string.account_restore_confirm), onClick = onConfirm),
            DialogAction(stringResource(R.string.account_cancel), DialogActionStyle.Secondary, onClick = onDismiss),
        ),
    ) {
        DialogInfoCard(Modifier.fillMaxWidth(), stringResource(R.string.account_restore_incoming)) {
            Text(summaryLine(preview.incoming))
        }
        DialogInfoCard(Modifier.fillMaxWidth(), stringResource(R.string.account_restore_current)) {
            Text(summaryLine(preview.current))
        }
    }
}

/**
 * 내려받기·교체 중에는 닫을 수 없다. 교체가 반쯤 된 상태로 화면을 떠나지 않게 한다.
 * 드라이브 복원은 내려받기([step] 0)와 교체(1) 두 단계라 단계 막대로 어디까지 왔는지 보여 준다.
 * 파일 복원은 교체 한 단계뿐이라 [step]을 넘기지 않는다.
 */
@Composable
internal fun ProgressDialog(message: String, step: Int? = null) {
    ChageunDialog(title = message, onDismiss = {}, actions = emptyList(), dismissible = false) {
        KLoadingIndicator(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            size = LOADING_SIZE,
            label = message,
        )
        if (step != null) {
            KStepIndicator(
                steps = listOf(
                    stringResource(R.string.account_restore_step_download),
                    stringResource(R.string.account_restore_step_replace),
                ),
                current = step,
                style = KStepStyle.Bar,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun backupDetails(backup: CloudBackup): String {
    val context = LocalContext.current
    val counts = listOf(
        pluralStringResource(R.plurals.account_count_records, backup.recordCount, backup.recordCount),
        pluralStringResource(R.plurals.account_count_photos, backup.photoCount, backup.photoCount),
    ).joinToString(" · ")
    return stringResource(
        R.string.account_backup_details,
        backup.vehicleLabel ?: backup.deviceModel ?: stringResource(R.string.account_backup_unknown_device),
        counts,
        Formatter.formatShortFileSize(context, backup.sizeBytes),
    )
}

@Composable
private fun summaryLine(summary: LocalDataSummary): String = listOf(
    pluralStringResource(R.plurals.account_count_vehicles, summary.vehicles, summary.vehicles),
    pluralStringResource(R.plurals.account_count_records, summary.records, summary.records),
    pluralStringResource(R.plurals.account_count_photos, summary.photos, summary.photos),
).joinToString(" · ")

private fun formatDateTime(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .format(instant.atZone(ZoneId.systemDefault()))

@Composable
internal fun backupNoticeMessage(notice: BackupNotice): String = stringResource(
    when (notice) {
        BackupNotice.BackedUp -> R.string.account_notice_backed_up
        BackupNotice.Restored -> R.string.account_notice_restored
        BackupNotice.Deleted -> R.string.account_notice_backup_deleted
        BackupNotice.NeedsUpdate -> R.string.account_notice_needs_update
        BackupNotice.RestoreFailed -> R.string.account_notice_restore_failed
        is BackupNotice.Failed -> when (notice.error) {
            CloudBackupError.Network -> R.string.account_error_network
            CloudBackupError.NoData -> R.string.account_backup_error_no_data
            CloudBackupError.TooLarge -> R.string.account_backup_error_too_large
            CloudBackupError.StorageFull -> R.string.drive_error_storage_full
            CloudBackupError.NotConnected -> R.string.drive_error_not_connected
            CloudBackupError.Unavailable -> R.string.drive_error_unavailable
            CloudBackupError.Unknown -> R.string.account_error_unknown
        }
    },
)

private val ROW_LOADING_SIZE = 28.dp
private val LOADING_SIZE = 48.dp

/** 백업·복원·삭제가 끝나면 성공, 실패하면 오류 토스트를 띄운다. 새 버전 백업 안내는 경고다. */
internal fun showBackupNotice(notice: BackupNotice, text: String) {
    when (notice) {
        BackupNotice.BackedUp, BackupNotice.Restored, BackupNotice.Deleted -> ChageunToasts.success(text)
        BackupNotice.NeedsUpdate -> ChageunToasts.show(text, KStatus.Warning)
        BackupNotice.RestoreFailed, is BackupNotice.Failed -> ChageunToasts.error(text)
    }
}
