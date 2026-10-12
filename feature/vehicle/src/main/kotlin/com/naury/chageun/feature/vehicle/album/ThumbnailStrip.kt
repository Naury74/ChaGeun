package com.naury.chageun.feature.vehicle.album

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.motion.ChageunMotion
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.motion.rememberReduceMotion
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.rememberFileImage
import com.naury.chageun.feature.vehicle.R

/**
 * 사진 보기 아래의 작은 사진 줄. 지금 사진은 조금 크게 흰 테두리로 표시하고, 넘길 때마다 가운데로 따라온다.
 * 처음 열 때는 왼쪽부터 차례로 펼쳐진다.
 */
@Composable
internal fun ThumbnailStrip(
    photos: List<AlbumPhoto>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0))
    val currentPx = with(LocalDensity.current) { CURRENT_SIZE.roundToPx() }
    LaunchedEffect(currentIndex) {
        if (currentIndex !in photos.indices) return@LaunchedEffect
        val viewport = listState.layoutInfo.viewportSize.width
        listState.animateScrollToItem(currentIndex, scrollOffset = -(viewport - currentPx) / 2)
    }
    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .height(CURRENT_SIZE),
        contentPadding = PaddingValues(horizontal = ChageunTheme.spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(ITEM_GAP, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
            StripThumbnail(
                photo = photo,
                position = index + 1,
                count = photos.size,
                isCurrent = index == currentIndex,
                appearDelayMs = (index - currentIndex).coerceIn(0, MAX_STAGGERED) * ChageunMotion.STAGGER_MS / 2,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun StripThumbnail(
    photo: AlbumPhoto,
    position: Int,
    count: Int,
    isCurrent: Boolean,
    appearDelayMs: Int,
    onClick: () -> Unit,
) {
    val image by rememberFileImage(photo.thumbnailPath)
    val description = stringResource(
        R.string.album_strip_photo,
        position,
        count,
        photo.comment ?: stringResource(R.string.album_photo_on, formatDate(photo.takenOn)),
    )
    val size by animateDpAsState(if (isCurrent) CURRENT_SIZE else ITEM_SIZE, motionSpec(), label = "strip-size")
    val appear = rememberAppear(appearDelayMs)
    Box(
        Modifier
            .graphicsLayer {
                alpha = appear.value
                val scale = APPEAR_START_SCALE + (1f - APPEAR_START_SCALE) * appear.value
                scaleX = scale
                scaleY = scale
            }
            .size(size)
            .clip(SHAPE)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (isCurrent) Modifier.border(RING_WIDTH, Color.White, SHAPE) else Modifier)
            .selectable(selected = isCurrent, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        image?.let {
            Image(
                it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (isCurrent) 1f else DIMMED_ALPHA },
            )
        }
    }
}

/** 처음 그려질 때 한 번만 0에서 1로 오르는 값. Reduce Motion이면 바로 1이다. */
@Composable
private fun rememberAppear(delayMs: Int): Animatable<Float, *> {
    val reduceMotion = rememberReduceMotion()
    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val spec = motionSpec<Float>(delayMs = delayMs)
    LaunchedEffect(Unit) { appear.animateTo(1f, spec) }
    return appear
}

private val ITEM_SIZE = 50.dp
private val CURRENT_SIZE = 60.dp
private val ITEM_GAP = 6.dp
private val RING_WIDTH = 2.5.dp
private val SHAPE = RoundedCornerShape(10.dp)
private const val DIMMED_ALPHA = 0.7f
private const val APPEAR_START_SCALE = 0.85f
private const val MAX_STAGGERED = 8
