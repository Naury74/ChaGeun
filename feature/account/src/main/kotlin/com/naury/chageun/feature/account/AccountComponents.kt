package com.naury.chageun.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.auth.AuthError
import com.naury.kinetica.ui.KAlert
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KButtonSize
import com.naury.kinetica.ui.KIconButton
import com.naury.kinetica.ui.KProgressRing
import com.naury.kinetica.ui.KStatus
import com.naury.kinetica.ui.KTone
import com.naury.kinetica.ui.KVariant
import com.naury.kinetica.ui.jellyPress
import com.naury.kinetica.ui.kClickable
import com.naury.kinetica.ui.staggerIn

/** 계정 화면들의 내용 폭. 펼친 폴드나 태블릿에서도 입력 줄이 너무 길어지지 않게 가운데 모은다. */
internal val ACCOUNT_CONTENT_MAX_WIDTH = 480.dp

private val BUTTON_MIN_HEIGHT = 54.dp
private val PROGRESS_SIZE = 20.dp

@Composable
internal fun BackButton(onBack: () -> Unit) {
    KIconButton(
        Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = stringResource(R.string.account_back),
        onClick = onBack,
        variant = KVariant.Text,
        tone = KTone.Neutral,
    )
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
 * KButton은 아이콘을 글자색으로 칠해 G 로고 색이 사라지므로, 모양과 누름 효과만 Kinetica 버튼에 맞춰 직접 그린다.
 */
@Composable
internal fun GoogleButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isBusy: Boolean,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.account_continue_google),
) {
    val active = enabled && !isBusy
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier
            .then(if (active) Modifier.jellyPress() else Modifier)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .fillMaxWidth()
            .heightIn(min = BUTTON_MIN_HEIGHT)
            .background(Color.White, shape)
            .border(1.dp, GoogleBorder, shape)
            .clip(shape)
            .kClickable(shape, enabled = active, onClick = onClick)
            .padding(horizontal = ChageunTheme.spacing.lg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isBusy) {
            KProgressRing(
                progress = null,
                modifier = Modifier.size(PROGRESS_SIZE),
                color = GoogleText,
                thickness = 2.dp,
            )
        } else {
            Icon(
                painterResource(R.drawable.ic_google_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(PROGRESS_SIZE),
            )
        }
        Spacer(Modifier.width(ChageunTheme.spacing.sm))
        Text(label, color = GoogleText, style = MaterialTheme.typography.titleMedium)
    }
}

/** 계정 화면들의 주 동작 버튼. 요청이 오가는 동안에는 [loading]으로 링을 돌리고, 끝나면 [success]로 체크를 보여 준다. */
@Composable
internal fun AccountPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    success: Boolean = false,
    tone: KTone = KTone.Accent,
    variant: KVariant = KVariant.Filled,
    icon: ImageVector? = null,
) {
    KButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        tone = tone,
        size = KButtonSize.Large,
        icon = icon,
        loading = loading,
        success = success,
        enabled = enabled,
        fullWidth = true,
    )
}

/** 입력 칸 아래나 버튼 위에 놓는 오류 상자. 나타나면 TalkBack이 바로 읽는다. */
@Composable
internal fun AccountErrorAlert(message: String, modifier: Modifier = Modifier) {
    KAlert(
        message = message,
        status = KStatus.Error,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** 화면을 열 때 [index] 순서대로 아래에서 떠오르게 한다. 같은 화면 안 요소들이 차례로 채워진다. */
@Composable
internal fun Modifier.appearIn(index: Int): Modifier = staggerIn(index, delayMs = APPEAR_STEP_MS)

@Composable
internal fun authErrorMessage(error: AuthError): String = stringResource(
    when (error) {
        AuthError.Network -> R.string.account_error_network
        AuthError.EmailInUse -> R.string.account_error_email_in_use
        AuthError.WeakPassword -> R.string.account_error_weak_password
        AuthError.InvalidCredentials -> R.string.account_error_invalid_credentials
        AuthError.TooManyRequests -> R.string.account_error_too_many
        AuthError.UserDisabled -> R.string.account_error_disabled
        AuthError.RecentLoginRequired -> R.string.account_error_recent_login
        AuthError.NoGoogleAccount -> R.string.account_error_no_google
        AuthError.Unavailable -> R.string.account_error_unavailable
        AuthError.Unknown -> R.string.account_error_unknown
    },
)

private val GoogleText = Color(0xFF1F1F1F)
private val GoogleBorder = Color(0xFF747775)
private const val DISABLED_ALPHA = 0.6f
private const val APPEAR_STEP_MS = 50
