package com.naury.chageun.feature.account

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow

@Composable
fun DeleteAccountRoute(onBack: () -> Unit, onDeleted: () -> Unit, viewModel: DeleteAccountViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            // 삭제 뒤 돌아가는 계정 화면은 로그인 전 모습이라 따로 알린다.
            Toast.makeText(context, R.string.account_delete_done, Toast.LENGTH_SHORT).show()
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
                    contentType = ContentType.Password,
                    isError = uiState.error != null,
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
            uiState.error?.let { error ->
                Text(
                    authErrorMessage(error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Button(
                onClick = onDelete,
                enabled = uiState.canDelete || uiState.isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = ButtonHeight,
            ) {
                ButtonContent(stringResource(R.string.account_delete_confirm), isBusy = uiState.isBusy)
            }
        }
    }
}
