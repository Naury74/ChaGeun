package com.naury.chageun.feature.account

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.ChageunToasts
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.kinetica.ui.KTone

@Composable
fun DeleteAccountRoute(onBack: () -> Unit, onDeleted: () -> Unit, viewModel: DeleteAccountViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val doneText = stringResource(R.string.account_delete_done)
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            // 삭제 뒤 돌아가는 계정 화면은 로그인 전 모습이라 따로 알린다.
            ChageunToasts.success(doneText)
            onDeleted()
        }
    }
    DeleteAccountScreen(
        uiState = uiState,
        onBack = onBack,
        onPasswordChange = viewModel::setPassword,
        onDelete = { viewModel.delete(context) },
    )
}

/** AC07. 지워지는 것과 남는 것을 먼저 보여 주고, 본인 확인을 거쳐 삭제한다. */
@Composable
fun DeleteAccountScreen(
    uiState: DeleteAccountUiState,
    onBack: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    LargeTitleScaffold(
        title = stringResource(R.string.account_delete_title),
        modifier = modifier,
        navigationIcon = { BackButton(onBack) },
    ) { padding ->
        CenteredColumn(Modifier.padding(padding)) {
            CardGroup(stringResource(R.string.account_delete_removed)) {
                ListRow(
                    icon = Icons.Filled.AccountCircle,
                    title = uiState.user?.email ?: uiState.user?.displayName.orEmpty(),
                    body = stringResource(R.string.account_delete_removed_account),
                    tone = ChageunTheme.colors.critical,
                )
            }
            CardGroup(stringResource(R.string.account_delete_kept)) {
                ListRow(
                    icon = Icons.Filled.DirectionsCar,
                    title = stringResource(R.string.account_delete_kept_data),
                    body = stringResource(R.string.account_delete_kept_body),
                    tone = ChageunTheme.colors.good,
                )
                GroupDivider()
                ListRow(
                    icon = Icons.Filled.AddToDrive,
                    title = stringResource(R.string.account_delete_kept_drive),
                    body = stringResource(R.string.account_delete_kept_drive_body),
                    tone = ChageunTheme.colors.good,
                )
            }
            if (uiState.confirmsWithPassword) {
                PasswordField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    label = stringResource(R.string.account_delete_password),
                    isNewPassword = false,
                    error = uiState.error?.let { authErrorMessage(it) },
                    supporting = null,
                    imeAction = ImeAction.Done,
                    onDone = { focusManager.clearFocus() },
                )
            } else {
                Text(
                    stringResource(R.string.account_delete_google_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // 비밀번호 칸이 있으면 오류는 칸 아래에 보인다. Google 계정은 칸이 없어 상자로 알린다.
            if (!uiState.confirmsWithPassword) {
                uiState.error?.let { AccountErrorAlert(authErrorMessage(it)) }
            }
            AccountPrimaryButton(
                text = stringResource(R.string.account_delete_confirm),
                onClick = onDelete,
                enabled = uiState.canDelete || uiState.isBusy,
                loading = uiState.isBusy,
                tone = KTone.Destructive,
            )
        }
    }
}
