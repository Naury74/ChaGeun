package com.naury.chageun.feature.vehicle.album

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.naury.chageun.core.designsystem.component.PressStyle
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.rememberFileImage
import com.naury.chageun.feature.vehicle.R
import kotlin.math.absoluteValue

/** 사진 카드가 차지할 자리. 조작 막대가 보일 때는 위·아래·옆을 비우고 모서리를 둥글게 한다. */
@Immutable
internal data class CardFrame(val top: Dp, val bottom: Dp, val side: Dp, val radius: Dp)

/**
 * Pager의 한 페이지. 사진 비율에 맞춘 카드를 남은 자리 가운데에 두고, 옆 페이지는 조금 작고 흐리게 그려
 * 넘길 때 깊이가 느껴지게 한다. 지금 페이지에만 날짜·메모를 카드 아래쪽에 겹친다.
 */
@Composable
internal fun ViewerPage(
    photo: AlbumPhoto,
    frame: CardFrame,
    pageOffset: () -> Float,
    zoom: PhotoZoomState,
    showInfo: Boolean,
    info: @Composable () -> Unit,
    onTap: () -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(start = frame.side, end = frame.side, top = frame.top, bottom = frame.bottom),
        contentAlignment = Alignment.Center,
    ) {
        val aspect = rememberMediaAspect(photo)
        val containerAspect = if (maxHeight > 0.dp) maxWidth / maxHeight else 1f
        val size = if (aspect >= containerAspect) {
            Modifier.fillMaxWidth().aspectRatio(aspect)
        } else {
            Modifier.fillMaxHeight().aspectRatio(aspect)
        }
        Box(
            size
                .graphicsLayer {
                    val distance = pageOffset().absoluteValue.coerceIn(0f, 1f)
                    val scale = lerp(SIDE_PAGE_SCALE, 1f, 1f - distance)
                    scaleX = scale
                    scaleY = scale
                    alpha = lerp(SIDE_PAGE_ALPHA, 1f, 1f - distance)
                }
                .clip(RoundedCornerShape(frame.radius)),
        ) {
            ViewerPageContent(photo, zoom, onTap)
            AnimatedVisibility(
                visible = showInfo,
                enter = fadeIn(motionSpec()),
                exit = fadeOut(motionSpec()),
                modifier = Modifier.align(Alignment.BottomStart),
            ) {
                info()
            }
        }
    }
}

/**
 * 페이지에 그릴 내용. 지금은 사진만 있지만 앨범에 동영상이 들어오면 여기서 종류에 따라 나눈다.
 * 비율을 정하는 [rememberMediaAspect]도 같은 자리에서 함께 바꾼다.
 */
@Composable
private fun ViewerPageContent(photo: AlbumPhoto, zoom: PhotoZoomState, onTap: () -> Unit) {
    val image by rememberFileImage(photo.filePath)
    ZoomableImage(
        image = image,
        contentDescription = photo.comment ?: stringResource(R.string.album_photo_on, formatDate(photo.takenOn)),
        state = zoom,
        onTap = onTap,
    )
}

/** 카드 비율. 파노라마처럼 극단적인 비율은 카드가 실선처럼 납작해지지 않도록 자른다. 읽기 전에는 4:3으로 둔다. */
@Composable
private fun rememberMediaAspect(photo: AlbumPhoto): Float {
    val thumbnail by rememberFileImage(photo.thumbnailPath)
    val image = thumbnail ?: return DEFAULT_ASPECT
    if (image.width <= 0 || image.height <= 0) return DEFAULT_ASPECT
    return (image.width.toFloat() / image.height).coerceIn(MIN_ASPECT, MAX_ASPECT)
}

/** 카드 아래쪽에 겹치는 날짜와 메모. 날짜를 누르면 달력으로, 메모를 누르면 정보 수정으로 간다. */
@Composable
internal fun PhotoInfoOverlay(photo: AlbumPhoto, actions: AlbumActions) {
    val spacing = ChageunTheme.spacing
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, INFO_SCRIM_MID, INFO_SCRIM_END)))
            .padding(start = spacing.md, end = spacing.md, top = spacing.xl, bottom = spacing.xs),
    ) {
        PhotoDateChip(photo.takenOn, actions.onOpenCalendar)
        val editLabel = stringResource(R.string.album_edit_details)
        val note = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.minTouchTarget)
            .pressable(onClick = { actions.onEdit(photo.id) }, style = PressStyle.Highlight, onClickLabel = editLabel)
        if (photo.comment != null) {
            Box(note, contentAlignment = Alignment.CenterStart) {
                Text(
                    photo.comment.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    maxLines = NOTE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            // 메모가 없으면 쓰기 시작할 자리를 대신 보여 준다.
            Row(
                note,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.EditNote, contentDescription = null, tint = MUTED, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.album_write_note),
                    style = MaterialTheme.typography.labelLarge,
                    color = MUTED,
                )
            }
        }
    }
}

private const val SIDE_PAGE_SCALE = 0.92f
private const val SIDE_PAGE_ALPHA = 0.6f
private const val DEFAULT_ASPECT = 4f / 3f
private const val MIN_ASPECT = 0.42f
private const val MAX_ASPECT = 2.4f
private const val NOTE_MAX_LINES = 3
private val INFO_SCRIM_MID = Color.Black.copy(alpha = 0.30f)
private val INFO_SCRIM_END = Color.Black.copy(alpha = 0.72f)
private val MUTED = Color.White.copy(alpha = 0.8f)
