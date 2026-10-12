package com.naury.chageun.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import kotlinx.coroutines.delay

/**
 * 방금 한 일을 화면 아래에 잠깐 알리고 되돌릴 수 있게 하는 막대. [message]가 null이면 숨는다.
 *
 * [key]가 바뀔 때마다 시간을 처음부터 다시 재고, 시간이 다 되면 [onTimeout]을 부른다. TalkBack 사용자가 버튼까지
 * 옮겨 갈 수 있도록 시스템이 권하는 시간(접근성 설정의 '조치를 취할 시간')만큼 더 머문다. 문구는 나타날 때 읽힌다.
 */
@Composable
fun UndoBar(
    message: String?,
    actionLabel: String,
    onAction: () -> Unit,
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
    key: Any? = message,
    icon: ImageVector? = null,
    durationMillis: Long = UNDO_BAR_MILLIS,
) {
    val accessibility = LocalAccessibilityManager.current
    val currentOnTimeout by rememberUpdatedState(onTimeout)
    if (message != null) {
        LaunchedEffect(key) {
            delay(
                accessibility?.calculateRecommendedTimeoutMillis(
                    durationMillis,
                    containsIcons = icon != null,
                    containsText = true,
                    containsControls = true,
                ) ?: durationMillis,
            )
            currentOnTimeout()
        }
    }
    // 사라지는 동안에도 마지막 문구를 그대로 보여 준다.
    val last = remember { mutableStateOf(message) }
    if (message != null) last.value = message
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(motionSpec()) { it / 2 } + fadeIn(motionSpec()),
        exit = slideOutVertically(motionSpec()) { it / 2 } + fadeOut(motionSpec()),
        modifier = modifier,
    ) {
        UndoBarContent(last.value.orEmpty(), actionLabel, onAction, icon)
    }
}

@Composable
private fun UndoBarContent(message: String, actionLabel: String, onAction: () -> Unit, icon: ImageVector?) {
    val spacing = ChageunTheme.spacing
    Surface(
        modifier = Modifier
            .padding(horizontal = spacing.gutter, vertical = spacing.sm)
            .widthIn(max = MAX_WIDTH)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = ELEVATION,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = spacing.minTouchTarget + spacing.xs)
                .padding(start = spacing.md, end = spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(ICON_SIZE)) }
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = spacing.sm)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(
                onClick = onAction,
                modifier = Modifier.heightIn(min = spacing.minTouchTarget),
            ) {
                Text(
                    actionLabel,
                    color = MaterialTheme.colorScheme.inversePrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** 되돌리기 막대가 머무는 기본 시간. 손을 옮겨 버튼을 누를 수 있을 만큼 넉넉히 둔다. */
const val UNDO_BAR_MILLIS = 6_000L

private val MAX_WIDTH = 480.dp
private val ICON_SIZE = 20.dp
private val ELEVATION = 6.dp
