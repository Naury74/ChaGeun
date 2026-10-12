package com.naury.chageun.feature.history.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.formatYearMonth
import com.naury.chageun.feature.history.R
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
        IconButton(onClick = { if (isWeek) actions.onMoveWeek(-1) else actions.onShowMonth(month.minusMonths(1)) }) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(
                    if (isWeek) R.string.calendar_previous_week else R.string.calendar_previous_month,
                ),
            )
        }
        Text(
            formatYearMonth(month),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        IconButton(onClick = { if (isWeek) actions.onMoveWeek(1) else actions.onShowMonth(month.plusMonths(1)) }) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(
                    if (isWeek) R.string.calendar_next_week else R.string.calendar_next_month,
                ),
            )
        }
        if (!isSearchOpen) {
            IconButton(onClick = actions.onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.calendar_search))
            }
        }
        TextButton(onClick = actions.onToday, modifier = Modifier.heightIn(min = ChageunTheme.spacing.minTouchTarget)) {
            Text(stringResource(R.string.calendar_today))
        }
    }
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
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            actions.onSearch(it)
        },
        placeholder = { Text(stringResource(R.string.history_search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = actions.onCloseSearch) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.calendar_search_close))
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
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
            IconButton(onClick = actions.onPreviousResult, enabled = state.previousResultDate != null) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.calendar_previous_result),
                )
            }
            IconButton(onClick = actions.onNextResult, enabled = state.nextResultDate != null) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.calendar_next_result))
            }
        }
        TextButton(
            onClick = actions.onClearAll,
            modifier = Modifier.heightIn(min = ChageunTheme.spacing.minTouchTarget),
        ) {
            Text(stringResource(R.string.calendar_clear_all))
        }
    }
}

/** 이번 달 지출·기록 수·사진 수. 필터가 있으면 보이는 것만 센다. */
@Composable
internal fun MonthSummary(summary: MonthSummaryValues, modifier: Modifier = Modifier) {
    val parts = listOfNotNull(
        stringResource(R.string.history_month_total, formatNumber(summary.totalWon)).takeIf { summary.totalWon > 0 },
        pluralStringResource(R.plurals.calendar_summary_records, summary.recordCount, summary.recordCount),
        pluralStringResource(R.plurals.calendar_summary_photos, summary.photoCount, summary.photoCount),
    )
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
            FilterChip(
                selected = selected,
                onClick = { onToggle(kind) },
                label = { Text(stringResource(kind.labelRes)) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(CHIP_ICON_SIZE)) }
                } else {
                    null
                },
            )
        }
        ItemsChip(filter.items.size, onPickItems)
    }
}

@Composable
private fun ItemsChip(itemCount: Int, onPickItems: () -> Unit) {
    FilterChip(
        selected = itemCount > 0,
        onClick = onPickItems,
        label = {
            Text(
                if (itemCount > 0) {
                    pluralStringResource(R.plurals.calendar_items_count, itemCount, itemCount)
                } else {
                    stringResource(R.string.calendar_pick_items)
                },
            )
        },
        leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(CHIP_ICON_SIZE)) },
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

private val CHIP_ICON_SIZE = 18.dp
