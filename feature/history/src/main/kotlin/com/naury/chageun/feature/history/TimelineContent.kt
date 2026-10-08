package com.naury.chageun.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.ads.NativeAdSlot
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.ui.EmptyState
import com.naury.chageun.core.ui.TimelineItemIcon
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.formatYearMonth
import com.naury.chageun.core.ui.timelineMileage
import com.naury.chageun.core.ui.timelineTitle

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TimelineContent(
    uiState: HistoryUiState,
    onKeywordChanged: (String) -> Unit,
    onFilterSelected: (HistoryFilter) -> Unit,
    onSelect: (RecordRef) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAdvancedFilter: () -> Unit = {},
    onClearAdvancedFilter: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    showFilters: Boolean = true,
) {
    val gutter = ChageunTheme.spacing.gutter
    // 끝에서 LOAD_MORE_AHEAD줄 전에 미리 다음 페이지를 읽어, 스크롤이 끝에서 멈추지 않게 한다.
    val isNearEnd by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisible >= layout.totalItemsCount - LOAD_MORE_AHEAD
        }
    }
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(isNearEnd, uiState.hasMore) {
        if (isNearEnd && uiState.hasMore) currentOnLoadMore()
    }
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(top = ChageunTheme.spacing.sm, bottom = FAB_CLEARANCE),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
    ) {
        // 세 칸 배치에서는 왼쪽 Pane이 검색과 필터를 맡는다.
        if (showFilters) {
            timelineFilters(uiState, onKeywordChanged, onFilterSelected, onOpenAdvancedFilter)
        }
        if (uiState.isEmpty) {
            item(key = "empty") {
                EmptyTimeline(
                    isFiltered = uiState.isFiltered,
                    onAdd = onAdd,
                    onClear = {
                        onFilterSelected(HistoryFilter.All)
                        onKeywordChanged("")
                        onClearAdvancedFilter()
                    },
                )
            }
        }
        // 기획서 17.2: 기록이 충분히 길 때만 8번째 기록 다음에 한 칸 둔다.
        val adAfter = uiState.sections.flatMap { it.items }.getOrNull(AD_AFTER_RECORDS - 1)?.ref
        uiState.sections.forEach { section ->
            stickyHeader(key = "month-${section.month}") {
                MonthHeader(section, Modifier.padding(horizontal = gutter))
            }
            itemsIndexed(section.items, key = { _, row -> "${row.ref.type}-${row.ref.id}" }) { index, item ->
                TimelineRow(
                    item = item,
                    isSelected = item.ref == uiState.selected,
                    isFirst = index == 0,
                    isLast = index == section.items.lastIndex,
                    onClick = { onSelect(item.ref) },
                    modifier = Modifier.padding(horizontal = gutter),
                )
                if (item.ref == adAfter) {
                    NativeAdSlot(Modifier.padding(start = gutter, end = gutter, top = ChageunTheme.spacing.xs))
                }
            }
        }
    }
}

