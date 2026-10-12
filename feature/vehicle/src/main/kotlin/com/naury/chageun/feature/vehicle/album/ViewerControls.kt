package com.naury.chageun.feature.vehicle.album

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.feature.vehicle.R
import java.time.LocalDate

/** 사진 위에 떠 있는 위쪽 줄. 뒤로 버튼은 한 칸 화면에서만 두고, 몇 번째 사진인지는 늘 가운데에 둔다. */
@Composable
internal fun ViewerTopBar(index: Int, count: Int, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = ChageunTheme.spacing.xs, vertical = ChageunTheme.spacing.xs),
    ) {
        onBack?.let {
            FilledIconButton(
                onClick = it,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = GLASS, contentColor = Color.White),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(ChageunTheme.spacing.minTouchTarget),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.album_back))
            }
        }
        val description = stringResource(R.string.album_counter_description, index + 1, count)
        Text(
            stringResource(R.string.album_counter, index + 1, count),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .background(GLASS, CircleShape)
                .padding(horizontal = ChageunTheme.spacing.sm, vertical = ChageunTheme.spacing.xxs + 2.dp)
                .semantics { contentDescription = description },
        )
    }
}

/** 사진 보기 위쪽에 잠깐 떠오르는 알림. */
internal enum class ViewerNotice { CoverUpdated, EditFailed }

/** 대표 사진으로 바꿨거나 편집한 내용을 저장하지 못한 직후에만 잠깐 떠오르는 알림. TalkBack에도 바로 읽어 준다. */
@Composable
internal fun ViewerNoticeBadge(notice: ViewerNotice?, modifier: Modifier = Modifier) {
    // 사라지는 동안에도 마지막 문구를 그대로 보여 준다.
    val last = remember { mutableStateOf(notice) }
    if (notice != null) last.value = notice
    AnimatedVisibility(
        visible = notice != null,
        enter = fadeIn(motionSpec()) + scaleIn(motionSpec(), initialScale = BADGE_START_SCALE),
        exit = fadeOut(motionSpec()) + scaleOut(motionSpec(), targetScale = BADGE_START_SCALE),
        modifier = modifier,
    ) {
        val isError = last.value == ViewerNotice.EditFailed
        val tone = if (isError) ChageunTheme.colors.critical else ChageunTheme.colors.good
        Row(
            Modifier
                .background(tone.container, CircleShape)
                .padding(horizontal = ChageunTheme.spacing.sm, vertical = ChageunTheme.spacing.xs)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = tone.content,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(if (isError) R.string.album_edit_failed else R.string.album_cover_updated),
                style = MaterialTheme.typography.labelLarge,
                color = tone.content,
            )
        }
    }
}

/** 찍은 날. 달력으로 갈 수 있으면 누를 수 있는 알약 모양으로, 아니면 글자로만 보인다. */
@Composable
internal fun PhotoDateChip(date: LocalDate, onOpenCalendar: ((LocalDate) -> Unit)?, modifier: Modifier = Modifier) {
    val content = MaterialTheme.colorScheme.onSurface
    val clickable = onOpenCalendar?.let { open ->
        Modifier.pressable(onClick = { open(date) }, onClickLabel = stringResource(R.string.album_open_calendar))
    } ?: Modifier
    Box(modifier.heightIn(min = ChageunTheme.spacing.minTouchTarget).then(clickable), Alignment.CenterStart) {
        Row(
            Modifier
                .background(GLASS, CircleShape)
                .padding(
                    start = ChageunTheme.spacing.sm - 2.dp,
                    end = if (onOpenCalendar != null) ChageunTheme.spacing.xs else ChageunTheme.spacing.sm,
                    top = ChageunTheme.spacing.xxs + 2.dp,
                    bottom = ChageunTheme.spacing.xxs + 2.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Text(formatDate(date), style = MaterialTheme.typography.labelLarge, color = content)
            if (onOpenCalendar != null) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * 정보 수정·편집·공유·대표 사진·삭제. 둥근 아이콘 아래에 짧은 이름을 두고, TalkBack에는 하는 일을 길게 알려 준다.
 * 편집·공유는 편집기를 붙인 뒤에만 보이고, 대표 사진은 동영상에서는 보이지 않는다.
 */
@Composable
internal fun ViewerActionRow(photo: AlbumPhoto, actions: AlbumActions, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        ViewerAction(
            icon = Icons.Outlined.Info,
            label = stringResource(R.string.album_action_details),
            clickLabel = stringResource(R.string.album_edit_details),
            onClick = { actions.onEdit(photo.id) },
        )
        actions.onEditImage?.let { editImage ->
            ViewerAction(
                icon = Icons.Filled.Tune,
                label = stringResource(R.string.album_action_edit_image),
                clickLabel = stringResource(
                    if (photo.isVideo) R.string.album_edit_video else R.string.album_edit_image,
                ),
                onClick = { editImage(photo) },
            )
        }
        actions.onShare?.let { share ->
            ViewerAction(
                icon = Icons.Filled.Share,
                label = stringResource(R.string.album_action_share),
                clickLabel = stringResource(
                    if (photo.isVideo) R.string.album_share_video else R.string.album_share_photo,
                ),
                onClick = { share(photo) },
            )
        }
        // 대표 사진은 사진으로만 정한다. 동영상의 포스터는 작아서 대표 사진으로 쓰기에 흐리다.
        if (!photo.isVideo) {
            ViewerAction(
                icon = Icons.Filled.DirectionsCar,
                label = stringResource(R.string.album_action_cover),
                clickLabel = stringResource(R.string.album_set_cover),
                onClick = { actions.onSetCover(photo) },
            )
        }
        ViewerAction(
            icon = Icons.Filled.DeleteOutline,
            label = stringResource(R.string.album_delete),
            clickLabel = stringResource(
                if (photo.isVideo) R.string.album_delete_video_title else R.string.album_delete_title,
            ),
            onClick = { actions.onDelete(photo.id) },
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ViewerAction(
    icon: ImageVector,
    label: String,
    clickLabel: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        Modifier
            .widthIn(min = ACTION_MIN_WIDTH)
            .pressable(onClick = onClick, onClickLabel = clickLabel)
            .padding(vertical = ChageunTheme.spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs + 2.dp),
    ) {
        Box(
            Modifier
                .size(ACTION_CIRCLE)
                .background(GLASS, CircleShape)
                .border(Dp.Hairline, Color.White.copy(alpha = EDGE_ALPHA), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint, textAlign = TextAlign.Center)
    }
}

/** 흐린 사진 배경 위에 뜨는 버튼 바탕. 밝은 사진 위에서도 흰 아이콘이 읽히도록 검정을 반쯤 깐다. */
internal val GLASS = Color.Black.copy(alpha = 0.38f)
private const val EDGE_ALPHA = 0.14f
private const val BADGE_START_SCALE = 0.8f
private val ACTION_MIN_WIDTH = 72.dp
private val ACTION_CIRCLE = 48.dp
