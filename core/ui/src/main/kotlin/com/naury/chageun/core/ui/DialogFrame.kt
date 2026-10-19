package com.naury.chageun.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naury.kinetica.ui.KDialog
import com.naury.kinetica.ui.KSheet
import com.naury.kinetica.ui.rememberKWindowLayout

/**
 * [ChageunDialog]와 날짜·지역 선택 창이 함께 쓰는 틀. 폰에서는 아래에서 올라오는 Kinetica 시트(뒤 화면이 조금 물러나고
 * 손잡이나 내용을 끌어 닫음), 가로 600dp 이상 창에서는 가운데 카드다. 열린 채 접거나 펴면 그 자리에서 모양이 바뀐다.
 *
 * 별도 Dialog 창에 그리므로 TalkBack 초점이 창 안에 머물고, 테스트에서도 `isDialog()`로 찾을 수 있다.
 * [content]에는 닫는 애니메이션을 시작하는 함수를 넘기고, 애니메이션이 끝나면 [onDismiss]를 부른다. 높이는 화면에서
 * 상태 표시줄과 위 여백을 뺀 만큼으로 제한하므로, 안쪽에서 `weight(1f, fill = false)`로 남은 높이를 나눠 쓴다.
 *
 * @param horizontalPadding 판 가장자리에서 내용까지의 여백. 판이 기본으로 두는 24dp보다 작으면 그만큼 내용을 넓힌다.
 */
@Composable
internal fun DialogFrame(
    paneTitle: String,
    onDismiss: () -> Unit,
    dismissible: Boolean,
    horizontalPadding: Dp,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val close: () -> Unit = { visible = false }
    val request: () -> Unit = { if (dismissible) close() }
    val body: @Composable ColumnScope.() -> Unit = {
        // 별도 창에서는 Material Surface가 없어 글자 색이 검정으로 돌아가므로 판 위 글자 색을 다시 정한다.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .bleed(PanelPadding - horizontalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { content(close) }
        }
    }
    if (rememberKWindowLayout().isCompact) {
        KSheet(
            visible = visible,
            onDismissRequest = request,
            title = paneTitle,
            onDismissed = { latestOnDismiss() },
            dragToDismiss = dismissible,
            content = body,
        )
    } else {
        KDialog(
            visible = visible,
            onDismissRequest = request,
            modifier = Modifier.fillMaxWidth(),
            title = paneTitle,
            onDismissed = { latestOnDismiss() },
            appearScale = CARD_START_SCALE,
            content = body,
        )
    }
}

/** 판의 좌우 여백을 [amount]만큼 덜어 내용을 판 가장자리 쪽으로 넓힌다. 0 이하면 그대로 둔다. */
internal fun Modifier.bleed(amount: Dp): Modifier = if (amount <= 0.dp) {
    this
} else {
    layout { measurable, constraints ->
        val extra = (amount * 2).roundToPx()
        val wider = if (constraints.hasBoundedWidth) {
            constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra)
        } else {
            constraints
        }
        val placeable = measurable.measure(wider)
        layout((placeable.width - extra).coerceAtLeast(0), placeable.height) { placeable.place(-amount.roundToPx(), 0) }
    }
}

/** Kinetica 시트와 카드가 내용 둘레에 두는 기본 여백. */
internal val PanelPadding = 24.dp

// 누른 자리에서 튀어나오기보다 거의 제 크기로 나타나게 한다. 예전 차근 카드와 같은 값이다.
private const val CARD_START_SCALE = 0.94f
