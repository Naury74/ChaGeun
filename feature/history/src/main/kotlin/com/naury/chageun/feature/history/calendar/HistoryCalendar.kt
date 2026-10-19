package com.naury.chageun.feature.history.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.naury.chageun.core.ui.currentSeparatingHinge
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.feature.history.R
import com.naury.kinetica.ui.KLoadingIndicator

/** 달력 칸들이 함께 쓰는 값. [mode]는 화면 배치에 맞춘 기본값까지 정해진 보기다. */
private class CalendarParts(
    val state: CalendarUiState,
    val calendar: CalendarMonth,
    val mode: CalendarMode,
    val actions: CalendarActions,
    val onPickItems: () -> Unit,
)

/**
 * 달력 보기의 배치.
 * - 휴대폰: 달력 아래에 고른 날 목록을 한 번에 스크롤한다. 목록을 위로 밀면 주 보기로 접히고, 주 보기 달력은
 *   위에 붙어 남는다. 맨 위에서 더 당기면 월 보기로 펼쳐진다. 기록을 열면 상세가 화면을 채운다.
 * - 탁자 자세: 위 칸에 달력, 아래 칸에 목록. 위 칸에 다 들어가도록 기본은 주 보기다.
 * - 두 칸(840dp 이상·책 자세): 왼쪽 달력, 오른쪽 목록 또는 기록 상세. 세로 접힘은 피한다. 월/주는 버튼으로만 바꾼다.
 * - 세 칸(1600dp 이상): 왼쪽에 필터·월 요약을 따로 두고 달력과 목록·상세를 나란히 둔다.
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
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { KLoadingIndicator() }
        return
    }
    BackHandler(enabled = detail != null) { onCloseDetail() }
    val isTabletop = hinge != null && !hinge.isVertical
    var isPickingItems by rememberSaveable { mutableStateOf(false) }
    val parts = CalendarParts(
        state = state,
        calendar = calendar,
        mode = state.mode ?: if (isTabletop) CalendarMode.Week else CalendarMode.Month,
        actions = actions,
        onPickItems = { isPickingItems = true },
    )
    val agendaOrDetail: @Composable () -> Unit = {
        if (detail != null) {
            detailPane(detail, Modifier.fillMaxSize())
        } else {
            CalendarLazyColumn(Modifier.fillMaxSize()) { calendarAgenda(state, actions) }
        }
    }
    when {
        isThreePane && !isTabletop -> ThreePaneListDetail(
            selected = detail,
            supporting = { CalendarSidePane(parts, Modifier.fillMaxSize()) },
            list = { CalendarLazyColumn(Modifier.fillMaxSize()) { calendarItems(parts, showFilters = false) } },
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
            CalendarLazyColumn(Modifier.fillMaxSize()) { calendarItems(parts) }
            Box(Modifier.fillMaxSize()) { agendaOrDetail() }
        }
        detail != null -> detailPane(detail, modifier.fillMaxSize())
        else -> PhoneCalendar(parts, modifier.fillMaxSize())
    }
    if (isPickingItems) {
        CalendarItemSheet(
            selected = state.filter.items,
            isExpanded = isTwoPane,
            onApply = actions.onSetItems,
            onDismiss = { isPickingItems = false },
        )
    }
}

@Composable
private fun PhoneCalendar(parts: CalendarParts, modifier: Modifier) {
    val collapse = rememberCalendarCollapse(parts.mode, parts.actions.onSetMode)
    CalendarLazyColumn(modifier.nestedScroll(collapse)) {
        calendarItems(parts, isPhone = true)
        calendarAgenda(parts.state, parts.actions)
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

/**
 * 머리글·검색·필터·월 요약·그리드. 세 칸에서는 필터와 요약을 왼쪽 칸으로 옮긴다.
 * 휴대폰 주 보기에서는 그리드를 목록 위에 붙여 두어 목록을 내려도 고른 주가 보인다.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.calendarItems(parts: CalendarParts, showFilters: Boolean = true, isPhone: Boolean = false) {
    item(key = "calendar-header") {
        Column(Modifier.padding(horizontal = GRID_PADDING)) {
            CalendarHeader(parts.calendar.month, parts.mode, parts.state.search.isOpen, parts.actions)
            Column(
                Modifier.padding(horizontal = ChageunTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs),
            ) {
                CalendarSearchControls(parts.state, parts.actions)
                if (showFilters) CalendarFilters(parts)
            }
        }
    }
    val grid: @Composable () -> Unit = {
        // 좁은 휴대폰에서도 칸이 48dp를 넘도록 그리드 좌우 여백은 화면 여백보다 좁게 둔다.
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = GRID_PADDING),
        ) {
            CalendarGrid(parts.state, parts.calendar, parts.mode, parts.actions)
            CalendarModeBar(parts.mode, showHandle = isPhone, onSetMode = parts.actions.onSetMode)
        }
    }
    if (isPhone && parts.mode == CalendarMode.Week) {
        stickyHeader(key = "calendar-grid") { grid() }
    } else {
        item(key = "calendar-grid") { grid() }
    }
}

@Composable
private fun CalendarFilters(parts: CalendarParts) {
    MonthSummary(parts.state.monthSummary(parts.calendar))
    CalendarFilterChips(parts.state.filter, parts.actions.onToggleKind, parts.onPickItems)
}

/** 세 칸 배치의 왼쪽 칸. 종류·항목 필터를 고르고, 이번 달 요약을 숫자로 보여 준다. */
@Composable
private fun CalendarSidePane(parts: CalendarParts, modifier: Modifier = Modifier) {
    val summary = parts.state.monthSummary(parts.calendar)
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
        Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
            Text(
                stringResource(R.string.calendar_side_filters),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(top = ChageunTheme.spacing.xs)
                    .semantics { heading() },
            )
            CalendarFilterChips(parts.state.filter, parts.actions.onToggleKind, parts.onPickItems)
        }
        CardGroup(stringResource(R.string.calendar_side_summary)) {
            InfoListRow(
                Icons.AutoMirrored.Filled.EventNote,
                stringResource(R.string.calendar_section_records),
                pluralStringResource(R.plurals.calendar_summary_records, summary.recordCount, summary.recordCount),
            )
            GroupDivider()
            InfoListRow(
                Icons.Filled.PhotoLibrary,
                stringResource(R.string.calendar_kind_photos),
                // 사진도 동영상도 없으면 "사진 0장"으로 둔다.
                mediaCountParts(summary).joinToString(" · ")
                    .ifEmpty { pluralStringResource(R.plurals.calendar_summary_photos, 0, 0) },
            )
            GroupDivider()
            InfoListRow(
                Icons.Filled.Payments,
                stringResource(R.string.calendar_side_spent),
                stringResource(R.string.history_won, formatNumber(summary.totalWon)),
            )
        }
    }
}

private val GRID_PADDING = 8.dp

// 세 칸에서는 달력 칸이 목록·상세보다 넓어야 날짜 칸에 사진 미리보기가 들어간다.
private const val THREE_PANE_CALENDAR_FRACTION = 0.55f
