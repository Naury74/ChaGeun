package com.naury.chageun.feature.history.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.formatYearMonth
import com.naury.chageun.feature.history.R
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KFilterChip
import com.naury.kinetica.ui.KIconButton
import com.naury.kinetica.ui.KSearchBar
import com.naury.kinetica.ui.KTone
import com.naury.kinetica.ui.KVariant
import java.time.YearMonth

/**
 * 달 이름과 앞뒤 이동, 검색, 오늘로 돌아가기. 주 보기에서는 화살표가 주를 옮기고, 달 이름은 고른 날이 있는 달을 따른다.
 */
@Composable
internal fun CalendarHeader(
    month: YearMonth,
    mode: CalendarMode,
    isSearchOpen: Boolean,
    actions: CalendarActions,
    modifier: Modifier = Modifier,
) {
    val isWeek = mode == CalendarMode.Week
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HeaderIconButton(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            stringResource(if (isWeek) R.string.calendar_previous_week else R.string.calendar_previous_month),
        ) { if (isWeek) actions.onMoveWeek(-1) else actions.onShowMonth(month.minusMonths(1)) }
        Text(
            formatYearMonth(month),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        HeaderIconButton(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            stringResource(if (isWeek) R.string.calendar_next_week else R.string.calendar_next_month),
        ) { if (isWeek) actions.onMoveWeek(1) else actions.onShowMonth(month.plusMonths(1)) }
        if (!isSearchOpen) {
            HeaderIconButton(
                Icons.Filled.Search,
                stringResource(R.string.calendar_search),
                onClick = actions.onOpenSearch,
            )
        }
        KButton(stringResource(R.string.calendar_today), onClick = actions.onToday, variant = KVariant.Text)
    }
}

/** 머리글의 화살표·검색처럼 바탕 없이 아이콘만 보이는 버튼. */
@Composable
private fun HeaderIconButton(icon: ImageVector, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    KIconButton(
        icon,
        contentDescription = description,
        onClick = onClick,
        variant = KVariant.Text,
        tone = KTone.Neutral,
        enabled = enabled,
    )
}

/**
 * 검색칸과 필터·검색 요약 줄. 요약 줄에는 "필터 2개 · ‘오일’ · 검색 결과 3건"과 결과가 있는 앞뒤 날짜로 가는
 * 버튼, [모두 지우기]를 둔다. 결과가 다른 달에 있으면 그 달로 넘어간다.
 */
@Composable
internal fun CalendarSearchControls(state: CalendarUiState, actions: CalendarActions, modifier: Modifier = Modifier) {
    if (!state.search.isOpen && !state.isFilteredOrSearching) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs)) {
        if (state.search.isOpen) SearchField(state.search.keyword, actions)
        if (state.isFilteredOrSearching) ActiveFilterSummary(state, actions)
    }
}

/**
 * 입력하는 글자는 화면에 먼저 반영하고 검색어는 따로 넘긴다. 저장소를 거쳐 돌아온 검색어로 칸을 다시 그리면
 * 빠르게 입력할 때 커서가 튄다.
 */
