package com.naury.chageun.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

/**
 * 입력 화면이다. Compact 창에서는 Bottom Sheet로, Sheet가 너무 넓어지는 창에서는 폭을 제한한 Dialog로 띄운다.
 *
 * 반쯤 접힌 폴드에서는 접히는 선을 가로지르지 않게 한쪽 화면에만 띄운다. 책처럼 세워 들면 목록 옆 오른쪽에,
 * 탁자에 놓으면(Tabletop) 손이 닿는 아래쪽에 둔다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheet(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    hinge: Hinge? = currentSeparatingHinge(),
    content: @Composable () -> Unit,
) {
    when {
        hinge != null -> HingeSideDialog(hinge, onDismiss, content)
        isExpanded -> Dialog(onDismissRequest = onDismiss) {
            Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.widthIn(max = MAX_DIALOG_WIDTH)) {
                content()
            }
        }
        else -> ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) { content() }
    }
}

@Composable
private fun HingeSideDialog(hinge: Hinge, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Hinge 좌표는 앱 창 기준이고 Dialog는 따로 창을 띄우므로, 화면 위치로 맞춰 접히는 선을 찾는다.
        var origin by remember { mutableFloatStateOf(0f) }
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    origin =
                        if (hinge.isVertical) it.positionOnScreen().x else it.positionOnScreen().y
                }
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null,
                    onClick = onDismiss,
                ),
        ) {
            Layout(
                content = {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        // 바깥을 누르면 닫히지만 창 안을 누른 것은 닫기로 이어지지 않게 막는다.
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                    ) { content() }
                },
            ) { measurables, constraints ->
                val gap = HINGE_GAP.roundToPx()
                val hingeEnd = (hinge.end - origin).roundToInt().coerceIn(
                    0,
                    maxOf(constraints.maxWidth, constraints.maxHeight),
                )
                // 접히는 선 뒤쪽(오른쪽 또는 아래쪽) 칸 안에서만 그린다.
                val areaX = if (hinge.isVertical) hingeEnd + gap else gap
                val areaY = if (hinge.isVertical) gap else hingeEnd + gap
                val areaWidth = (constraints.maxWidth - areaX - gap).coerceAtLeast(0)
                val areaHeight = (constraints.maxHeight - areaY - gap).coerceAtLeast(0)
                val placeable = measurables.single().measure(
                    Constraints(maxWidth = minOf(areaWidth, MAX_DIALOG_WIDTH.roundToPx()), maxHeight = areaHeight),
                )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(
                        areaX + (areaWidth - placeable.width) / 2,
                        if (hinge.isVertical) areaY + (areaHeight - placeable.height) / 2 else areaY,
                    )
                }
            }
        }
    }
}

private val MAX_DIALOG_WIDTH = 560.dp
private val HINGE_GAP = 16.dp