private fun LazyListScope.timelineFilters(
    uiState: HistoryUiState,
    onKeywordChanged: (String) -> Unit,
    onFilterSelected: (HistoryFilter) -> Unit,
    onOpenAdvancedFilter: () -> Unit,
) {
    item(key = "search") {
        OutlinedTextField(
            value = uiState.keyword,
            onValueChange = onKeywordChanged,
            placeholder = { Text(stringResource(R.string.history_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ChageunTheme.spacing.gutter),
        )
    }
    item(key = "filters") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = ChageunTheme.spacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            item(key = "advanced") {
                val count = uiState.advanced.activeCount
                FilterChip(
                    selected = count > 0,
                    onClick = onOpenAdvancedFilter,
                    leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                    label = {
                        Text(
                            if (count > 0) {
                                stringResource(R.string.history_filter_count, count)
                            } else {
                                stringResource(R.string.history_filter)
                            },
                        )
                    },
                )
            }
            items(HistoryFilter.entries) { filter ->
                FilterChip(
                    selected = uiState.filter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(stringResource(filter.labelRes)) },
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(section: TimelineSection, modifier: Modifier = Modifier) {
    val total = section.totalWon
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .then(modifier)
            .padding(top = ChageunTheme.spacing.sm, bottom = ChageunTheme.spacing.xs)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = section.month?.let { formatYearMonth(it) } ?: stringResource(R.string.history_unknown_date),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (total > 0) {
            Text(
                stringResource(R.string.history_month_total, formatNumber(total)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 아이콘 사이를 세로선으로 이어 같은 달의 기록이 하나의 흐름으로 읽히게 한다. */
@Composable
private fun TimelineRow(
    item: TimelineItem,
    isSelected: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val gap = ChageunTheme.spacing.xs
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    val stroke = LINE_WIDTH.toPx()
                    // 행 사이 간격까지 이어 그려야 선이 끊기지 않는다.
                    val top = if (isFirst) size.height / 2 else -gap.toPx()
                    val bottom = if (isLast) size.height / 2 else size.height + gap.toPx()
                    drawLine(lineColor, Offset(x, top), Offset(x, bottom), stroke)
                },
            contentAlignment = Alignment.Center,
        ) {
            TimelineItemIcon(item)
        }
        Surface(
            modifier = Modifier
                .weight(1f)
                .semantics { selected = isSelected }
                .pressable(onClick = onClick),
            shape = MaterialTheme.shapes.medium,
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ) {
            Row(Modifier.padding(ChageunTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs)) {
                    // 좁은 카드에서 날짜·주행거리가 줄바꿈되어도 밀리지 않도록 첨부 표시는 제목 옆에 둔다.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            timelineTitle(item),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (item.attachmentCount > 0) AttachmentMark(item.attachmentCount)
                    }
                    val meta = listOfNotNull(
                        item.date?.let { formatDate(it) },
                        timelineMileage(item),
                    )
                    Text(
                        meta.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item.costWon?.let {
                    Text(
                        stringResource(R.string.history_won, formatNumber(it)),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

/** 사진·영수증이 붙은 기록임을 카드에서 바로 알 수 있게 한다. */
@Composable
private fun AttachmentMark(count: Int) {
    val description = pluralStringResource(R.plurals.history_attachment_count, count, count)
    Row(
        modifier = Modifier
            .padding(start = ChageunTheme.spacing.xs)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.AttachFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ATTACHMENT_ICON_SIZE),
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyTimeline(isFiltered: Boolean, onAdd: () -> Unit, onClear: () -> Unit) {
    if (isFiltered) {
        EmptyState(
            icon = Icons.Filled.SearchOff,
            title = stringResource(R.string.history_empty_filtered),
            body = stringResource(R.string.history_empty_filtered_body),
            actionLabel = stringResource(R.string.history_clear_filters),
            onAction = onClear,
        )
    } else {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.EventNote,
            title = stringResource(R.string.history_empty),
            body = stringResource(R.string.history_empty_body),
            tone = ChageunTheme.colors.good,
            actionLabel = stringResource(R.string.history_empty_action),
            onAction = onAdd,
        )
    }
}

internal val HistoryFilter.labelRes: Int
    get() = when (this) {
        HistoryFilter.All -> R.string.history_filter_all
        HistoryFilter.Maintenance -> R.string.history_filter_maintenance
        HistoryFilter.Fuel -> R.string.history_filter_fuel
        HistoryFilter.Check -> R.string.history_filter_check
        HistoryFilter.Other -> R.string.history_filter_other
        HistoryFilter.Mileage -> R.string.history_filter_mileage
    }

private val LINE_WIDTH = 2.dp

private val FAB_CLEARANCE = 88.dp

private const val AD_AFTER_RECORDS = 8

private val ATTACHMENT_ICON_SIZE = 14.dp

private const val LOAD_MORE_AHEAD = 10
