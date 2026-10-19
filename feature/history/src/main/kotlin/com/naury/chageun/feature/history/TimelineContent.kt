package com.naury.chageun.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.ads.NativeAdSlot
import com.naury.chageun.core.designsystem.component.ChageunCard
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.LocalBottomBarPadding
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.ui.TimelineItemIcon
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.formatYearMonth
import com.naury.chageun.core.ui.timelineMileage
import com.naury.chageun.core.ui.timelineTitle
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KChipRow
import com.naury.kinetica.ui.KEmptyIcon
import com.naury.kinetica.ui.KEmptyIconKind
import com.naury.kinetica.ui.KEmptyState
import com.naury.kinetica.ui.KFilterChip
import com.naury.kinetica.ui.KMenuItem
import com.naury.kinetica.ui.KSearchBar
import com.naury.kinetica.ui.KVariant
import com.naury.kinetica.ui.RollingText
import com.naury.kinetica.ui.jellyPress
import com.naury.kinetica.ui.kClickable
import com.naury.kinetica.ui.kContextMenu
import com.naury.kinetica.ui.rememberKStaggerState
import com.naury.kinetica.ui.staggerIn

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
    recordActions: TimelineRecordActions = TimelineRecordActions(),
    /** 위 막대 높이. 목록이 막대 뒤로 지나가도록 안쪽 여백으로 준다. */
    contentTop: Dp = 0.dp,
    /** 목록 맨 위에서 함께 스크롤되는 머리글. 휴대폰에서는 목록·달력 전환을 여기 둔다. */
    header: (@Composable () -> Unit)? = null,
) {
    val gutter = ChageunTheme.spacing.gutter
    // 처음 열 때만 위에서부터 차례로 떠오르고, 스크롤해 나갔다 돌아온 기록은 다시 등장하지 않는다.
    val stagger = rememberKStaggerState()
    // 길게 눌러 고른 삭제는 상세와 같은 확인 창을 거친다. RecordRef는 저장할 수 없어 "종류:ID"로 둔다.
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
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
    // 목록이 막대 뒤로 지나가면 붙박이 달 머리글이 막대에 가려 흐려진다. 그때는 머리글을 막대 바로 아래에 따로 띄운다.
    val pinBelowBar = contentTop > 0.dp
    Box(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = contentTop + if (header == null) ChageunTheme.spacing.sm else 0.dp,
                bottom = FAB_CLEARANCE + LocalBottomBarPadding.current,
            ),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            header?.let { item(key = "header") { it() } }
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
            var order = 0
            uiState.sections.forEach { section ->
                val month: @Composable () -> Unit = { MonthHeader(section, Modifier.padding(horizontal = gutter)) }
                if (pinBelowBar) {
                    item(key = monthKey(section)) {
                        month()
                    }
                } else {
                    stickyHeader(key = monthKey(section)) { month() }
                }
                val sectionStart = order
                order += section.items.size
                itemsIndexed(section.items, key = { _, row -> rowKey(row) }) { index, item ->
                    TimelineRow(
                        item = item,
                        isSelected = item.ref == uiState.selected,
                        isFirst = index == 0,
                        isLast = index == section.items.lastIndex,
                        onClick = { onSelect(item.ref) },
                        menu = recordMenu(item, recordActions) { deleting = "${item.ref.type.name}:${item.ref.id}" },
                        modifier = Modifier
                            .padding(horizontal = gutter)
                            .staggerIn(sectionStart + index, stagger),
                    )
                    if (item.ref == adAfter) {
                        NativeAdSlot(Modifier.padding(start = gutter, end = gutter, top = ChageunTheme.spacing.xs))
                    }
                }
            }
        }
        if (pinBelowBar) PinnedMonthHeader(listState, uiState.sections, contentTop)
    }
    deleting?.toRecordRef()?.let { ref ->
        DeleteRecordDialog(
            onConfirm = {
                deleting = null
                recordActions.onDelete?.invoke(ref)
            },
            onDismiss = { deleting = null },
        )
    }
}

