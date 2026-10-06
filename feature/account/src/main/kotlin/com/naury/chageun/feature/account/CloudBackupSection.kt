package com.naury.chageun.feature.account

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.backup.LocalDataSummary
import com.naury.chageun.core.domain.cloudbackup.CloudBackup
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import com.naury.chageun.core.domain.cloudbackup.CloudBackupRepository
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class CloudBackupActions(
    val onBackUpNow: () -> Unit = {},
    val onSelect: (CloudBackup?) -> Unit = {},
    val onRestore: (CloudBackup) -> Unit = {},
    val onConfirmRestore: () -> Unit = {},
    val onCancelRestore: () -> Unit = {},
    val onRequestDelete: (CloudBackup) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onCancelDelete: () -> Unit = {},
)

/** AC05 지금 백업과 AC06 백업 목록. 목록은 최대 다섯 개라 따로 화면을 두지 않는다. */
@Composable
internal fun CloudBackupSection(state: CloudBackupUiState, canBackUp: Boolean, actions: CloudBackupActions) {
    CardGroup(stringResource(R.string.account_section_backup)) {
        ListRow(
            icon = Icons.Filled.CloudUpload,
            title = stringResource(R.string.account_backup_now),
            body = when {
                !canBackUp -> stringResource(R.string.account_backup_needs_verification)
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
            enabled = canBackUp && !state.isWorking,
            trailing = if (state.isBackingUp) {
                { CircularProgressIndicator(Modifier.size(PROGRESS_SIZE), strokeWidth = 2.dp) }
            } else {
                null
            },
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

/** 고른 백업의 작업 창, 삭제 확인, 복원 진행·확인 창. 상태는 ViewModel에 있어 폴드를 접고 펴도 그대로다. */
@Composable
internal fun CloudBackupDialogs(state: CloudBackupUiState, actions: CloudBackupActions) {
    state.selected?.let { backup ->
        AlertDialog(
            onDismissRequest = { actions.onSelect(null) },
            title = { Text(formatDateTime(backup.createdAt)) },
            text = { Text(backupDetails(backup)) },
            confirmButton = {
                TextButton(onClick = { actions.onRestore(backup) }) {
                    Text(stringResource(R.string.account_backup_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = { actions.onRequestDelete(backup) }) {
                    Text(stringResource(R.string.account_backup_delete), color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
    state.pendingDelete?.let {
        AlertDialog(
            onDismissRequest = actions.onCancelDelete,
            title = { Text(stringResource(R.string.account_backup_delete_title)) },
            text = { Text(stringResource(R.string.account_backup_delete_body)) },
            confirmButton = {
                TextButton(onClick = actions.onConfirmDelete) {
                    Text(
                        stringResource(R.string.account_backup_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = actions.onCancelDelete) { Text(stringResource(R.string.account_cancel)) }
            },
        )
    }
    when (val step = state.restore) {
        is RestoreStep.Downloading -> ProgressDialog(stringResource(R.string.account_restore_downloading))
        is RestoreStep.Restoring -> ProgressDialog(stringResource(R.string.account_restore_restoring))
        is RestoreStep.Confirming -> AlertDialog(
            onDismissRequest = actions.onCancelRestore,
            title = { Text(stringResource(R.string.account_restore_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                    Text(stringResource(R.string.account_restore_incoming), style = MaterialTheme.typography.titleSmall)
                    Text(summaryLine(step.preview.incoming))
                    Text(stringResource(R.string.account_restore_current), style = MaterialTheme.typography.titleSmall)
                    Text(summaryLine(step.preview.current))
                    Text(stringResource(R.string.account_restore_body))
                }
            },
            confirmButton = {
                TextButton(onClick = actions.onConfirmRestore) {
                    Text(stringResource(R.string.account_restore_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = actions.onCancelRestore) { Text(stringResource(R.string.account_cancel)) }
            },
        )
        null -> Unit
    }
}

/** 내려받기·교체 중에는 닫을 수 없다. 교체가 반쯤 된 상태로 화면을 떠나지 않게 한다. */
@Composable
private fun ProgressDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        confirmButton = {},
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
            ) {
                CircularProgressIndicator(Modifier.size(PROGRESS_SIZE * 2))
                Text(message)
            }
        },
    )
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
            CloudBackupError.EmailNotVerified -> R.string.account_backup_needs_verification
            CloudBackupError.NotSignedIn,
            CloudBackupError.Unavailable,
            -> R.string.account_backup_error_unavailable
            CloudBackupError.Unknown -> R.string.account_error_unknown
        }
    },
)

private val PROGRESS_SIZE = 20.dp
