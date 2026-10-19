package com.naury.chageun.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.naury.chageun.core.designsystem.component.ChageunToasts
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.component.PressStyle
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.model.AuthUser
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.ChageunDialog
import com.naury.chageun.core.ui.ChageunLinks
import com.naury.chageun.core.ui.DialogAction
import com.naury.chageun.core.ui.DialogActionStyle
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.ProfileAvatar
import com.naury.chageun.core.ui.openUriSafely
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KStatus
import com.naury.kinetica.ui.KVariant

@Composable
fun AccountRoute(
    onBack: () -> Unit,
    onOpenEmail: () -> Unit,
    onOpenDeleteAccount: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel(),
) {
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
        onDeleteAccount = onOpenDeleteAccount,
        onStartOver = viewModel::startOver,
        onEditName = viewModel::editName,
    )
    uiState.nameEditor?.let {
        ProfileNameDialog(
            state = it,
            onDraftChange = viewModel::changeNameDraft,
            onSave = { viewModel.saveName() },
            onUseDefaultName = { viewModel.saveName(useDefaultName = true) },
            onDismiss = viewModel::closeNameEditor,
        )
    }
    if (isSignOutConfirming) {
        ChageunDialog(
            title = stringResource(R.string.account_sign_out_title),
            message = stringResource(R.string.account_sign_out_body),
            icon = Icons.AutoMirrored.Filled.Logout,
            onDismiss = { isSignOutConfirming = false },
            actions = listOf(
                DialogAction(stringResource(R.string.account_sign_out)) {
                    isSignOutConfirming = false
                    viewModel.signOut()
                },
                DialogAction(stringResource(R.string.account_cancel), DialogActionStyle.Secondary) {
                    isSignOutConfirming = false
                },
            ),
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
    onDeleteAccount: () -> Unit = {},
    onStartOver: () -> Unit = {},
    onEditName: () -> Unit = {},
) {
    val noticeText = uiState.notice?.let { noticeMessage(it) }
    LaunchedEffect(uiState.notice) {
        val notice = uiState.notice
        if (notice != null && noticeText != null) {
            showAccountNotice(notice, noticeText)
            onNoticeShown()
        }
    }
    LargeTitleScaffold(
        title = stringResource(R.string.account_title),
        modifier = modifier,
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
                // 이메일 인증을 마쳐야 가입이 끝난다. 그 전에는 인증 안내만 보여 준다(ADR-006).
                user.needsEmailVerification -> VerifyEmail(
                    email = user.email.orEmpty(),
                    isBusy = uiState.isBusy,
                    onCheckVerification = onCheckVerification,
                    onResendVerification = onResendVerification,
                    onStartOver = onStartOver,
                    onSignOut = onSignOut,
                )
                else -> AccountHome(
                    user = user,
                    photoPath = uiState.photoPath,
                    isBusy = uiState.isBusy,
                    onSignOut = onSignOut,
                    onDeleteAccount = onDeleteAccount,
                    onEditName = onEditName,
                )
            }
        }
    }
}

