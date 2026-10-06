package com.naury.chageun.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.auth.AuthError

/** 계정 화면들의 내용 폭. 펼친 폴드나 태블릿에서도 입력 줄이 너무 길어지지 않게 가운데 모은다. */
internal val ACCOUNT_CONTENT_MAX_WIDTH = 480.dp

private val BUTTON_MIN_HEIGHT = 52.dp
private val PROGRESS_SIZE = 20.dp

@Composable
internal fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.account_back))
    }
}

/** 스크롤되는 가운데 정렬 열. 키보드가 올라와도 입력 칸까지 스크롤할 수 있다. */
@Composable
internal fun CenteredColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = ACCOUNT_CONTENT_MAX_WIDTH)
                .fillMaxWidth()
                .padding(horizontal = ChageunTheme.spacing.gutter, vertical = ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
            content = content,
        )
    }
}

/**
 * Google 브랜드 가이드의 밝은 버튼. 흰 바탕, 회색 테두리, 왼쪽 G 로고를 지킨다. 다크 테마에서도 같은 모양을 쓴다.
 */
@Composable
internal fun GoogleButton(onClick: () -> Unit, enabled: Boolean, isBusy: Boolean, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isBusy,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BUTTON_MIN_HEIGHT),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = GoogleText,
            disabledContainerColor = Color.White.copy(alpha = DISABLED_ALPHA),
            disabledContentColor = GoogleText.copy(alpha = DISABLED_ALPHA),
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isBusy) {
                CircularProgressIndicator(Modifier.size(PROGRESS_SIZE), strokeWidth = 2.dp, color = GoogleText)
            } else {
                Icon(
                    painterResource(R.drawable.ic_google_logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(PROGRESS_SIZE),
                )
            }
            Spacer(Modifier.width(ChageunTheme.spacing.sm))
            Text(stringResource(R.string.account_continue_google))
        }
    }
}

/** 진행 중이면 글자 대신 원형 진행 표시를 보여 준다. 버튼 크기는 그대로다. */
@Composable
internal fun ButtonContent(text: String, isBusy: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        Text(text, color = if (isBusy) Color.Transparent else LocalContentColor.current)
        if (isBusy) {
            CircularProgressIndicator(
                Modifier.size(PROGRESS_SIZE),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        }
    }
}

internal val ButtonHeight = Modifier
    .fillMaxWidth()
    .heightIn(min = BUTTON_MIN_HEIGHT)

@Composable
internal fun authErrorMessage(error: AuthError): String = stringResource(
    when (error) {
        AuthError.Network -> R.string.account_error_network
        AuthError.EmailInUse -> R.string.account_error_email_in_use
        AuthError.WeakPassword -> R.string.account_error_weak_password
        AuthError.InvalidCredentials -> R.string.account_error_invalid_credentials
        AuthError.TooManyRequests -> R.string.account_error_too_many
        AuthError.UserDisabled -> R.string.account_error_disabled
        AuthError.NoGoogleAccount -> R.string.account_error_no_google
        AuthError.Unavailable -> R.string.account_error_unavailable
        AuthError.Unknown -> R.string.account_error_unknown
    },
)

private val GoogleText = Color(0xFF1F1F1F)
private const val DISABLED_ALPHA = 0.6f
