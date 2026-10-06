package com.naury.chageun.feature.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.launchExternal

/** 온보딩 "백업에서 복원하기". ZIP 파일이나 내 Google 드라이브에서 고른다. */
@Composable
fun RestoreStartRoute(onBack: () -> Unit, onOpenDrive: () -> Unit, viewModel: FileRestoreViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.preview(it.toString()) }
    }
    RestoreStartScreen(
        uiState = uiState,
        onBack = onBack,
        onPickFile = { context.launchExternal { picker.launch(ZIP_MIME_TYPES) } },
        onOpenDrive = onOpenDrive,
        onNoticeShown = viewModel::dismissNotice,
    )
    val preview = uiState.preview
    when {
        preview != null -> RestoreConfirmDialog(preview, onConfirm = viewModel::confirm, onDismiss = viewModel::cancel)
        uiState.isWorking -> ProgressDialog(stringResource(R.string.account_restore_restoring))
    }
}

@Composable
fun RestoreStartScreen(
    uiState: FileRestoreUiState,
    onBack: () -> Unit,
    onPickFile: () -> Unit,
    onOpenDrive: () -> Unit,
    modifier: Modifier = Modifier,
    onNoticeShown: () -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    val noticeText = uiState.notice?.let { backupNoticeMessage(it) }
    LaunchedEffect(uiState.notice) {
        if (noticeText != null) {
            snackbar.showSnackbar(noticeText)
            onNoticeShown()
        }
    }
    Box(modifier.fillMaxSize()) {
        LargeTitleScaffold(
            title = stringResource(R.string.account_restore_mode_title),
            navigationIcon = { BackButton(onBack) },
        ) { padding ->
            CenteredColumn(Modifier.padding(padding)) {
                Text(
                    stringResource(R.string.restore_start_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CardGroup(title = null) {
                    ListRow(
                        icon = Icons.Filled.FolderZip,
                        title = stringResource(R.string.restore_start_file),
                        body = stringResource(R.string.restore_start_file_body),
                        tone = ChageunTheme.colors.good,
                        onClick = onPickFile,
                        enabled = !uiState.isWorking,
                    )
                    GroupDivider()
                    ListRow(
                        icon = Icons.Filled.AddToDrive,
                        title = stringResource(R.string.restore_start_drive),
                        body = stringResource(R.string.restore_start_drive_body),
                        tone = ChageunTheme.colors.upcoming,
                        onClick = onOpenDrive,
                        enabled = !uiState.isWorking,
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

/** 메신저나 메일로 받은 파일은 형식이 application/octet-stream으로 붙기도 한다. */
private val ZIP_MIME_TYPES = arrayOf("application/zip", "application/octet-stream")
