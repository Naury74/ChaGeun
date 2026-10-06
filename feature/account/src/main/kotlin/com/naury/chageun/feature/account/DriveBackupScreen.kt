package com.naury.chageun.feature.account

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow

/**
 * 사용자 본인 Google 드라이브 백업(ADR-006). [isRestoreMode]는 새 기기 온보딩에서 들어온 경우로,
 * 지금 백업은 숨기고 고른 백업을 바로 복원한다.
 */
@Composable
fun DriveBackupRoute(
    onBack: () -> Unit,
    isRestoreMode: Boolean = false,
    connectionViewModel: DriveConnectionViewModel = hiltViewModel(),
    backupViewModel: CloudBackupViewModel = hiltViewModel(),
) {
    val connection by connectionViewModel.uiState.collectAsStateWithLifecycle()
    val backup by backupViewModel.uiState.collectAsStateWithLifecycle()
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        connectionViewModel.onConsentResult(if (it.resultCode == Activity.RESULT_OK) it.data else null)
    }
    LaunchedEffect(connection.consent) {
        connection.consent?.let {
            consentLauncher.launch(IntentSenderRequest.Builder(it).build())
            connectionViewModel.onConsentShown()
        }
    }
    val actions = CloudBackupActions(
        onBackUpNow = backupViewModel::backUpNow,
        onAutoBackupChange = backupViewModel::setAutoBackupEnabled,
        onSelect = backupViewModel::select,
        onRestore = backupViewModel::startRestore,
        onConfirmRestore = backupViewModel::confirmRestore,
        onCloseDialog = backupViewModel::closeDialog,
        onRequestDelete = backupViewModel::requestDelete,
        onConfirmDelete = backupViewModel::confirmDelete,
    )
    DriveBackupScreen(
        connection = connection,
        backup = backup,
        actions = actions,
        onBack = onBack,
        onConnect = connectionViewModel::connect,
        onDisconnect = connectionViewModel::disconnect,
        onConnectionFailureShown = connectionViewModel::dismissFailure,
        onBackupNoticeShown = backupViewModel::dismissNotice,
        isRestoreMode = isRestoreMode,
    )
    CloudBackupDialogs(backup, actions)
}

@Composable
fun DriveBackupScreen(
    connection: DriveConnectionUiState,
    backup: CloudBackupUiState,
    actions: CloudBackupActions,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
    onConnectionFailureShown: () -> Unit = {},
    onBackupNoticeShown: () -> Unit = {},
    isRestoreMode: Boolean = false,
) {
    val snackbar = remember { SnackbarHostState() }
    val failureText = stringResource(R.string.drive_connect_failed)
    LaunchedEffect(connection.hasFailed) {
        if (connection.hasFailed) {
            snackbar.showSnackbar(failureText)
            onConnectionFailureShown()
        }
    }
    val noticeText = backup.notice?.let { backupNoticeMessage(it) }
    LaunchedEffect(backup.notice) {
        if (noticeText != null) {
            snackbar.showSnackbar(noticeText)
            onBackupNoticeShown()
        }
    }
    Box(modifier.fillMaxSize()) {
        LargeTitleScaffold(
            title = stringResource(if (isRestoreMode) R.string.account_restore_mode_title else R.string.drive_title),
            navigationIcon = { BackButton(onBack) },
        ) { padding ->
            CenteredColumn(Modifier.padding(padding)) {
                when {
                    !connection.isConnected -> DriveIntro(isConnecting = connection.isConnecting, onConnect = onConnect)
                    isRestoreMode -> RestorePicker(backup, actions, onStartFresh = onBack)
                    else -> {
                        CloudBackupSection(backup, actions)
                        CardGroup(title = null) {
                            ListRow(
                                icon = Icons.Filled.LinkOff,
                                title = stringResource(R.string.drive_disconnect),
                                body = stringResource(R.string.drive_disconnect_body),
                                onClick = onDisconnect,
                                enabled = !backup.isWorking,
                                trailing = {},
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

/** 연결 전 안내. 어디에 저장되고 누가 볼 수 있는지 먼저 알려 준다. */
@Composable
private fun ColumnScope.DriveIntro(isConnecting: Boolean, onConnect: () -> Unit) {
    val tone = ChageunTheme.colors.good
    Box(
        Modifier
            .size(72.dp)
            .background(tone.container, CircleShape)
            .align(Alignment.CenterHorizontally),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.AddToDrive, contentDescription = null, tint = tone.content, modifier = Modifier.size(36.dp))
    }
    Text(
        stringResource(R.string.drive_intro_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
    )
    CardGroup(title = null) {
        ListRow(Icons.Filled.FolderOff, stringResource(R.string.drive_intro_hidden_folder), tone = tone)
        GroupDivider()
        ListRow(Icons.Filled.Lock, stringResource(R.string.drive_intro_private), tone = ChageunTheme.colors.upcoming)
        GroupDivider()
        ListRow(Icons.Filled.Storage, stringResource(R.string.drive_intro_storage))
    }
    GoogleButton(
        onClick = onConnect,
        enabled = true,
        isBusy = isConnecting,
        label = stringResource(R.string.drive_connect),
    )
    Text(
        stringResource(R.string.drive_intro_zip_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
