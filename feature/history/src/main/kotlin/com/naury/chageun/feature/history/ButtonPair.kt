package com.naury.chageun.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.kinetica.ui.kClickable

/**
 * 나란히 놓는 두 버튼. Kinetica 버튼은 글자를 한 줄로만 그리므로, 큰 글꼴이나 좁은 칸에서 반쪽 폭에 글자가
 * 다 들어가지 않으면 위아래로 쌓는다. 쌓을 때는 [content]의 순서대로 위에서부터 놓는다.
 */
@Composable
internal fun ButtonPair(
    modifier: Modifier = Modifier,
    gap: Dp = ChageunTheme.spacing.xs,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val space = gap.roundToPx()
        val width = constraints.maxWidth
        val half = ((width - space * (measurables.size - 1)) / measurables.size.coerceAtLeast(1)).coerceAtLeast(0)
        val fitsSideBySide = measurables.all { it.maxIntrinsicWidth(constraints.maxHeight) <= half }
        if (fitsSideBySide) {
            val placeables = measurables.map { it.measure(Constraints.fixedWidth(half)) }
            val height = placeables.maxOfOrNull { it.height } ?: 0
            layout(width, height) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(index * (half + space), (height - placeable.height) / 2)
                }
            }
        } else {
            val placeables = measurables.map { it.measure(Constraints.fixedWidth(width)) }
            val height = placeables.sumOf { it.height } + space * (placeables.size - 1).coerceAtLeast(0)
            layout(width, height) {
                var y = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(0, y)
                    y += placeable.height + space
                }
            }
        }
    }
}

/**
 * 아이콘과 글자만 있는 긴 이름의 동작 버튼. Kinetica 글자 버튼은 한 줄로만 그려 큰 글꼴에서 잘리므로,
 * 같은 누름 표시를 쓰되 글자가 넘치면 줄을 바꾼다.
 */
@Composable
internal fun WrappingTextButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = MaterialTheme.shapes.small
    val color = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .heightIn(min = ChageunTheme.spacing.minTouchTarget)
            .clip(shape)
            .kClickable(shape, enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = ChageunTheme.spacing.sm, vertical = ChageunTheme.spacing.xs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(ICON_SIZE))
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = ChageunTheme.spacing.xs),
        )
    }
}

private val ICON_SIZE = 18.dp
private const val DISABLED_ALPHA = 0.38f
