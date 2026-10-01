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
import com.naury.chageun.core.domain.backup.LocalDataSummary

@Composable
internal fun DataSection(state: DataUiState, onExport: () -> Unit, onRequestDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Text(
            stringResource(R.string.settings_export_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onExport, enabled = !state.isWorking) {
            Text(stringResource(R.string.settings_export))
        }
        state.exportResult?.let { result ->
            Text(
                stringResource(
                    if (result ==
                        ExportResult.Success
                    ) {
                        R.string.settings_export_done
                    } else {
                        R.string.settings_export_failed
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (result ==
                    ExportResult.Success
                ) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                },
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
                Text(pluralStringResource(R.plurals.settings_delete_vehicles, summary.vehicles, summary.vehicles))
                Text(pluralStringResource(R.plurals.settings_delete_records, summary.records, summary.records))
                Text(pluralStringResource(R.plurals.settings_delete_photos, summary.photos, summary.photos))
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
