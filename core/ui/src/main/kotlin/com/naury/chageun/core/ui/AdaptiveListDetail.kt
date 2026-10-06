package com.naury.chageun.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.naury.chageun.core.designsystem.motion.ChageunMotion
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import kotlin.math.roundToInt

/**
 * 넓은 창의 목록-상세. 선택 전에는 목록이 전체 너비를 쓰고, 선택하면 목록이 [listFraction]까지 줄어들며
 * 상세가 오른쪽에서 밀려 들어온다. 상세는 처음부터 최종 너비로 그려 두고 위치만 옮기므로 들어오는 동안
 * 내용이 다시 줄바꿈되지 않는다.
 *
 * 반쯤 접힌 폴드처럼 Hinge가 창을 세로로 나누면 내용이 접힘을 가로지르지 않도록 고정 두 칸을 쓴다.
 */
@Composable
fun <T : Any> AdaptiveListDetail(
    selected: T?,
    list: @Composable () -> Unit,
    detail: @Composable (T) -> Unit,
    emptyDetail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    listFraction: Float = DEFAULT_LIST_FRACTION,
    gap: Dp = ChageunTheme.spacing.paneGap,
    hinge: Hinge? = currentSeparatingHinge(),
) {
    if (hinge?.isVertical == true) {
        HingeAwarePanes(listOf(listFraction, 1f - listFraction), modifier.fillMaxSize(), gap = gap, hinge = hinge) {
            Box(Modifier.fillMaxSize()) { list() }
            Box(Modifier.fillMaxSize()) { if (selected != null) detail(selected) else emptyDetail() }
        }
        return
    }
    // 닫히는 동안에도 마지막 상세를 그려야 밀려 나가는 모습이 보인다.
    var shown by remember { mutableStateOf(selected) }
    if (selected != null && selected != shown) shown = selected
    val fraction by animateFloatAsState(
        targetValue = if (selected != null) listFraction else 1f,
        animationSpec = motionSpec(TRANSITION_MS),
        label = "list-detail",
        finishedListener = { if (it == 1f) shown = null },
    )
    val progress = ((1f - fraction) / (1f - listFraction)).coerceIn(0f, 1f)
    Layout(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        content = {
            Box(Modifier.fillMaxSize()) { list() }
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress }) { shown?.let { detail(it) } }
        },
    ) { measurables, constraints ->
        val total = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val listWidth = (total * fraction).roundToInt().coerceAtMost(total)
        val detailWidth = (total * (1f - listFraction)).roundToInt() - gapPx
        val listPlaceable = measurables[0].measure(Constraints.fixed(listWidth, constraints.maxHeight))
        val detailPlaceable = measurables[1].measure(
            Constraints.fixed(detailWidth.coerceAtLeast(0), constraints.maxHeight),
        )
        layout(total, constraints.maxHeight) {
            listPlaceable.place(0, 0)
            if (progress > 0f) detailPlaceable.place(listWidth + gapPx, 0)
        }
    }
}

private const val DEFAULT_LIST_FRACTION = 0.42f

// 목록 재배치와 상세 진입이 한 번에 읽히도록 일반 상태 전환보다 조금 길게 둔다.
private const val TRANSITION_MS = ChageunMotion.MEDIUM_MS + 120
