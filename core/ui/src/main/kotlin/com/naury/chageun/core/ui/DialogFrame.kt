package com.naury.chageun.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.naury.chageun.core.designsystem.motion.ChageunMotion
import com.naury.chageun.core.designsystem.motion.rememberReduceMotion
import kotlinx.coroutines.launch

/**
 * [ChageunDialog]와 날짜 선택 창이 함께 쓰는 틀. 폰에서는 아래에서 올라오는 시트, 넓은 창에서는 가운데 카드다.
 *
 * 시스템 Dialog 창 안에 그리므로 TalkBack 초점이 창 안에 머물고, 테스트에서도 `isDialog()`로 찾을 수 있다.
 * [content]에는 닫는 애니메이션을 시작하는 함수를 넘긴다. 높이는 화면에서 상태 표시줄과 위 여백을 뺀 만큼으로
 * 제한하므로, 안쪽에서 `weight(1f, fill = false)`로 남은 높이를 나눠 쓴다.
 */
@Composable
internal fun DialogFrame(
    paneTitle: String,
    onDismiss: () -> Unit,
    dismissible: Boolean,
    horizontalPadding: Dp,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberReduceMotion()
    // 0은 닫힘, 1은 열림. 시트 위치, 배경 어둡기, 카드 크기가 모두 이 값을 따른다.
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    // 손으로 끌어 내린 거리(px). 놓았을 때 충분히 내렸으면 닫고, 아니면 제자리로 돌아간다.
    val drag = remember { Animatable(0f) }
    var isClosing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { progress.animateTo(1f, spring(OPEN_DAMPING, Spring.StiffnessMediumLow)) }
    val close: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            scope.launch {
                if (!reduceMotion) progress.animateTo(0f, tween(ChageunMotion.SHORT_MS, easing = FastOutSlowInEasing))
                onDismiss()
            }
        }
    }
    Dialog(
        onDismissRequest = { if (dismissible) close() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        // 시스템 기본 어둡게 하기는 애니메이션이 없어 끄고, 직접 그린 배경을 시트와 함께 움직인다.
        val view = LocalView.current
        SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }
        val closeLabel = stringResource(R.string.dialog_close)
        val paneModifier = Modifier.semantics {
            this.paneTitle = paneTitle
            if (dismissible) {
                dismiss(closeLabel) {
                    close()
                    true
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress.value }
                    .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                    // 바깥 누르기는 손가락으로 닫는 방법이다. TalkBack에서는 창의 닫기 동작과 뒤로 가기로 닫는다.
                    .pointerInput(dismissible) { if (dismissible) detectTapGestures { close() } },
            )
            if (maxWidth >= WIDE_BREAKPOINT) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime))
                        .padding(CARD_MARGIN),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        paneModifier
                            .widthIn(max = CARD_MAX_WIDTH)
                            .graphicsLayer {
                                val scale = CARD_START_SCALE + (1f - CARD_START_SCALE) * progress.value
                                scaleX = scale
                                scaleY = scale
                                alpha = progress.value
                            }
                            .shadow(SHADOW, MaterialTheme.shapes.extraLarge)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(MaterialTheme.colorScheme.surface)
                            .blockClicks()
                            .padding(horizontal = horizontalPadding)
                            .padding(top = CARD_TOP_PADDING, bottom = BOTTOM_PADDING),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { content(close) }
                }
            } else {
                val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val density = LocalDensity.current
                Column(
                    paneModifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .heightIn(max = maxHeight - statusBar - SHEET_TOP_GAP)
                        .graphicsLayer {
                            // 열릴 때는 아래에서 올라오고, 끄는 동안은 손가락을 따라 내려간다.
                            translationY = (1f - progress.value) * size.height + drag.value
                        }
                        .shadow(SHADOW, SheetShape)
                        .clip(SheetShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .blockClicks()
                        .draggable(
                            enabled = dismissible,
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                scope.launch { drag.snapTo((drag.value + delta).coerceAtLeast(0f)) }
                            },
                            onDragStopped = { velocity ->
                                val threshold = with(density) { DISMISS_DRAG.toPx() }
                                if (drag.value > threshold || velocity > DISMISS_VELOCITY) {
                                    close()
                                } else {
                                    drag.animateTo(0f, spring(dampingRatio = RETURN_DAMPING))
                                }
                            },
                        )
                        // 키보드가 올라오면 시트 안쪽이 그만큼 줄어 입력 칸과 버튼이 키보드 위에 남는다.
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                        .padding(horizontal = horizontalPadding)
                        .padding(top = HANDLE_TOP_PADDING, bottom = BOTTOM_PADDING),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 끌어서 닫을 수 있다는 손잡이. 같은 동작은 TalkBack의 닫기 동작으로도 할 수 있어 읽지 않는다.
                    Box(
                        Modifier
                            .width(HANDLE_WIDTH)
                            .height(HANDLE_HEIGHT)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HANDLE_ALPHA),
                                CircleShape,
                            ),
                    )
                    Spacer(Modifier.height(HANDLE_GAP))
                    content(close)
                }
            }
        }
    }
}

/**
 * 창 안을 누른 것이 바깥 배경의 닫기로 이어지지 않게 막는다. clickable을 쓰면 창 전체가 TalkBack에서
 * 하나의 버튼으로 묶이므로 semantics 없이 누름만 받는다.
 */
private fun Modifier.blockClicks(): Modifier = pointerInput(Unit) { detectTapGestures {} }

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val WIDE_BREAKPOINT = 600.dp
private val CARD_MAX_WIDTH = 440.dp
private val CARD_MARGIN = 24.dp
private val CARD_TOP_PADDING = 28.dp
private val HANDLE_TOP_PADDING = 10.dp
private val BOTTOM_PADDING = 20.dp
private val HANDLE_WIDTH = 36.dp
private val HANDLE_HEIGHT = 5.dp
private val HANDLE_GAP = 18.dp
private val SHEET_TOP_GAP = 48.dp
private val SHADOW = 24.dp
private val DISMISS_DRAG = 96.dp
private const val DISMISS_VELOCITY = 1800f
private const val SCRIM_ALPHA = 0.45f
private const val HANDLE_ALPHA = 0.35f
private const val CARD_START_SCALE = 0.94f
private const val OPEN_DAMPING = 0.86f
private const val RETURN_DAMPING = 0.8f