/** 완료는 성공, 실패는 오류 토스트로 띄운다. 인증 메일 안내처럼 결과가 아닌 알림은 보통 토스트다. */
private fun showAccountNotice(notice: AccountNotice, text: String) {
    when (notice) {
        is AccountNotice.Failed -> ChageunToasts.error(text)
        AccountNotice.Verified -> ChageunToasts.success(text)
        AccountNotice.VerificationSent -> ChageunToasts.show(text, KStatus.Info)
        AccountNotice.NotVerifiedYet -> ChageunToasts.show(text, KStatus.Warning)
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
        Modifier
            .appearIn(0)
            .fillMaxWidth(),
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
                Icons.Filled.AccountCircle,
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
    CardGroup(title = null, modifier = Modifier.appearIn(1)) {
        ListRow(
            Icons.Filled.Mail,
            stringResource(R.string.account_benefit_backup),
            tone = ChageunTheme.colors.good,
        )
        GroupDivider()
        ListRow(
            Icons.Filled.VerifiedUser,
            stringResource(R.string.account_benefit_restore),
            tone = ChageunTheme.colors.upcoming,
        )
        GroupDivider()
        ListRow(Icons.Filled.Security, stringResource(R.string.account_benefit_optional))
    }
    GoogleButton(
        onClick = onContinueWithGoogle,
        enabled = isGoogleConfigured,
        isBusy = isBusy,
        modifier = Modifier.appearIn(2),
    )
    if (!isGoogleConfigured) {
        Text(
            stringResource(R.string.account_google_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    AccountPrimaryButton(
        text = stringResource(R.string.account_continue_email),
        onClick = onContinueWithEmail,
        enabled = !isBusy,
        icon = Icons.Filled.Mail,
        modifier = Modifier.appearIn(2),
    )
    KButton(
        text = stringResource(R.string.account_privacy_policy),
        onClick = onOpenPrivacyPolicy,
        variant = KVariant.Text,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
}

/** AC04. 인증 메일의 링크를 누르고 돌아와 확인을 눌러야 가입이 끝난다. */
@Composable
private fun ColumnScope.VerifyEmail(
    email: String,
    isBusy: Boolean,
    onCheckVerification: () -> Unit,
    onResendVerification: () -> Unit,
    onStartOver: () -> Unit,
    onSignOut: () -> Unit,
) {
    val tone = ChageunTheme.colors.upcoming
    Box(
        Modifier
            .appearIn(0)
            .size(HERO_ICON_BOX)
            .background(tone.container, CircleShape)
            .align(Alignment.CenterHorizontally),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.MarkEmailUnread,
            contentDescription = null,
            tint = tone.content,
            modifier = Modifier.size(36.dp),
        )
    }
    Text(
        stringResource(R.string.account_verify_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() },
    )
    Text(
        stringResource(R.string.account_verify_body, email),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    AccountPrimaryButton(
        text = stringResource(R.string.account_verify_check),
        onClick = onCheckVerification,
        loading = isBusy,
    )
    CardGroup(title = null, modifier = Modifier.appearIn(1)) {
        ListRow(
            Icons.Filled.Refresh,
            stringResource(R.string.account_verify_resend),
            onClick = onResendVerification,
            enabled = !isBusy,
        )
        GroupDivider()
        ListRow(
            Icons.Filled.Edit,
            stringResource(R.string.account_verify_start_over),
            onClick = onStartOver,
            enabled = !isBusy,
        )
        GroupDivider()
        ListRow(
            Icons.AutoMirrored.Filled.Logout,
            stringResource(R.string.account_sign_out),
            onClick = onSignOut,
            enabled = !isBusy,
            trailing = {},
        )
    }
}

/** AC05 계정 화면. */
@Composable
private fun AccountHome(
    user: AuthUser,
    photoPath: String?,
    isBusy: Boolean,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onEditName: () -> Unit,
) {
    ProfileHeader(user, photoPath, onEditName)
    CardGroup(title = null, modifier = Modifier.appearIn(1)) {
        ListRow(
            Icons.AutoMirrored.Filled.Logout,
            stringResource(R.string.account_sign_out),
            titleColor = MaterialTheme.colorScheme.error,
            onClick = onSignOut,
            enabled = !isBusy,
            trailing = {},
        )
        GroupDivider()
        ListRow(
            Icons.Filled.PersonRemove,
            stringResource(R.string.account_delete),
            titleColor = MaterialTheme.colorScheme.error,
            onClick = onDeleteAccount,
            enabled = !isBusy,
        )
    }
}

/** 계정 화면 맨 위. Google로 로그인했으면 Google 사진과 이름이 바로 보인다. 이름을 누르면 부를 이름을 바꾼다. */
@Composable
private fun ProfileHeader(user: AuthUser, photoPath: String?, onEditName: () -> Unit) {
    val name = user.profileName
    Column(
        Modifier
            .appearIn(0)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs),
    ) {
        // 이름은 아래 글자로 읽으므로 동그라미는 장식으로 둔다.
        ProfileAvatar(
            name = name,
            photoPath = photoPath,
            size = HERO_ICON_BOX,
            modifier = Modifier.padding(bottom = ChageunTheme.spacing.xs),
        )
        val title = name ?: user.email.orEmpty()
        EditableName(title, onEditName)
        user.email?.takeIf { it != title }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            stringResource(
                if (AuthMethod.Google in user.methods) {
                    R.string.account_signed_in_google
                } else {
                    R.string.account_signed_in_email
                },
            ),
            style = MaterialTheme.typography.labelMedium,
            color = ChageunTheme.colors.good.content,
            textAlign = TextAlign.Center,
        )
    }
}

/** 이름 옆 연필로 바꿀 수 있다는 걸 보여 준다. TalkBack은 이름을 읽고 "부를 이름 바꾸기"를 동작으로 알려 준다. */
@Composable
private fun EditableName(name: String, onEditName: () -> Unit) {
    Row(
        Modifier
            .clip(MaterialTheme.shapes.medium)
            .pressable(
                onClick = onEditName,
                style = PressStyle.Highlight,
                onClickLabel = stringResource(R.string.account_name_edit),
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = ChageunTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
    ) {
        // 연필과 같은 폭을 왼쪽에 비워 이름이 사진·이메일과 같은 가운데에 오게 한다.
        Spacer(Modifier.size(EDIT_ICON))
        Text(
            name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f, fill = false)
                .semantics { heading() },
        )
        Icon(
            Icons.Filled.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(EDIT_ICON),
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
private val EDIT_ICON = 20.dp