private fun monthKey(section: TimelineSection) = "month-${section.month}"

private fun rowKey(item: TimelineItem) = "${item.ref.type}-${item.ref.id}"

/**
 * 막대 아래에 붙는 달 머리글. 막대 바로 아래 줄을 지나는 기록의 달을 보여 주고, 다음 달 머리글이 다가오면 위로 밀려
 * 막대 뒤로 들어간다. 목록 안의 머리글이 아직 막대 아래에 보이면 겹치지 않게 숨긴다.
 */
@Composable
private fun BoxScope.PinnedMonthHeader(listState: LazyListState, sections: List<TimelineSection>, top: Dp) {
    val topPx = with(LocalDensity.current) { top.roundToPx() }
    val sectionOf = remember(sections) {
        buildMap {
            sections.forEachIndexed { index, section ->
                put(monthKey(section), index)
                section.items.forEach { put(rowKey(it), index) }
            }
        }
    }
    val pinned by remember(sections, topPx) {
        derivedStateOf {
            val info = listState.layoutInfo
            val start = info.viewportStartOffset
            val crossing = info.visibleItemsInfo.firstOrNull { it.offset - start + it.size > topPx }
            val index = crossing?.let { sectionOf[it.key] } ?: return@derivedStateOf null
            val ownHeader = info.visibleItemsInfo.firstOrNull { it.key == monthKey(sections[index]) }
            index.takeUnless { ownHeader != null && ownHeader.offset - start >= topPx }
        }
    }
    val index = pinned ?: return
    val nextKey = sections.getOrNull(index + 1)?.let(::monthKey)
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val info = listState.layoutInfo
                val nextTop = info.visibleItemsInfo.firstOrNull { it.key == nextKey }
                    ?.let { it.offset - info.viewportStartOffset }
                val push = nextTop?.let { (it - topPx - placeable.height).coerceAtMost(0) } ?: 0
                layout(placeable.width, placeable.height) { placeable.place(0, topPx + push) }
            }
            // 같은 글이 두 번 읽히지 않도록 목록 안의 머리글만 읽게 한다.
            .clearAndSetSemantics {},
    ) {
        MonthHeader(sections[index], Modifier.padding(horizontal = ChageunTheme.spacing.gutter))
    }
}

/**
 * 기록 카드를 길게 누르면 뜨는 메뉴. 상세에 있는 동작만 그대로 옮긴다. 넘기지 않은 동작은 메뉴에 두지 않고,
 * 주행거리 기록은 상세처럼 삭제만 둔다.
 */
internal data class TimelineRecordActions(
    val onEdit: ((TimelineItem) -> Unit)? = null,
    val onAskAi: ((RecordRef) -> Unit)? = null,
    val onDelete: ((RecordRef) -> Unit)? = null,
)

@Composable
private fun recordMenu(item: TimelineItem, actions: TimelineRecordActions, onDelete: () -> Unit): List<KMenuItem> {
    val isMileage = item.ref.type == TimelineEventType.Mileage
    val edit = stringResource(R.string.history_edit)
    val askAi = stringResource(R.string.history_ask_ai)
    val delete = stringResource(R.string.history_delete)
    return listOfNotNull(
        actions.onEdit?.takeUnless { isMileage }?.let { KMenuItem(edit, Icons.Filled.Edit) { it(item) } },
        actions.onAskAi?.takeUnless { isMileage }?.let { KMenuItem(askAi, Icons.Filled.AutoAwesome) { it(item.ref) } },
        actions.onDelete?.let { KMenuItem(delete, Icons.Filled.DeleteOutline, destructive = true, onClick = onDelete) },
    )
}

