package com.naury.chageun.feature.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.component.SegmentedControl
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.ChageunDialog
import com.naury.chageun.core.ui.ChageunLinks
import com.naury.chageun.core.ui.DialogAction
import com.naury.chageun.core.ui.DialogActionStyle
import com.naury.chageun.core.ui.openUriSafely
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KButtonSize
import com.naury.kinetica.ui.KCheckbox
import com.naury.kinetica.ui.KPasswordField
import com.naury.kinetica.ui.KTextField
import com.naury.kinetica.ui.KVariant
import kotlinx.coroutines.delay

@Composable
fun EmailAuthRoute(onBack: () -> Unit, onCompleted: () -> Unit, viewModel: EmailAuthViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            // 버튼이 체크 표시로 바뀌는 걸 보고 넘어가도록 잠깐 기다린다.
            delay(SUCCESS_HOLD_MS)
            onCompleted()
        }
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
        CenteredColumn(top = padding.calculateTopPadding()) {
            SegmentedControl(
                options = listOf(
                    stringResource(R.string.account_mode_sign_in),
                    stringResource(R.string.account_mode_sign_up),
                ),
                selectedIndex = EmailAuthMode.entries.indexOf(uiState.mode),
                onSelect = { actions.onModeSelected(EmailAuthMode.entries[it]) },
            )
            KTextField(
                value = uiState.email,
                onValueChange = actions.onEmailChange,
                label = stringResource(R.string.account_email_label),
                error = stringResource(R.string.account_error_email_invalid)
                    .takeIf { uiState.showFieldErrors && uiState.isEmailInvalid },
                keyboardOptions = EmailKeyboard.copy(imeAction = ImeAction.Next),
                // KTextField는 modifier를 입력란 바깥 묶음에 붙여 이메일 자동 채우기 종류를 입력란에 달 수 없다.
                // 비밀번호 칸의 자동 채우기 종류는 KPasswordField가 단다.
                modifier = Modifier.fillMaxWidth(),
            )
            PasswordField(
                value = uiState.password,
                onValueChange = actions.onPasswordChange,
                label = stringResource(R.string.account_password_label),
                isNewPassword = uiState.isSignUp,
                error = stringResource(R.string.account_password_hint)
                    .takeIf { uiState.showFieldErrors && uiState.isPasswordWeak },
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
            uiState.error?.let { AccountErrorAlert(authErrorMessage(it)) }
            AccountPrimaryButton(
                text = stringResource(
                    if (uiState.isSignUp) R.string.account_submit_sign_up else R.string.account_submit_sign_in,
                ),
                onClick = actions.onSubmit,
                enabled = uiState.canSubmit || uiState.isBusy || uiState.isCompleted,
                loading = uiState.isBusy,
                success = uiState.isCompleted,
            )
            if (!uiState.isSignUp) {
                KButton(
                    text = stringResource(R.string.account_forgot_password),
                    onClick = actions.onForgotPassword,
                    variant = KVariant.Text,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
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
        isNewPassword = true,
        error = stringResource(R.string.account_error_password_mismatch)
            .takeIf { uiState.showFieldErrors && uiState.isConfirmMismatch },
        supporting = null,
        imeAction = ImeAction.Done,
        onDone = onDone,
    )
    PrivacyAgreement(
        checked = uiState.isPrivacyAgreed,
        onCheckedChange = actions.onPrivacyAgreedChange,
        onOpenPrivacyPolicy = actions.onOpenPrivacyPolicy,
    )
}

/**
 * 눈 아이콘으로 보이고 숨기는 비밀번호 칸. 앱의 비밀번호 규칙(8자 이상, 글자와 숫자)이 Kinetica 강도 막대·체크리스트 기준과
 * 달라서 둘 다 끄고, 규칙은 [supporting] 문구로 알린다. 자동 채우기 종류는 [isNewPassword]로 정한다.
 */
@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isNewPassword: Boolean,
    error: String?,
    supporting: String?,
    imeAction: ImeAction,
    onDone: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        KPasswordField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            showStrength = false,
            newPassword = isNewPassword,
            error = error,
            imeAction = imeAction,
            keyboardActions = KeyboardActions(onDone = { onDone() }),
        )
        // 오류가 보일 때는 같은 자리에 오류 문구가 나오므로 안내는 숨긴다.
        if (supporting != null && error == null) {
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = ChageunTheme.spacing.md, vertical = ChageunTheme.spacing.xxs),
            )
        }
    }
}

@Composable
private fun PrivacyAgreement(checked: Boolean, onCheckedChange: (Boolean) -> Unit, onOpenPrivacyPolicy: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.small)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                .heightIn(min = TOUCH_MIN),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KCheckbox(checked = checked, onCheckedChange = null)
            Text(
                stringResource(R.string.account_privacy_agree),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = ChageunTheme.spacing.xs),
            )
        }
        KButton(
            text = stringResource(R.string.account_privacy_view),
            onClick = onOpenPrivacyPolicy,
            variant = KVariant.Text,
            size = KButtonSize.Small,
        )
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
    val actions = if (state.isSent) {
        listOf(DialogAction(stringResource(R.string.account_close), onClick = onDismiss))
    } else {
        listOf(
            DialogAction(
                stringResource(R.string.account_reset_send),
                enabled = !state.isBusy && !state.isEmailInvalid,
                onClick = onSend,
            ),
            DialogAction(stringResource(R.string.account_cancel), DialogActionStyle.Secondary, onClick = onDismiss),
        )
    }
    ChageunDialog(
        title = stringResource(R.string.account_reset_title),
        message = if (state.isSent) {
            stringResource(R.string.account_reset_sent, state.email.trim())
        } else {
            stringResource(R.string.account_reset_body)
        },
        icon = if (state.isSent) Icons.Filled.MarkEmailRead else Icons.Filled.LockReset,
        onDismiss = onDismiss,
        actions = actions,
        content = if (state.isSent) {
            null
        } else {
            {
                KTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = stringResource(R.string.account_email_label),
                    error = state.error?.let { authErrorMessage(it) },
                    keyboardOptions = EmailKeyboard.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

/** 한글 키보드를 쓰는 사람도 이메일 칸에서는 영문 자판이 먼저 뜨게 한다. */
private val EmailKeyboard = KeyboardOptions(
    keyboardType = KeyboardType.Email,
    autoCorrectEnabled = false,
    hintLocales = LocaleList(Locale("en")),
)

private const val SUCCESS_HOLD_MS = 700L
private val TOUCH_MIN = 48.dp
