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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.ChageunCard
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.CutoutFailure
import com.naury.chageun.core.model.CutoutStatus
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KProgressBar
import com.naury.kinetica.ui.KProgressRing
import com.naury.kinetica.ui.KVariant
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
                // Hero 하늘 위에서는 글래스, 그 밖에서는 글자 버튼이다.
                HeroGlassButton(
                    text = stringResource(R.string.cutout_remove_background),
                    onClick = onRemoveBackground,
                    icon = Icons.Outlined.AutoFixHigh,
                    outside = KVariant.Text,
                )
            }
            is CutoutStatus.DownloadingModel -> ModelDownloadCard(
                stringResource(R.string.cutout_downloading),
                shown.progress,
            )
            CutoutStatus.Processing -> StatusCard {
                KProgressRing(progress = null, modifier = Modifier.size(18.dp), thickness = 2.dp)
                StatusText(stringResource(R.string.cutout_processing))
            }
            is CutoutStatus.Failed -> Failed(shown.reason, onRemoveBackground)
        }
    }
}

/**
 * 기기 안에서 쓰는 ML 모델을 처음 내려받는 동안 이유와 진행률을 보여 준다.
 * [progress]는 0~1이며 크기를 아직 모르면 null로 두어 움직이는 막대를 보여 준다.
 */
@Composable
fun ModelDownloadCard(label: String, progress: Float?, modifier: Modifier = Modifier) {
    StatusCard(
        modifier = modifier,
        footer = {
            // 받는 동안 막대가 액체처럼 출렁이며 차오른다.
            KProgressBar(progress = progress, thickness = PROGRESS_THICKNESS, wave = true)
        },
    ) {
        StatusIcon(Icons.Outlined.CloudDownload, MaterialTheme.colorScheme.primary)
        StatusText(label, Modifier.weight(1f))
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
            KButton(
                text = stringResource(R.string.cutout_retry),
                onClick = onRetry,
                modifier = Modifier.heightIn(min = ChageunTheme.spacing.minTouchTarget),
                variant = KVariant.Text,
            )
        }
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    ChageunCard(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
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
private val PROGRESS_THICKNESS = 4.dp