private fun LazyListScope.timelineFilters(
    uiState: HistoryUiState,
    onKeywordChanged: (String) -> Unit,
    onFilterSelected: (HistoryFilter) -> Unit,
    onOpenAdvancedFilter: () -> Unit,
) {
    item(key = "search") {
        KSearchBar(
            query = uiState.keyword,
            onQueryChange = onKeywordChanged,
            onSearch = onKeywordChanged,
            placeholder = stringResource(R.string.history_search_hint),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ChageunTheme.spacing.gutter),
        )
    }
    item(key = "filters") {
        // 칩마다 48dp 누름 영역을 두므로 그림자 여유는 따로 두지 않는다.
        KChipRow(
            contentPadding = PaddingValues(horizontal = ChageunTheme.spacing.gutter),
            fadeEdges = true,
            shadowRoom = false,
        ) {
            val count = uiState.advanced.activeCount
            KFilterChip(
                label = if (count > 0) {
                    stringResource(R.string.history_filter_count, count)
                } else {
                    stringResource(R.string.history_filter)
                },
                selected = count > 0,
                // 고급 필터 칩은 켜고 끄지 않고 조건 시트를 연다. 켜짐은 적용 중인 조건이 있다는 표시다.
                onSelectedChange = { onOpenAdvancedFilter() },
                leadingIcon = Icons.Filled.Tune,
                minTouchTarget = true,
            )
            HistoryFilter.entries.forEach { filter ->
                KFilterChip(
                    label = stringResource(filter.labelRes),
                    selected = uiState.filter == filter,
                    onSelectedChange = { onFilterSelected(filter) },
                    minTouchTarget = true,
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
            // 필터를 바꿔 합계가 달라지면 바뀐 자릿수만 굴러가며 바뀐다.
            RollingText(
                stringResource(R.string.history_month_total, formatNumber(total)),
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
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
    menu: List<KMenuItem> = emptyList(),
) {
    val shape = MaterialTheme.shapes.medium
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
        ChageunCard(
            modifier = Modifier
                .weight(1f)
                .jellyPress(CARD_JELLY)
                .then(if (menu.isEmpty()) Modifier else Modifier.kContextMenu(menu))
                .semantics { selected = isSelected }
                .kClickable(shape, onClick = onClick),
            shape = shape,
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
                        if (item.attachmentCount > 0) {
                            val count = item.attachmentCount
                            AttachmentMark(
                                Icons.Filled.AttachFile,
                                count,
                                pluralStringResource(R.plurals.history_attachment_count, count, count),
                            )
                        }
                        if (item.soundCount > 0) {
                            val count = item.soundCount
                            AttachmentMark(
                                Icons.Outlined.GraphicEq,
                                count,
                                pluralStringResource(R.plurals.history_sound_count, count, count),
                            )
                        }
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

/** 사진·영수증이나 녹음한 소리가 붙은 기록임을 카드에서 바로 알 수 있게 한다. 사진과 소리는 따로 센다. */
@Composable
private fun AttachmentMark(icon: ImageVector, count: Int, description: String) {
    Row(
        modifier = Modifier
            .padding(start = ChageunTheme.spacing.xs)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
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
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (isFiltered) {
            KEmptyState(
                title = stringResource(R.string.history_empty_filtered),
                message = stringResource(R.string.history_empty_filtered_body),
                art = { KEmptyIcon(KEmptyIconKind.NoResults) },
                action = {
                    KButton(stringResource(R.string.history_clear_filters), onClick = onClear, variant = KVariant.Tonal)
                },
            )
        } else {
            KEmptyState(
                title = stringResource(R.string.history_empty),
                message = stringResource(R.string.history_empty_body),
                art = { KEmptyIcon(KEmptyIconKind.EmptyInbox) },
                action = { KButton(stringResource(R.string.history_empty_action), onClick = onAdd) },
            )
        }
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

// 넓은 카드라 기본값(0.08)이면 너무 납작해진다. 눌린 느낌만 남을 만큼 줄인다.
private const val CARD_JELLY = 0.04f

private val FAB_CLEARANCE = 88.dp

private const val AD_AFTER_RECORDS = 8

private val ATTACHMENT_ICON_SIZE = 14.dp

private const val LOAD_MORE_AHEAD = 10
