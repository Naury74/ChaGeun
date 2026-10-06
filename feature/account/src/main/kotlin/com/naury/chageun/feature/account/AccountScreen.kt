package com.naury.chageun.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
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
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.model.AuthUser
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.ChageunLinks
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.openUriSafely

@Composable
fun AccountRoute(onBack: () -> Unit, onOpenEmail: () -> Unit, viewModel: AccountViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var isSignOutConfirming by rememberSaveable { mutableStateOf(false) }
    AccountScreen(
        uiState = uiState,
        onBack = onBack,
        onContinueWithGoogle = { viewModel.signInWithGoogle(context) },
        onContinueWithEmail = onOpenEmail,
        onOpenPrivacyPolicy = { uriHandler.openUriSafely(context, ChageunLinks.PRIVACY_POLICY) },
        onCheckVerification = viewModel::checkVerification,
        onResendVerification = viewModel::resendVerification,
        onSignOut = { isSignOutConfirming = true },
        onNoticeShown = viewModel::dismissNotice,
    )
    if (isSignOutConfirming) {
        AlertDialog(
            onDismissRequest = { isSignOutConfirming = false },
            title = { Text(stringResource(R.string.account_sign_out_title)) },
            text = { Text(stringResource(R.string.account_sign_out_body)) },
            confirmButton = {
                TextButton(onClick = {
                    isSignOutConfirming = false
                    viewModel.signOut()
                }) { Text(stringResource(R.string.account_sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = { isSignOutConfirming = false }) { Text(stringResource(R.string.account_cancel)) }
            },
        )
    }
}

@Composable
fun AccountScreen(
    uiState: AccountUiState,
    onBack: () -> Unit,
    onContinueWithGoogle: () -> Unit,
    onContinueWithEmail: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onCheckVerification: () -> Unit,
    onResendVerification: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onNoticeShown: () -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    val noticeText = uiState.notice?.let { noticeMessage(it) }
    LaunchedEffect(uiState.notice) {
        if (noticeText != null) {
            snackbar.showSnackbar(noticeText)
            onNoticeShown()
        }
    }
    Box(modifier.fillMaxSize()) {
        LargeTitleScaffold(
            title = stringResource(R.string.account_title),
            navigationIcon = { BackButton(onBack) },
        ) { padding ->
            val user = uiState.user
            CenteredColumn(Modifier.padding(padding)) {
                when {
                    uiState.isLoading -> Unit
                    user == null -> AccountStart(
                        isGoogleConfigured = uiState.isGoogleConfigured,
                        isBusy = uiState.isBusy,
                        onContinueWithGoogle = onContinueWithGoogle,
                        onContinueWithEmail = onContinueWithEmail,
                        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                    )
                    else -> AccountHome(
                        user = user,
                        isBusy = uiState.isBusy,
                        onCheckVerification = onCheckVerification,
                        onResendVerification = onResendVerification,
                        onSignOut = onSignOut,
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

/** AC01. 계정이 왜 필요한지 먼저 보여 주고, 가입과 로그인은 버튼 하나로 묶는다. */
@Composable
private fun ColumnScope.AccountStart(
    isGoogleConfigured: Boolean,
    isBusy: Boolean,
    onContinueWithGoogle: () -> Unit,
    onContinueWithEmail: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        val tone = ChageunTheme.colors.good
        Box(
            Modifier
                .size(HERO_ICON_BOX)
                .background(tone.container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.CloudDone,
                contentDescription = null,
                tint = tone.content,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            stringResource(R.string.account_start_headline),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
    }
    CardGroup(title = null) {
        ListRow(
            Icons.Filled.CloudUpload,
            stringResource(R.string.account_benefit_backup),
            tone = ChageunTheme.colors.good,
        )
        GroupDivider()
        ListRow(
            Icons.Filled.Devices,
            stringResource(R.string.account_benefit_restore),
            tone = ChageunTheme.colors.upcoming,
        )
        GroupDivider()
        ListRow(Icons.Filled.Security, stringResource(R.string.account_benefit_optional))
    }
    GoogleButton(onClick = onContinueWithGoogle, enabled = isGoogleConfigured, isBusy = isBusy)
    if (!isGoogleConfigured) {
        Text(
            stringResource(R.string.account_google_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Button(onClick = onContinueWithEmail, enabled = !isBusy, modifier = ButtonHeight) {
        Icon(Icons.Filled.Mail, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.account_continue_email), Modifier.padding(start = ChageunTheme.spacing.sm))
    }
    TextButton(onClick = onOpenPrivacyPolicy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text(stringResource(R.string.account_privacy_policy))
    }
}

/** AC05의 계정 부분과 AC04 인증 안내. 백업은 P2에서 이 화면에 붙는다. */
@Composable
private fun AccountHome(
    user: AuthUser,
    isBusy: Boolean,
    onCheckVerification: () -> Unit,
    onResendVerification: () -> Unit,
    onSignOut: () -> Unit,
) {
    if (user.needsEmailVerification) {
        CardGroup(title = null) {
            ListRow(
                icon = Icons.Filled.MarkEmailUnread,
                title = stringResource(R.string.account_verify_title),
                body = stringResource(R.string.account_verify_body, user.email.orEmpty()),
                tone = ChageunTheme.colors.upcoming,
            )
            GroupDivider()
            ListRow(
                Icons.Filled.MarkEmailRead,
                stringResource(R.string.account_verify_check),
                onClick = onCheckVerification,
                enabled = !isBusy,
            )
            GroupDivider()
            ListRow(
                Icons.Filled.Refresh,
                stringResource(R.string.account_verify_resend),
                onClick = onResendVerification,
                enabled = !isBusy,
            )
        }
    }
    CardGroup(stringResource(R.string.account_section_account)) {
        ListRow(
            icon = Icons.Filled.AccountCircle,
            title = user.email ?: user.displayName.orEmpty(),
            body = stringResource(
                if (AuthMethod.Google in user.methods) {
                    R.string.account_signed_in_google
                } else {
                    R.string.account_signed_in_email
                },
            ),
            tone = ChageunTheme.colors.good,
        )
    }
    CardGroup(stringResource(R.string.account_section_backup)) {
        ListRow(
            Icons.Filled.CloudUpload,
            stringResource(R.string.account_backup_now),
            body = stringResource(R.string.account_backup_coming),
            enabled = false,
            onClick = {},
            trailing = {},
        )
    }
    CardGroup(title = null) {
        ListRow(
            Icons.AutoMirrored.Filled.Logout,
            stringResource(R.string.account_sign_out),
            titleColor = MaterialTheme.colorScheme.error,
            onClick = onSignOut,
            enabled = !isBusy,
            trailing = {},
        )
    }
}

@Composable
private fun noticeMessage(notice: AccountNotice): String = when (notice) {
    is AccountNotice.Failed -> authErrorMessage(notice.error)
    AccountNotice.VerificationSent -> stringResource(R.string.account_notice_verification_sent)
    AccountNotice.NotVerifiedYet -> stringResource(R.string.account_notice_not_verified)
    AccountNotice.Verified -> stringResource(R.string.account_notice_verified)
}

private val HERO_ICON_BOX = 72.dp
