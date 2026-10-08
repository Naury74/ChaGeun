package com.naury.chageun.core.ui

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import kotlin.math.roundToInt

/** 창을 나누는 접힘 부분이나 디스플레이 사이 틈이다. 나누는 축 방향의 창 픽셀 좌표로 나타낸다. */
data class Hinge(val start: Float, val end: Float, val isVertical: Boolean)

/**
 * 콘텐츠가 가로질러서는 안 되는 Hinge다. 반쯤 펼친 폴드(Book 또는 Tabletop)와 듀얼 디스플레이가 해당한다.
 * 완전히 펼친 폴드는 아무것도 보고하지 않으므로 일반 가중치 레이아웃을 그대로 쓴다.
 */
@Composable
fun currentSeparatingHinge(): Hinge? = currentWindowAdaptiveInfo().windowPosture.hingeList
    .firstOrNull { it.isSeparating || it.isOccluding }
    ?.let { hinge ->
        val bounds = hinge.bounds
        if (hinge.isVertical) {
            Hinge(bounds.left, bounds.right, isVertical = true)
        } else {
            Hinge(bounds.top, bounds.bottom, isVertical = false)
        }
    }

/** Expanded 폭 이상인 창. 입력 시트를 Bottom Sheet 대신 Dialog로 띄울지 정할 때 쓴다. */
@Composable
fun isExpandedWidth(): Boolean = currentWindowAdaptiveInfo().windowSizeClass
    .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

/** 목록-상세 화면은 Expanded 폭부터, 또는 세로 Hinge가 창을 나눌 때마다 Pane 두 개를 쓴다. */
@Composable
fun isListDetailTwoPane(): Boolean = isExpandedWidth() || currentSeparatingHinge()?.isVertical == true

/**
 * Large 폭(1200dp)부터는 필터 Pane을 더해 세 칸을 쓴다. 세로 Hinge가 창을 나누면 칸이 접힘에 걸치지 않도록
 * 두 칸 배치를 그대로 쓴다.
 */
@Composable
fun isListDetailThreePane(): Boolean =
    isListDetailThreePane(currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass) &&
        currentSeparatingHinge()?.isVertical != true

// 1200dp부터는 왼쪽 고정 메뉴가 360dp 가까이 차지해, Large 폭에서 세 칸으로 나누면 가운데 목록이 너무 좁다.
fun isListDetailThreePane(windowSizeClass: WindowSizeClass): Boolean =
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND)

/**
 * 각 자식을 Pane으로 배치한다. 나란히 놓거나, [stacked]이면 위아래로 쌓는다.
 * 그 축을 가로지르는 Hinge가 없으면 Pane들이 [weights] 비율로 공간을 나눈다. Hinge가 있으면 첫 Pane은
 * Hinge 앞에서 끝나고 나머지는 Hinge 뒤에서 시작해, 접힘 부분에 카드나 버튼이 걸치지 않는다.
 */
@Composable
fun HingeAwarePanes(
    weights: List<Float>,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
    gap: Dp = ChageunTheme.spacing.paneGap,
    hinge: Hinge? = currentSeparatingHinge(),
    content: @Composable () -> Unit,
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val splitting = hinge?.takeIf { it.isVertical != stacked }
    Layout(
        content = content,
        modifier = modifier.onPlaced { origin = it.positionInWindow() },
    ) { measurables, constraints ->
        val total = if (stacked) constraints.maxHeight else constraints.maxWidth
        val shift = if (stacked) origin.y else origin.x
        val slots = paneSlots(
            total = total,
            gap = gap.roundToPx(),
            weights = weights.take(measurables.size),
            hingeStart = splitting?.let { (it.start - shift).roundToInt() },
            hingeEnd = splitting?.let { (it.end - shift).roundToInt() },
        )
        val placeables = measurables.zip(slots) { measurable, slot ->
            measurable.measure(
                if (stacked) {
                    Constraints.fixed(constraints.maxWidth, slot.size)
                } else {
                    Constraints.fixed(slot.size, constraints.maxHeight)
                },
            )
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.zip(slots) { placeable, slot ->
                if (stacked) placeable.place(0, slot.offset) else placeable.place(slot.offset, 0)
            }
        }
    }
}

internal data class PaneSlot(val offset: Int, val size: Int)

/**
 * [total] 픽셀을 Pane들로 나눈다. [gap]보다 좁은 Hinge는 양쪽에 여백을 더해 Pane이 최소 [gap]만큼
 * 떨어지게 한다. 레이아웃 밖에 있는 Hinge는 무시한다.
 */
internal fun paneSlots(total: Int, gap: Int, weights: List<Float>, hingeStart: Int?, hingeEnd: Int?): List<PaneSlot> {
    val hingeInside = hingeStart != null && hingeEnd != null && hingeStart > 0 && hingeEnd < total
    if (!hingeInside || weights.size < 2) return weighted(0, total, gap, weights)
    val padding = ((gap - (hingeEnd - hingeStart)) / 2).coerceAtLeast(0)
    val firstEnd = hingeStart - padding
    val restStart = hingeEnd + padding
    return listOf(PaneSlot(0, firstEnd)) + weighted(restStart, total - restStart, gap, weights.drop(1))
}

private fun weighted(start: Int, length: Int, gap: Int, weights: List<Float>): List<PaneSlot> {
    if (weights.isEmpty()) return emptyList()
    val available = (length - gap * (weights.size - 1)).coerceAtLeast(0)
    val sum = weights.sum()
    var offset = start
    return weights.mapIndexed { index, weight ->
        val size = if (index == weights.lastIndex) start + length - offset else (available * weight / sum).toInt()
        PaneSlot(offset, size.coerceAtLeast(0)).also { offset += size + gap }
    }
}
