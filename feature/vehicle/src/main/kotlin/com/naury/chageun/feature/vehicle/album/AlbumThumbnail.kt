package com.naury.chageun.feature.vehicle.album

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.ui.rememberFileImage
import com.naury.chageun.feature.vehicle.R
import com.naury.kinetica.core.scene.NodeEffects
import com.naury.kinetica.core.scene.PressEffect
import com.naury.kinetica.core.scene.PressKind
import com.naury.kinetica.render.kineticaEffects
import com.naury.kinetica.ui.KSelectionBadge

/** 여러 장 고르기에서 칸의 상태. 고르는 중이 아니면 [None]이다. */
internal enum class PickState { None, Unpicked, Picked }

/**
 * 그리드 한 칸. 누르면 사진을 열고, 길게 누르면 진동과 함께 여러 장 고르기를 시작한다. 고르는 중에는 누를 때마다
 * 고르고 뺀다. 누르는 동안 살짝 작아지고, 두 칸 화면에서 열려 있는 사진은 테두리로 표시한다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AlbumThumbnail(
    photo: AlbumPhoto,
    isOpen: Boolean,
    pick: PickState,
    actions: AlbumActions,
    modifier: Modifier = Modifier,
) {
    val image by rememberFileImage(photo.thumbnailPath)
    val description = mediaDescription(photo)
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    // 고른 사진은 iOS 사진 앱처럼 안쪽으로 살짝 줄어 고르지 않은 사진과 구분된다.
    val pickedScale by animateFloatAsState(
        if (pick == PickState.Picked) PICKED_SCALE else 1f,
        motionSpec(),
        label = "thumbnail-picked",
    )
    val ring = MaterialTheme.colorScheme.primary
    val gap = MaterialTheme.colorScheme.background
    val showRing = (isOpen && pick == PickState.None) || focused
    Box(
        modifier
            .aspectRatio(1f)
            .kineticaEffects(TILE_PRESS, SHAPE)
            .clip(SHAPE)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Image,
                onClickLabel = description,
                onLongClickLabel = stringResource(R.string.album_pick),
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    actions.onTogglePick(photo.id)
                },
                onClick = {
                    if (pick ==
                        PickState.None
                    ) {
                        actions.onSelect(photo.id)
                    } else {
                        actions.onTogglePick(photo.id)
                    }
                },
            )
            .semantics { selected = isOpen || pick == PickState.Picked }
            // 열린 사진은 사진을 줄이지 않고 위에 테두리를 그린다. 안쪽의 얇은 바탕색 선이 어두운 사진과 테두리를 나눈다.
            .drawWithContent {
                drawContent()
                if (!showRing) return@drawWithContent
                val ringPx = RING_WIDTH.toPx()
                val corner = size.minDimension * CORNER_PERCENT / PERCENT
                drawRoundRect(
                    gap,
                    topLeft = Offset(ringPx, ringPx),
                    size = Size(size.width - ringPx * 2, size.height - ringPx * 2),
                    cornerRadius = CornerRadius(corner - ringPx),
                    style = Stroke(RING_GAP.toPx() * 2),
                )
                drawRoundRect(ring, cornerRadius = CornerRadius(corner), style = Stroke(ringPx * 2))
            },
    ) {
        image?.let {
            Image(
                it,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pickedScale
                        scaleY = pickedScale
                    }
                    .clip(SHAPE),
            )
        }
        if (photo.isVideo) {
            // 아주 작은 칸에서는 길이를 빼고 재생 표시만 남겨 사진을 덜 가린다.
            BoxWithConstraints(Modifier.matchParentSize(), contentAlignment = Alignment.BottomEnd) {
                VideoDurationBadge(
                    photo.durationMs,
                    Modifier.padding(5.dp),
                    showDuration = maxWidth >= MIN_WIDTH_FOR_DURATION,
                )
            }
        }
        if (pick != PickState.None) {
            // 칸 전체가 눌리므로 표시는 누를 수 없게 두고, 골랐는지만 썸네일 이름 뒤에 붙여 읽힌다.
            KSelectionBadge(
                selected = pick == PickState.Picked,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                size = MARK_SIZE,
            )
        }
    }
}

/**
 * 두 손가락으로 벌리면 [onStep]에 1을, 오므리면 -1을 보낸다. 한 번 잡는 동안에는 한 단계만 바꾼다.
 * 두 손가락일 때만 이벤트를 먼저 받아 소비하므로 한 손가락 스크롤과 사진 누르기는 그대로 동작한다.
 */
internal fun Modifier.pinchSteps(onStep: (Int) -> Unit): Modifier = pointerInput(onStep) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var total = 1f
        var isHandled = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } >= 2) {
                total *= event.calculateZoom()
                val step = when {
                    total > PINCH_OUT -> 1
                    total < PINCH_IN -> -1
                    else -> 0
                }
                if (!isHandled && step != 0) {
                    onStep(step)
                    isHandled = true
                }
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

// 기본 크기(약 110dp)에서 20dp 모서리가 되는 비율. 칸을 줄여도 동그랗게 변하지 않는다.
private const val CORNER_PERCENT = 18
private const val PERCENT = 100f
private val SHAPE = RoundedCornerShape(CORNER_PERCENT)
private val RING_WIDTH = 3.dp
private val RING_GAP = 2.dp
private val MARK_SIZE = 22.dp

// 누르는 동안 칸이 살짝 작아졌다가 튕기듯 돌아온다. 사진 위라 어둡게나 물결은 쓰지 않는다.
private val TILE_PRESS = NodeEffects(press = listOf(PressEffect(PressKind.Scale, amount = 0.95)))
private const val PICKED_SCALE = 0.86f
private const val PINCH_OUT = 1.25f
private const val PINCH_IN = 0.8f
private val MIN_WIDTH_FOR_DURATION = 72.dp
