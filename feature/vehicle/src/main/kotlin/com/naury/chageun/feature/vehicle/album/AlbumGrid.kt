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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.feature.vehicle.R

/**
 * 찍은 날짜별 사진 그리드. 두 손가락으로 벌리면 칸이 커지고 오므리면 작아진다. 칸 크기는 단계로만 바뀌어
 * 넓은 창에서는 같은 단계에서도 한 줄에 더 많이 들어간다.
 */
@Composable
internal fun AlbumGrid(
    uiState: AlbumUiState,
    actions: AlbumActions,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
) {
    val gutter = ChageunTheme.spacing.gutter
    LazyVerticalGrid(
        columns = GridCells.Adaptive(THUMBNAIL_MIN_SIZES[uiState.gridStep.coerceIn(THUMBNAIL_MIN_SIZES.indices)]),
        state = state,
        modifier = modifier.pinchSteps(actions.onZoomGrid),
        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = ChageunTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(GRID_GAP),
        verticalArrangement = Arrangement.spacedBy(GRID_GAP),
    ) {
        if (uiState.failedCount > 0) {
            item(key = "failed", span = { GridItemSpan(maxLineSpan) }) { FailedRow(uiState.failedCount, actions) }
        }
        uiState.days.forEach { day ->
            item(key = "day-${day.date}", span = { GridItemSpan(maxLineSpan) }) {
                DayHeader(day, uiState.isPicking, isDayPicked = day.photos.all { it.id in uiState.picked }, actions)
            }
            items(day.photos, key = { it.id }) { photo ->
                AlbumThumbnail(
                    photo = photo,
                    isOpen = photo.id == uiState.selected?.id,
                    pick = when {
                        !uiState.isPicking -> PickState.None
                        photo.id in uiState.picked -> PickState.Picked
                        else -> PickState.Unpicked
                    },
                    actions = actions,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun FailedRow(count: Int, actions: AlbumActions) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            pluralStringResource(R.plurals.album_failed, count, count),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = actions.onDismissMessage) { Text(stringResource(R.string.album_close)) }
    }
}

/** 기록 탭의 월 머리글처럼 날짜는 왼쪽에, 장 수는 오른쪽에 회색으로 둔다. 고르는 중에는 그날 전체 선택 버튼이 된다. */
@Composable
private fun DayHeader(day: AlbumDay, isPicking: Boolean, isDayPicked: Boolean, actions: AlbumActions) {
    Row(
        Modifier
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
            TextButton(onClick = { actions.onPickDay(day.date) }) {
                Text(stringResource(if (isDayPicked) R.string.album_unpick_day else R.string.album_pick_day))
            }
        } else {
            Text(
                pluralStringResource(R.plurals.album_day_count, day.photos.size, day.photos.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// 휴대폰 폭에서 한 줄에 6·5·4·3·2장이 들어가는 크기. 넓은 창에서는 같은 단계에서 더 많이 들어간다.
private val THUMBNAIL_MIN_SIZES = listOf(50.dp, 62.dp, 78.dp, 104.dp, 160.dp)
private val GRID_GAP = 4.dp