@Composable
private fun SearchField(keyword: String, actions: CalendarActions) {
    var text by rememberSaveable { mutableStateOf(keyword) }
    val focusRequester = remember { FocusRequester() }
    // 검색 버튼으로 열었을 때 바로 입력할 수 있게 한다. 회전 뒤 다시 그릴 때는 이미 입력한 글자가 있어 건너뛴다.
    LaunchedEffect(Unit) { if (text.isEmpty()) focusRequester.requestFocus() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // 닫기 버튼이 따로 있어 검색창의 '취소'는 두지 않는다. 지우기 버튼은 글자가 있을 때 나온다.
        KSearchBar(
            query = text,
            onQueryChange = {
                text = it
                actions.onSearch(it)
            },
            onSearch = actions.onSearch,
            placeholder = stringResource(R.string.history_search_hint),
            showCancel = false,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
        HeaderIconButton(
            Icons.Filled.Close,
            stringResource(R.string.calendar_search_close),
            onClick = actions.onCloseSearch,
        )
    }
}

@Composable
private fun ActiveFilterSummary(state: CalendarUiState, actions: CalendarActions) {
    val count = state.filter.activeCount
    val search = state.search
    val results = state.searchResults.size
    val parts = listOfNotNull(
        pluralStringResource(R.plurals.calendar_filter_count, count, count).takeIf { count > 0 },
        stringResource(R.string.calendar_filter_keyword, search.keyword.trim()).takeIf { search.isActive },
        pluralStringResource(R.plurals.calendar_search_results, results, results).takeIf { search.isActive },
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            parts.joinToString(" · "),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (search.isActive) {
            HeaderIconButton(
                Icons.Filled.KeyboardArrowUp,
                stringResource(R.string.calendar_previous_result),
                enabled = state.previousResultDate != null,
                onClick = actions.onPreviousResult,
            )
            HeaderIconButton(
                Icons.Filled.KeyboardArrowDown,
                stringResource(R.string.calendar_next_result),
                enabled = state.nextResultDate != null,
                onClick = actions.onNextResult,
            )
        }
        KButton(stringResource(R.string.calendar_clear_all), onClick = actions.onClearAll, variant = KVariant.Text)
    }
}

/** 이번 달 지출·기록 수·사진 수·동영상 수. 필터가 있으면 보이는 것만 센다. 0인 사진·동영상은 빼고 말한다. */
@Composable
internal fun MonthSummary(summary: MonthSummaryValues, modifier: Modifier = Modifier) {
    val parts = listOfNotNull(
        stringResource(R.string.history_month_total, formatNumber(summary.totalWon)).takeIf { summary.totalWon > 0 },
        pluralStringResource(R.plurals.calendar_summary_records, summary.recordCount, summary.recordCount),
    ) + mediaCountParts(summary)
    Text(
        parts.joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * 종류 칩과 [항목 선택]. 아무것도 고르지 않으면 모두 보인다. 옆으로 미는 한 줄로 두면 화면 끝에 반쯤 걸린 칩의
 * 터치 영역이 작아지므로 줄을 바꿔 모두 보이게 한다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalendarFilterChips(
    filter: CalendarFilter,
    onToggle: (CalendarKind) -> Unit,
    onPickItems: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        CalendarKind.entries.forEach { kind ->
            val selected = kind in filter.kinds
            KFilterChip(
                label = stringResource(kind.labelRes),
                selected = selected,
                onSelectedChange = { onToggle(kind) },
                minTouchTarget = true,
            )
        }
        ItemsChip(filter.items.size, onPickItems)
    }
}

@Composable
private fun ItemsChip(itemCount: Int, onPickItems: () -> Unit) {
    // 켜고 끄지 않고 항목 고르기 시트를 연다. 켜짐은 고른 항목이 있다는 표시다.
    KFilterChip(
        label = if (itemCount > 0) {
            pluralStringResource(R.plurals.calendar_items_count, itemCount, itemCount)
        } else {
            stringResource(R.string.calendar_pick_items)
        },
        selected = itemCount > 0,
        onSelectedChange = { onPickItems() },
        leadingIcon = Icons.Filled.Tune,
        minTouchTarget = true,
    )
}

internal val CalendarKind.labelRes: Int
    get() = when (this) {
        CalendarKind.Maintenance -> R.string.calendar_kind_maintenance
        CalendarKind.Fuel -> R.string.calendar_kind_fuel
        CalendarKind.CheckRepair -> R.string.calendar_kind_check
        CalendarKind.Mileage -> R.string.calendar_kind_mileage
        CalendarKind.Photos -> R.string.calendar_kind_photos
        CalendarKind.Upcoming -> R.string.calendar_kind_upcoming
    }

/** "사진 3장", "동영상 1개"처럼 0이 아닌 것만 나눠 센다. */
@Composable
internal fun mediaCountParts(summary: MonthSummaryValues): List<String> = listOfNotNull(
    summary.photoCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.calendar_summary_photos, it, it) },
    summary.videoCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.calendar_summary_videos, it, it) },
)
