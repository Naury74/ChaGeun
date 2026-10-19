package com.naury.chageun.feature.vehicle.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.LocalBottomBarPadding
import com.naury.chageun.core.domain.album.MAX_ALBUM_VIDEO_BYTES
import com.naury.chageun.core.domain.album.MAX_ALBUM_VIDEO_DURATION_MS
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.feature.vehicle.R
import com.naury.chageun.feature.vehicle.TextAction
import com.naury.kinetica.ui.KStaggerState
import com.naury.kinetica.ui.staggerIn

/**
 * 찍은 날짜별 사진 그리드. 두 손가락으로 벌리면 칸이 커지고 오므리면 작아진다. 칸 크기는 단계로만 바뀌어
 * 넓은 창에서는 같은 단계에서도 한 줄에 더 많이 들어간다.
 * [stagger]를 주면 처음 열 때 사진이 위에서부터 차례로 떠오른다. 사진 보기를 열었다 닫아도 다시 떠오르지 않게 바깥에서 기억한다.
 */
@Composable
internal fun AlbumGrid(
    uiState: AlbumUiState,
    actions: AlbumActions,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    stagger: KStaggerState? = null,
) {
    val gutter = ChageunTheme.spacing.gutter
    LazyVerticalGrid(
        columns = GridCells.Adaptive(THUMBNAIL_MIN_SIZES[uiState.gridStep.coerceIn(THUMBNAIL_MIN_SIZES.indices)]),
        state = state,
        modifier = modifier.pinchSteps(actions.onZoomGrid),
        contentPadding = PaddingValues(
            start = gutter,
            end = gutter,
            bottom =
            ChageunTheme.spacing.lg + LocalBottomBarPadding.current,
        ),
        horizontalArrangement = Arrangement.spacedBy(GRID_GAP),
        verticalArrangement = Arrangement.spacedBy(GRID_GAP),
    ) {
        if (uiState.failedCount > 0 || uiState.tooLargeCount > 0) {
            item(key = "failed", span = { GridItemSpan(maxLineSpan) }) {
                FailedRow(uiState.failedCount, uiState.tooLargeCount, actions)
            }
        }
        // 머리글과 사진에 위에서부터 순번을 매긴다. 날짜가 바뀌어도 한 줄로 이어 센다.
        var order = 0
        uiState.days.forEach { day ->
            val headerOrder = order++
            item(key = "day-${day.date}", span = { GridItemSpan(maxLineSpan) }) {
                DayHeader(
                    day,
                    uiState.isPicking,
                    isDayPicked = day.photos.all { it.id in uiState.picked },
                    actions,
                    Modifier.appear(headerOrder, stagger),
                )
            }
            val firstOrder = order
            order += day.photos.size
            itemsIndexed(day.photos, key = { _, photo -> photo.id }) { index, photo ->
                AlbumThumbnail(
                    photo = photo,
                    isOpen = photo.id == uiState.selected?.id,
                    pick = when {
                        !uiState.isPicking -> PickState.None
                        photo.id in uiState.picked -> PickState.Picked
                        else -> PickState.Unpicked
                    },
                    actions = actions,
                    modifier = Modifier
                        .animateItem()
                        .appear(firstOrder + index, stagger),
                )
            }
        }
    }
}

/** 넣지 못한 사진·동영상 안내. 읽지 못한 것과 너무 길거나 큰 동영상은 할 수 있는 일이 달라 따로 적는다. */
@Composable
private fun FailedRow(failed: Int, tooLarge: Int, actions: AlbumActions) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val lines = listOfNotNull(
            pluralStringResource(R.plurals.album_failed, failed, failed).takeIf { failed > 0 },
            pluralStringResource(
                R.plurals.album_video_too_large,
                tooLarge,
                tooLarge,
                MAX_ALBUM_VIDEO_DURATION_MS / MILLIS_PER_MINUTE,
                MAX_ALBUM_VIDEO_BYTES / BYTES_PER_MEGABYTE,
            ).takeIf { tooLarge > 0 },
        )
        Text(
            lines.joinToString("\n"),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        TextAction(stringResource(R.string.album_close), actions.onDismissMessage)
    }
}

/** 기록 탭의 월 머리글처럼 날짜는 왼쪽에, 장 수는 오른쪽에 회색으로 둔다. 고르는 중에는 그날 전체 선택 버튼이 된다. */
@Composable
private fun DayHeader(
    day: AlbumDay,
    isPicking: Boolean,
    isDayPicked: Boolean,
    actions: AlbumActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = ChageunTheme.spacing.minTouchTarget)
            .padding(top = ChageunTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatDate(day.date),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (isPicking) {
            TextAction(
                stringResource(if (isDayPicked) R.string.album_unpick_day else R.string.album_pick_day),
                { actions.onPickDay(day.date) },
            )
        } else {
            // 동영상이 섞인 날은 "장" 대신 "개"로 센다.
            val count = if (day.photos.any {
                    it.isVideo
                }
            ) {
                R.plurals.album_day_count_media
            } else {
                R.plurals.album_day_count
            }
            Text(
                pluralStringResource(count, day.photos.size, day.photos.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Modifier.appear(order: Int, stagger: KStaggerState?): Modifier =
    if (stagger == null) this else staggerIn(order, stagger)

// 휴대폰 폭에서 한 줄에 6·5·4·3·2장이 들어가는 크기. 넓은 창에서는 같은 단계에서 더 많이 들어간다.
private val THUMBNAIL_MIN_SIZES = listOf(50.dp, 62.dp, 78.dp, 104.dp, 160.dp)
private val GRID_GAP = 4.dp
private const val MILLIS_PER_MINUTE = 60_000L
private const val BYTES_PER_MEGABYTE = 1_024L * 1_024
