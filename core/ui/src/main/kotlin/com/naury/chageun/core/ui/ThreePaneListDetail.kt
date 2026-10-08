package com.naury.chageun.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import kotlin.math.roundToInt

/**
 * 아주 넓은 창의 필터-목록-상세. 왼쪽 [supporting]은 고정 폭, 남은 폭을 목록과 상세가 [listFraction] 비율로
 * 나눈다. 칸이 충분하므로 두 칸 배치처럼 목록을 넓혔다 줄이지 않고, 선택 전에는 상세 자리에 [emptyDetail]을 둔다.
 * 목록은 항상 같은 자리에 있어 상세를 바꿔 열어도 스크롤 위치가 그대로다.
 *
 * Hinge가 창을 나누는 경우는 다루지 않는다. 그때는 [AdaptiveListDetail]을 쓴다.
 */
@Composable
fun <T : Any> ThreePaneListDetail(
    selected: T?,
    supporting: @Composable () -> Unit,
    list: @Composable () -> Unit,
    detail: @Composable (T) -> Unit,
    emptyDetail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingWidth: Dp = DEFAULT_SUPPORTING_WIDTH,
    listFraction: Float = DEFAULT_LIST_FRACTION,
    gap: Dp = ChageunTheme.spacing.paneGap,
) {
    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            Box(Modifier.fillMaxSize()) { supporting() }
            Box(Modifier.fillMaxSize()) { list() }
            Box(Modifier.fillMaxSize()) { if (selected != null) detail(selected) else emptyDetail() }
        },
    ) { measurables, constraints ->
        val total = constraints.maxWidth
        val height = constraints.maxHeight
        val gapPx = gap.roundToPx()
        val supportingPx = supportingWidth.roundToPx().coerceAtMost(total)
        val rest = (total - supportingPx - gapPx * 2).coerceAtLeast(0)
        val listPx = (rest * listFraction).roundToInt()
        val detailPx = rest - listPx
        val placeables = listOf(supportingPx, listPx, detailPx).mapIndexed { index, width ->
            measurables[index].measure(Constraints.fixed(width, height))
        }
        layout(total, height) {
            var x = 0
            placeables.forEach { placeable ->
                placeable.place(x, 0)
                x += placeable.width + gapPx
            }
        }
    }
}

// 필터 이름과 개수가 한 줄에 들어가면서 목록·상세 몫을 너무 빼앗지 않는 폭.
private val DEFAULT_SUPPORTING_WIDTH = 296.dp

private const val DEFAULT_LIST_FRACTION = 0.42f
