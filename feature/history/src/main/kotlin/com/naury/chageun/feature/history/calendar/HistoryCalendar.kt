package com.naury.chageun.feature.history.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.CalendarMonth
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.Hinge
import com.naury.chageun.core.ui.HingeAwarePanes
import com.naury.chageun.core.ui.InfoListRow
import com.naury.chageun.core.ui.ThreePaneListDetail
import com.naury.chageun.core.ui.ToggleListRow
import com.naury.chageun.core.ui.currentSeparatingHinge
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.feature.history.R

/**
 * 달력 보기의 배치.
 * - 휴대폰: 달력 아래에 고른 날 목록을 한 번에 스크롤한다. 기록을 열면 상세가 화면을 채운다.
 * - 탁자 자세: 위 칸에 달력, 아래 칸에 목록.
 * - 두 칸(840dp 이상·책 자세): 왼쪽 달력, 오른쪽 목록 또는 기록 상세. 세로 접힘은 피한다.
 * - 세 칸(1600dp 이상): 왼쪽에 레이어·월 요약을 따로 두고 달력과 목록·상세를 나란히 둔다.
 */
@Composable
internal fun HistoryCalendar(
    state: CalendarUiState,
    actions: CalendarActions,
    detail: RecordDetail?,
    detailPane: @Composable (RecordDetail, Modifier) -> Unit,
    onCloseDetail: () -> Unit,
    isTwoPane: Boolean,
    isThreePane: Boolean,
    modifier: Modifier = Modifier,
    hinge: Hinge? = currentSeparatingHinge(),
) {
    val calendar = state.calendar
    if (state.isLoading || calendar == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    BackHandler(enabled = detail != null) { onCloseDetail() }
    val agendaOrDetail: @Composable () -> Unit = {
        if (detail != null) {
            detailPane(detail, Modifier.fillMaxSize())
        } else {
            CalendarLazyColumn(Modifier.fillMaxSize()) { calendarAgenda(state, actions) }
        }
    }
    val isTabletop = hinge != null && !hinge.isVertical
    when {
        isThreePane && !isTabletop -> ThreePaneListDetail(
            selected = detail,
            supporting = { CalendarSidePane(state, calendar, actions, Modifier.fillMaxSize()) },
            list = {
                CalendarLazyColumn(Modifier.fillMaxSize()) {
                    calendarItems(state, calendar, actions, showControls = false)
                }
            },
            detail = { detailPane(it, Modifier.fillMaxSize()) },
            emptyDetail = { CalendarLazyColumn(Modifier.fillMaxSize()) { calendarAgenda(state, actions) } },
            listFraction = THREE_PANE_CALENDAR_FRACTION,
            modifier = modifier,
        )
        isTabletop || isTwoPane -> HingeAwarePanes(
            weights = listOf(1f, 1f),
            modifier = modifier.fillMaxSize(),
            stacked = isTabletop,
            hinge = hinge,
        ) {
            CalendarLazyColumn(Modifier.fillMaxSize()) { calendarItems(state, calendar, actions) }
            Box(Modifier.fillMaxSize()) { agendaOrDetail() }
        }
        detail != null -> detailPane(detail, modifier.fillMaxSize())
        else -> CalendarLazyColumn(modifier.fillMaxSize()) {
            calendarItems(state, calendar, actions)
            calendarAgenda(state, actions)
        }
    }
}

@Composable
private fun CalendarLazyColumn(modifier: Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = ChageunTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        content = content,
    )
}

/** 머리글·레이어 칩·월 요약·그리드. 세 칸에서는 칩과 요약을 왼쪽 칸으로 옮긴다. */
private fun LazyListScope.calendarItems(
    state: CalendarUiState,
    calendar: CalendarMonth,
    actions: CalendarActions,
    showControls: Boolean = true,
) {
    item(key = "calendar-header") {
        Column(Modifier.padding(horizontal = GRID_PADDING)) {
            CalendarHeader(calendar.month, actions.onShowMonth, actions.onToday)
            if (showControls) {
                Column(
                    Modifier.padding(horizontal = ChageunTheme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs),
                ) {
                    MonthSummary(calendar)
                    CalendarLayerChips(state.layers, actions.onToggleLayer)
                }
            }
        }
    }
    item(key = "calendar-grid") {
        // 좁은 휴대폰에서도 칸이 48dp를 넘도록 그리드 좌우 여백은 화면 여백보다 좁게 둔다.
        MonthGrid(
            state = state,
            calendar = calendar,
            onSelect = actions.onSelectDate,
            onShowMonth = actions.onShowMonth,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GRID_PADDING),
        )
    }
}

/** 세 칸 배치의 왼쪽 칸. 레이어를 켜고 끄며, 이번 달 요약을 숫자로 보여 준다. */
@Composable
private fun CalendarSidePane(
    state: CalendarUiState,
    calendar: CalendarMonth,
    actions: CalendarActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(
                start = ChageunTheme.spacing.gutter,
                top = ChageunTheme.spacing.sm,
                bottom = ChageunTheme.spacing.lg,
            ),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
    ) {
        CardGroup(stringResource(R.string.calendar_side_layers)) {
            CalendarLayer.entries.forEachIndexed { index, layer ->
                if (index > 0) GroupDivider()
                ToggleListRow(
                    icon = layer.icon,
                    title = stringResource(layer.labelRes),
                    body = null,
                    checked = layer in state.layers,
                    onCheckedChange = { actions.onToggleLayer(layer) },
                )
            }
        }
        CardGroup(stringResource(R.string.calendar_side_summary)) {
            InfoListRow(
                Icons.AutoMirrored.Filled.EventNote,
                stringResource(R.string.calendar_layer_records),
                pluralStringResource(R.plurals.calendar_summary_records, calendar.recordCount, calendar.recordCount),
            )
            GroupDivider()
            InfoListRow(
                Icons.Filled.PhotoLibrary,
                stringResource(R.string.calendar_layer_photos),
                pluralStringResource(R.plurals.calendar_summary_photos, calendar.photoCount, calendar.photoCount),
            )
            GroupDivider()
            InfoListRow(
                Icons.Filled.Payments,
                stringResource(R.string.calendar_side_spent),
                stringResource(R.string.history_won, formatNumber(calendar.monthTotalWon)),
            )
        }
    }
}

private val CalendarLayer.icon: ImageVector
    get() = when (this) {
        CalendarLayer.Records -> Icons.AutoMirrored.Filled.EventNote
        CalendarLayer.Photos -> Icons.Filled.PhotoLibrary
        CalendarLayer.Upcoming -> Icons.Filled.EventRepeat
    }

private val GRID_PADDING = 8.dp

// 세 칸에서는 달력 칸이 목록·상세보다 넓어야 날짜 칸에 사진 미리보기가 들어간다.
private const val THREE_PANE_CALENDAR_FRACTION = 0.55f
