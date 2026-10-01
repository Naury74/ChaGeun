package com.naury.chageun.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.backup.ImportPreview
import com.naury.chageun.core.domain.backup.LocalDataSummary

@Composable
internal fun DataSection(state: DataUiState, onExport: () -> Unit, onImport: () -> Unit, onRequestDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Text(
            stringResource(R.string.settings_export_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onExport, enabled = !state.isWorking) {
            Text(stringResource(R.string.settings_export))
        }
        OutlinedButton(onClick = onImport, enabled = !state.isWorking) {
            Text(stringResource(R.string.settings_import))
        }
        state.message?.let { message ->
            Text(
                stringResource(message.textRes),
                style = MaterialTheme.typography.bodySmall,
                color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        TextButton(onClick = onRequestDelete, enabled = !state.isWorking) {
            Text(stringResource(R.string.settings_delete_all), color = MaterialTheme.colorScheme.error)
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
