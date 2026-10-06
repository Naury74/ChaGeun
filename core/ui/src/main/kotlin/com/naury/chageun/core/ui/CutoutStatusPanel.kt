package com.naury.chageun.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.CutoutFailure
import com.naury.chageun.core.model.CutoutStatus
import kotlin.math.roundToInt

/**
 * 내 차 사진의 배경 지우기 진행을 Hero 사진 바로 아래에 알린다.
 * 모델을 처음 내려받을 때는 이유와 진행률을, 실패하면 이유와 다시 시도를 보여 준다.
 *
 * @param canRemoveBackground 배경을 지우지 않은 사진이 있어 "배경 지우기"를 보여 줄 수 있으면 true.
 */
@Composable
fun CutoutStatusPanel(
    status: CutoutStatus,
    canRemoveBackground: Boolean,
    onRemoveBackground: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fade = motionSpec<Float>()
    AnimatedContent(
        targetState = status.takeUnless { it == CutoutStatus.Idle && !canRemoveBackground },
        transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
        contentKey = { it?.let { state -> state::class } },
        modifier = modifier.fillMaxWidth(),
        label = "cutout-status",
    ) { shown ->
        when (shown) {
            null -> Spacer(Modifier)
            // Hero의 다른 내용과 같이 왼쪽에 맞춘다.
            CutoutStatus.Idle -> Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onRemoveBackground) {
                    Icon(Icons.Outlined.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(ChageunTheme.spacing.xs))
                    Text(stringResource(R.string.cutout_remove_background))
                }
            }
            is CutoutStatus.DownloadingModel -> Downloading(shown.progress)
            CutoutStatus.Processing -> StatusCard {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                StatusText(stringResource(R.string.cutout_processing))
            }
            is CutoutStatus.Failed -> Failed(shown.reason, onRemoveBackground)
        }
    }
}

@Composable
private fun Downloading(progress: Float?) {
    StatusCard(
        footer = {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            if (progress == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), trackColor = trackColor, strokeCap = StrokeCap.Round)
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = trackColor,
                    strokeCap = StrokeCap.Round,
                )
            }
        },
    ) {
        StatusIcon(Icons.Outlined.CloudDownload, MaterialTheme.colorScheme.primary)
        StatusText(stringResource(R.string.cutout_downloading), Modifier.weight(1f))
        progress?.let {
            Text(
                stringResource(R.string.cutout_progress, (it * PERCENT).roundToInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Failed(reason: CutoutFailure, onRetry: () -> Unit) {
    val message = when (reason) {
        CutoutFailure.ModelUnavailable -> R.string.cutout_failed_download
        CutoutFailure.NoSubject -> R.string.cutout_failed_no_car
        CutoutFailure.Error -> R.string.cutout_failed
    }
    StatusCard {
        // 차를 찾지 못한 경우는 같은 사진으로 다시 해도 결과가 같으므로 다른 사진을 고르도록 안내만 한다.
        if (reason == CutoutFailure.NoSubject) {
            StatusIcon(Icons.Outlined.Info, MaterialTheme.colorScheme.onSurfaceVariant)
            StatusText(stringResource(message), Modifier.weight(1f))
        } else {
            StatusIcon(Icons.Outlined.ErrorOutline, MaterialTheme.colorScheme.error)
            StatusText(stringResource(message), Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text(stringResource(R.string.cutout_retry)) }
        }
    }
}

@Composable
private fun StatusCard(footer: (@Composable () -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(
            Modifier.padding(horizontal = ChageunTheme.spacing.md, vertical = ChageunTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            ) { content() }
            footer?.invoke()
        }
    }
}

@Composable
private fun StatusIcon(icon: ImageVector, tint: Color) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
}

@Composable
private fun StatusText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
}

private const val PERCENT = 100
