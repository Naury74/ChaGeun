package com.naury.chageun.feature.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.component.SegmentedControl
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.ChageunLinks
import com.naury.chageun.core.ui.openUriSafely

@Composable
fun EmailAuthRoute(onBack: () -> Unit, onCompleted: () -> Unit, viewModel: EmailAuthViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) onCompleted()
    }
    EmailAuthScreen(
        uiState = uiState,
        actions = EmailAuthActions(
            onBack = onBack,
            onModeSelected = viewModel::setMode,
            onEmailChange = viewModel::setEmail,
            onPasswordChange = viewModel::setPassword,
            onPasswordConfirmChange = viewModel::setPasswordConfirm,
            onPrivacyAgreedChange = viewModel::setPrivacyAgreed,
            onOpenPrivacyPolicy = { uriHandler.openUriSafely(context, ChageunLinks.PRIVACY_POLICY) },
            onSubmit = viewModel::submit,
            onForgotPassword = viewModel::openPasswordReset,
        ),
    )
    uiState.reset?.let { reset ->
        PasswordResetDialog(
            state = reset,
            onEmailChange = viewModel::setResetEmail,
            onSend = viewModel::sendPasswordReset,
            onDismiss = viewModel::closePasswordReset,
        )
    }
}

data class EmailAuthActions(
    val onBack: () -> Unit = {},
    val onModeSelected: (EmailAuthMode) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onPasswordConfirmChange: (String) -> Unit = {},
    val onPrivacyAgreedChange: (Boolean) -> Unit = {},
    val onOpenPrivacyPolicy: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onForgotPassword: () -> Unit = {},
)

/** AC02. 로그인과 가입은 입력 칸이 거의 같아서 한 화면에서 세그먼트로 바꾼다. */
@Composable
fun EmailAuthScreen(uiState: EmailAuthUiState, actions: EmailAuthActions, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    LargeTitleScaffold(
        title = stringResource(R.string.account_email_title),
        modifier = modifier,
        navigationIcon = { BackButton(actions.onBack) },
    ) { padding ->
        CenteredColumn(Modifier.padding(padding)) {
            SegmentedControl(
                options = listOf(
                    stringResource(R.string.account_mode_sign_in),
                    stringResource(R.string.account_mode_sign_up),
                ),
                selectedIndex = EmailAuthMode.entries.indexOf(uiState.mode),
                onSelect = { actions.onModeSelected(EmailAuthMode.entries[it]) },
            )
            OutlinedTextField(
                value = uiState.email,
                onValueChange = actions.onEmailChange,
                label = { Text(stringResource(R.string.account_email_label)) },
                singleLine = true,
                isError = uiState.showFieldErrors && uiState.isEmailInvalid,
                supportingText = if (uiState.showFieldErrors && uiState.isEmailInvalid) {
                    { Text(stringResource(R.string.account_error_email_invalid)) }
                } else {
                    null
                },
                keyboardOptions = EmailKeyboard.copy(imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.EmailAddress },
            )
            PasswordField(
                value = uiState.password,
                onValueChange = actions.onPasswordChange,
                label = stringResource(R.string.account_password_label),
                contentType = if (uiState.isSignUp) ContentType.NewPassword else ContentType.Password,
                isError = uiState.showFieldErrors && uiState.isPasswordWeak,
                supporting = if (uiState.isSignUp) stringResource(R.string.account_password_hint) else null,
                imeAction = if (uiState.isSignUp) ImeAction.Next else ImeAction.Done,
                onDone = {
                    focusManager.clearFocus()
                    actions.onSubmit()
                },
            )
            if (uiState.isSignUp) {
                SignUpFields(uiState, actions, onDone = { focusManager.clearFocus() })
            }
            uiState.error?.let { error ->
                Text(
                    authErrorMessage(error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Button(onClick = actions.onSubmit, enabled = uiState.canSubmit || uiState.isBusy, modifier = ButtonHeight) {
                ButtonContent(
                    stringResource(
                        if (uiState.isSignUp) R.string.account_submit_sign_up else R.string.account_submit_sign_in,
                    ),
                    isBusy = uiState.isBusy,
                )
            }
            if (!uiState.isSignUp) {
                TextButton(
                    onClick = actions.onForgotPassword,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text(stringResource(R.string.account_forgot_password)) }
            }
        }
    }
}

/** 가입할 때만 더 받는 비밀번호 확인과 개인정보처리방침 동의. */
@Composable
private fun SignUpFields(uiState: EmailAuthUiState, actions: EmailAuthActions, onDone: () -> Unit) {
    PasswordField(
        value = uiState.passwordConfirm,
        onValueChange = actions.onPasswordConfirmChange,
        label = stringResource(R.string.account_password_confirm_label),
        contentType = ContentType.NewPassword,
        isError = uiState.showFieldErrors && uiState.isConfirmMismatch,
        supporting = if (uiState.showFieldErrors && uiState.isConfirmMismatch) {
            stringResource(R.string.account_error_password_mismatch)
        } else {
            null
        },
        imeAction = ImeAction.Done,
        onDone = onDone,
    )
    PrivacyAgreement(
        checked = uiState.isPrivacyAgreed,
        onCheckedChange = actions.onPrivacyAgreedChange,
        onOpenPrivacyPolicy = actions.onOpenPrivacyPolicy,
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    contentType: ContentType,
    isError: Boolean,
    supporting: String?,
    imeAction: ImeAction,
    onDone: () -> Unit,
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supporting?.let { { Text(it) } },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(
                        if (isVisible) R.string.account_password_hide else R.string.account_password_show,
                    ),
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.contentType = contentType },
    )
}

@Composable
private fun PrivacyAgreement(checked: Boolean, onCheckedChange: (Boolean) -> Unit, onOpenPrivacyPolicy: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Text(
                stringResource(R.string.account_privacy_agree),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = ChageunTheme.spacing.xs),
            )
        }
        TextButton(onClick = onOpenPrivacyPolicy) { Text(stringResource(R.string.account_privacy_view)) }
    }
}

/** AC03. 가입 여부와 상관없이 같은 안내를 보여 준다. */
@Composable
private fun PasswordResetDialog(
    state: PasswordResetState,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.account_reset_title)) },
        text = {
            if (state.isSent) {
                Text(stringResource(R.string.account_reset_sent, state.email.trim()))
            } else {
                Column {
                    Text(stringResource(R.string.account_reset_body))
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = { Text(stringResource(R.string.account_email_label)) },
                        singleLine = true,
                        isError = state.error != null,
                        supportingText = state.error?.let { { Text(authErrorMessage(it)) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Send,
                        ),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = ChageunTheme.spacing.sm),
                    )
                }
            }
        },
        confirmButton = {
            if (state.isSent) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.account_close)) }
            } else {
                TextButton(onClick = onSend, enabled = !state.isBusy && !state.isEmailInvalid) {
                    Text(stringResource(R.string.account_reset_send))
                }
            }
        },
        dismissButton = if (state.isSent) {
            null
        } else {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.account_cancel)) } }
        },
    )
}

/** 한글 키보드를 쓰는 사람도 이메일 칸에서는 영문 자판이 먼저 뜨게 한다. */
private val EmailKeyboard = KeyboardOptions(
    keyboardType = KeyboardType.Email,
    autoCorrectEnabled = false,
    hintLocales = LocaleList(Locale("en")),
)
