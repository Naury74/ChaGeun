package com.naury.chageun.feature.vehicle.album

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * 사진 보기의 확대 상태. 화면 좌표 `s`와 확대 전 좌표 `l`은 `s = c + (l - c) * zoom + pan`이다(`c`는 영역 중심).
 * 확대된 사진이 영역 밖으로 빠져 검은 빈 곳이 보이지 않게 [pan]을 제한한다.
 */
@Stable
internal class PhotoZoomState {
    var zoom by mutableFloatStateOf(1f)
        private set
    var pan by mutableStateOf(Offset.Zero)
        private set

    val isZoomed: Boolean get() = zoom > ZOOMED_THRESHOLD

    /** 손가락 중심 아래의 사진이 그대로 머물도록 확대하고 옮긴다. */
    fun transform(zoomChange: Float, panChange: Offset, centroid: Offset, size: Size) {
        val next = (zoom * zoomChange).coerceIn(1f, MAX_ZOOM)
        val center = size.center
        val anchored = (centroid - center) - (centroid - center - pan) * (next / zoom)
        zoom = next
        pan = clamp(anchored + panChange, next, size)
    }

    /** 확대한 사진이 이미 왼쪽·오른쪽 끝까지 와 있는데 그쪽으로 더 미는 중이면 true. */
    fun isAtHorizontalEdge(panChange: Offset, size: Size): Boolean {
        if (!isZoomed || abs(panChange.x) <= abs(panChange.y)) return false
        val maxX = (zoom - 1f) * size.width / 2f
        return (panChange.x > 0f && pan.x >= maxX - EDGE_SLOP_PX) || (panChange.x < 0f && pan.x <= -maxX + EDGE_SLOP_PX)
    }

    fun reset() {
        zoom = 1f
        pan = Offset.Zero
    }

    /** 확대되어 있으면 원래 크기로, 아니면 누른 곳을 중심으로 [DOUBLE_TAP_ZOOM]배로 부드럽게 바꾼다. */
    suspend fun toggleAt(point: Offset, size: Size) {
        val targetZoom = if (isZoomed) 1f else DOUBLE_TAP_ZOOM
        val targetPan = if (isZoomed) {
            Offset.Zero
        } else {
            clamp((size.center - point) * (DOUBLE_TAP_ZOOM - 1f), DOUBLE_TAP_ZOOM, size)
        }
        animateTo(targetZoom, targetPan)
    }

    suspend fun animateTo(targetZoom: Float, targetPan: Offset) {
        val startZoom = zoom
        val startPan = pan
        animate(0f, 1f, animationSpec = tween(ANIMATION_MS, easing = FastOutSlowInEasing)) { fraction, _ ->
            zoom = startZoom + (targetZoom - startZoom) * fraction
            pan = startPan + (targetPan - startPan) * fraction
        }
    }

    private fun clamp(value: Offset, zoom: Float, size: Size): Offset {
        val maxX = (zoom - 1f) * size.width / 2f
        val maxY = (zoom - 1f) * size.height / 2f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    companion object {
        const val MAX_ZOOM = 5f
        const val DOUBLE_TAP_ZOOM = 2.5f
        private const val ZOOMED_THRESHOLD = 1.01f
        private const val ANIMATION_MS = 260
        private const val EDGE_SLOP_PX = 0.5f
    }
}

/**
 * 두 손가락으로 확대하고, 확대한 동안에는 한 손가락으로 옮긴다. 확대하지 않은 한 손가락 끌기는 소비하지 않아
 * 바깥 Pager가 다음 사진으로 넘긴다. 한 번 탭은 [onTap], 두 번 탭은 확대·되돌리기다.
 */
@Composable
internal fun ZoomableImage(
    image: ImageBitmap?,
    contentDescription: String?,
    state: PhotoZoomState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val currentOnTap by rememberUpdatedState(onTap)
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(state) {
                detectTapGestures(
                    onTap = { currentOnTap() },
                    onDoubleTap = { point -> scope.launch { state.toggleAt(point, size.toSize()) } },
                )
            }
            .pointerInput(state) { detectZoomAndPan(state) },
        contentAlignment = Alignment.Center,
    ) {
        image?.let {
            Image(
                it,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = state.zoom
                        scaleY = state.zoom
                        translationX = state.pan.x
                        translationY = state.pan.y
                    },
            )
        }
    }
}

/**
 * 두 손가락이거나 이미 확대한 상태에서만 이벤트를 소비한다. 확대한 사진을 한 손가락으로 옮기다 가장자리에 닿은 채
 * 더 밀면 그 제스처는 끝까지 Pager에 넘겨 다음 사진으로 넘어가게 한다.
 */
private suspend fun PointerInputScope.detectZoomAndPan(state: PhotoZoomState) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var handedOff = false
        do {
            val event = awaitPointerEvent()
            val fingers = event.changes.count { it.pressed }
            val centroid = event.calculateCentroid(useCurrent = false)
            val pan = event.calculatePan()
            if (fingers == 1 && state.isAtHorizontalEdge(pan, size.toSize())) handedOff = true
            val handles = !handedOff && !event.changes.any { it.isConsumed } && (fingers >= 2 || state.isZoomed)
            if (handles && centroid.isSpecified) {
                state.transform(event.calculateZoom(), pan, centroid, size.toSize())
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}
