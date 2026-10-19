package com.naury.chageun.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.backup.ImportPreview
import com.naury.chageun.core.domain.backup.LocalDataSummary
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.ChageunDialog
import com.naury.chageun.core.ui.DialogAction
import com.naury.chageun.core.ui.DialogActionStyle
import com.naury.chageun.core.ui.DialogInfoCard
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.kinetica.ui.KAlert
import com.naury.kinetica.ui.KProgressBar
import com.naury.kinetica.ui.KStatus

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
        // 내보내기·가져오기는 길이를 알 수 없어 흐르는 막대로 진행 중임만 알린다.
        if (state.isWorking) KProgressBar(progress = null, thickness = WORKING_BAR_HEIGHT)
        state.message?.let { message ->
            KAlert(
                message = stringResource(message.textRes),
                status = if (message.isError) KStatus.Error else KStatus.Success,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
internal fun DeleteAllDialog(summary: LocalDataSummary, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ChageunDialog(
        title = stringResource(R.string.settings_delete_title),
        message = stringResource(R.string.settings_delete_body),
        icon = Icons.Filled.DeleteForever,
        tone = ChageunTheme.colors.critical,
        onDismiss = onDismiss,
        actions = listOf(
            DialogAction(
                stringResource(R.string.settings_delete_confirm),
                DialogActionStyle.Destructive,
                onClick = onConfirm,
            ),
            DialogAction(stringResource(R.string.settings_cancel), DialogActionStyle.Secondary, onClick = onDismiss),
        ),
    ) {
        DialogInfoCard(Modifier.fillMaxWidth()) { SummaryLines(summary) }
    }
}

@Composable
internal fun ImportDialog(preview: ImportPreview.Ready, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ChageunDialog(
        title = stringResource(R.string.settings_import_title),
        message = stringResource(R.string.settings_import_body),
        icon = Icons.Filled.Download,
        onDismiss = onDismiss,
        actions = listOf(
            DialogAction(stringResource(R.string.settings_import_confirm), onClick = onConfirm),
            DialogAction(stringResource(R.string.settings_cancel), DialogActionStyle.Secondary, onClick = onDismiss),
        ),
    ) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
            DialogInfoCard(Modifier.weight(1f).fillMaxHeight(), stringResource(R.string.settings_import_incoming)) {
                SummaryLines(preview.incoming)
            }
            DialogInfoCard(Modifier.weight(1f).fillMaxHeight(), stringResource(R.string.settings_import_current)) {
                SummaryLines(preview.current)
            }
        }
    }
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

private val WORKING_BAR_HEIGHT = 4.dp
