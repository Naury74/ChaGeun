package com.naury.chageun.feature.manage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme

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
