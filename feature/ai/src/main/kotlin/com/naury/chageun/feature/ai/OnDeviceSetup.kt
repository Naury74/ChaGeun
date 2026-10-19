package com.naury.chageun.feature.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.ChageunButton
import com.naury.chageun.core.designsystem.component.ChageunCard
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.kinetica.ui.KLoadingIndicator
import com.naury.kinetica.ui.KLoadingStyle
import com.naury.kinetica.ui.KProgressBar
import com.naury.kinetica.ui.KVariant
/**
 * 차근 AI를 처음 쓸 때 보여 주는 준비 카드. 무엇을 받는지와 좋은 점을 먼저 알리고, 사용자가 눌러야 내려받는다.
 * 받는 동안에는 진행률을, 실패하면 다시 시도를 보여 주고, 준비가 끝나면 이 카드 대신 추천 질문이 나온다.
 */
@Composable
internal fun OnDeviceSetupCard(state: OnDeviceState, onDownload: () -> Unit, onUseWeb: () -> Unit) {
    ChageunCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(ChageunTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(AiGradient, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White)
                }
                Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs)) {
                    Text(
                        stringResource(R.string.ai_setup_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.ai_setup_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AnimatedContent(targetState = setupStep(state), label = "ai-setup-step") { step ->
                when (step) {
                    SetupStep.Checking -> Checking()
                    SetupStep.Downloading -> Downloading(state.downloadProgress)
                    SetupStep.Ready, SetupStep.Failed -> Actions(
                        hasFailed = step == SetupStep.Failed,
                        onDownload = onDownload,
                        onUseWeb = onUseWeb,
                    )
                }
            }
            Column(
                Modifier.padding(top = ChageunTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            ) {
                Benefit(Icons.Filled.Lock, stringResource(R.string.ai_setup_private))
                Benefit(Icons.Filled.WifiOff, stringResource(R.string.ai_setup_offline))
                Benefit(Icons.Filled.PhoneAndroid, stringResource(R.string.ai_setup_system))
            }
        }
    }
}

private enum class SetupStep { Checking, Ready, Downloading, Failed }

private fun setupStep(state: OnDeviceState): SetupStep = when {
    state.status == null -> SetupStep.Checking
    state.isDownloading -> SetupStep.Downloading
    state.hasDownloadFailed -> SetupStep.Failed
    else -> SetupStep.Ready
}

@Composable
private fun Benefit(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp).gradientTint())
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Checking() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        modifier = Modifier.padding(top = ChageunTheme.spacing.xs),
    ) {
        val checking = stringResource(R.string.ai_destination_on_device_checking)
        KLoadingIndicator(style = KLoadingStyle.Dots, color = AiAccent, size = CHECKING_SIZE, label = checking)
        Text(
            checking,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Downloading(progress: Float?) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        modifier = Modifier
            .padding(top = ChageunTheme.spacing.xs)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            progress?.let { stringResource(R.string.ai_preparing, (it * PERCENT).toInt()) }
                ?: stringResource(R.string.ai_preparing_unknown),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        // 진행률을 모르면 빛 한 줄기가 흐르고, 알면 용수철처럼 따라 찬다.
        KProgressBar(progress = progress, color = AiAccent)
        Text(
            stringResource(R.string.ai_setup_keep_open),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Actions(hasFailed: Boolean, onDownload: () -> Unit, onUseWeb: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Text(
            stringResource(if (hasFailed) R.string.ai_prepare_failed else R.string.ai_setup_wifi),
            style = MaterialTheme.typography.bodySmall,
            color = if (hasFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AiAccentColors {
            ChageunButton(
                text = stringResource(if (hasFailed) R.string.ai_setup_retry else R.string.ai_setup_download),
                onClick = onDownload,
                icon = Icons.Filled.Download,
                fullWidth = true,
            )
        }
        ChageunButton(
            text = stringResource(R.string.ai_setup_use_web),
            onClick = onUseWeb,
            variant = KVariant.Text,
            fullWidth = true,
        )
    }
}

private const val PERCENT = 100
private val CHECKING_SIZE = 24.dp
