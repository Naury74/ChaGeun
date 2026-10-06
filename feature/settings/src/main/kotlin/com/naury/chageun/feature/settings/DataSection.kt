package com.naury.chageun.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.backup.ImportPreview
import com.naury.chageun.core.domain.backup.LocalDataSummary
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow

@Composable
internal fun DataSection(
    state: DataUiState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onRequestDelete: () -> Unit,
    onShare: () -> Unit = {},
    onOpenDriveBackup: () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        CardGroup(stringResource(R.string.settings_section_data)) {
            ListRow(
                icon = Icons.Filled.AddToDrive,
                title = stringResource(R.string.settings_drive_backup),
                body = stringResource(R.string.settings_drive_backup_body),
                tone = ChageunTheme.colors.good,
                onClick = onOpenDriveBackup,
                enabled = !state.isWorking,
            )
            GroupDivider()
            ListRow(
                icon = Icons.Filled.Share,
                title = stringResource(R.string.settings_share),
                body = stringResource(R.string.settings_share_body),
                tone = ChageunTheme.colors.upcoming,
                onClick = onShare,
                enabled = !state.isWorking,
            )
            GroupDivider()
            ListRow(
                icon = Icons.Filled.Upload,
                title = stringResource(R.string.settings_export),
                body = stringResource(R.string.settings_export_body),
                onClick = onExport,
                enabled = !state.isWorking,
            )
            GroupDivider()
            ListRow(
                icon = Icons.Filled.Download,
                title = stringResource(R.string.settings_import),
                onClick = onImport,
                enabled = !state.isWorking,
            )
            GroupDivider()
            ListRow(
                icon = Icons.Filled.DeleteForever,
                title = stringResource(R.string.settings_delete_all),
                tone = ChageunTheme.colors.critical,
                titleColor = MaterialTheme.colorScheme.error,
                onClick = onRequestDelete,
                enabled = !state.isWorking,
            )
        }
        state.message?.let { message ->
            Text(
                stringResource(message.textRes),
                style = MaterialTheme.typography.bodySmall,
                color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun DeleteAllDialog(summary: LocalDataSummary, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                SummaryLines(summary)
                Text(stringResource(R.string.settings_delete_body))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_delete_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
    )
}

@Composable
internal fun ImportDialog(preview: ImportPreview.Ready, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_import_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                Text(stringResource(R.string.settings_import_incoming), style = MaterialTheme.typography.titleSmall)
                SummaryLines(preview.incoming)
                Text(stringResource(R.string.settings_import_current), style = MaterialTheme.typography.titleSmall)
                SummaryLines(preview.current)
                Text(stringResource(R.string.settings_import_body))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.settings_import_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
    )
}

@Composable
private fun SummaryLines(summary: LocalDataSummary) {
    Text(pluralStringResource(R.plurals.settings_count_vehicles, summary.vehicles, summary.vehicles))
    Text(pluralStringResource(R.plurals.settings_count_records, summary.records, summary.records))
    Text(pluralStringResource(R.plurals.settings_count_photos, summary.photos, summary.photos))
}

private val DataMessage.textRes: Int
    get() = when (this) {
        DataMessage.ExportDone -> R.string.settings_export_done
        DataMessage.ExportFailed -> R.string.settings_export_failed
        DataMessage.ImportDone -> R.string.settings_import_done
        DataMessage.ImportFailed -> R.string.settings_import_failed
        DataMessage.ImportUnsupported -> R.string.settings_import_unsupported
        DataMessage.ImportInvalid -> R.string.settings_import_invalid
    }

private val DataMessage.isError: Boolean
    get() = this != DataMessage.ExportDone && this != DataMessage.ImportDone
