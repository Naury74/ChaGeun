package com.naury.chageun.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.formatYearMonth
import com.naury.chageun.core.ui.labelRes

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TimelineContent(
    uiState: HistoryUiState,
    onKeywordChanged: (String) -> Unit,
    onFilterSelected: (HistoryFilter) -> Unit,
    onSelect: (RecordRef) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gutter = ChageunTheme.spacing.gutter
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = FAB_CLEARANCE),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
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
                    .padding(horizontal = gutter),
            )
        }
        item(key = "filters") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = gutter),
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            ) {
                items(HistoryFilter.entries) { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(stringResource(filter.labelRes)) },
                    )
                }
            }
        }
        if (uiState.isEmpty) {
            item(key = "empty") {
                EmptyTimeline(
                    isFiltered = uiState.isFiltered,
                    onAdd = onAdd,
                    onClear = {
                        onFilterSelected(HistoryFilter.All)
                        onKeywordChanged("")
                    },
                )
            }
        }
        uiState.sections.forEach { section ->
            stickyHeader(key = "month-${section.month}") {
                Text(
                    text = section.month?.let { formatYearMonth(it) } ?: stringResource(R.string.history_unknown_date),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = gutter, vertical = ChageunTheme.spacing.xs)
                        .semantics { heading() },
                )
            }
            items(section.items, key = { "${it.ref.type}-${it.ref.id}" }) { item ->
                TimelineRow(
                    item = item,
                    isSelected = item.ref == uiState.selected,
                    onClick = { onSelect(item.ref) },
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(item: TimelineItem, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.padding(ChageunTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs)) {
                Text(timelineTitle(item), style = MaterialTheme.typography.titleSmall)
                val meta = listOfNotNull(
                    item.date?.let { formatDate(it) },
                    item.mileage?.let { stringResource(R.string.history_km, formatNumber(it.value)) },
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

@Composable
internal fun timelineTitle(item: TimelineItem): String {
    val typeLabel = stringResource(item.ref.type.labelRes)
    val maintenanceItem = item.maintenanceItem
    return when {
        maintenanceItem != null -> stringResource(maintenanceItem.labelRes)
        item.ref.type == TimelineEventType.Fuel -> listOfNotNull(typeLabel, item.title).joinToString(" · ")
        else -> item.title ?: typeLabel
    }
}

@Composable
private fun EmptyTimeline(isFiltered: Boolean, onAdd: () -> Unit, onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ChageunTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        Text(
            stringResource(if (isFiltered) R.string.history_empty_filtered else R.string.history_empty),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (isFiltered) {
            TextButton(onClick = onClear) { Text(stringResource(R.string.history_clear_filters)) }
        } else {
            TextButton(onClick = onAdd) { Text(stringResource(R.string.history_empty_action)) }
        }
    }
}

internal val TimelineEventType.labelRes: Int
    get() = when (this) {
        TimelineEventType.Maintenance -> R.string.history_filter_maintenance
        TimelineEventType.Fuel -> R.string.history_type_fuel
        TimelineEventType.Inspection -> R.string.history_type_inspection
        TimelineEventType.Repair -> R.string.history_type_repair
        TimelineEventType.Note -> R.string.history_type_note
    }

private val HistoryFilter.labelRes: Int
    get() = when (this) {
        HistoryFilter.All -> R.string.history_filter_all
        HistoryFilter.Maintenance -> R.string.history_filter_maintenance
        HistoryFilter.Fuel -> R.string.history_filter_fuel
        HistoryFilter.Check -> R.string.history_filter_check
        HistoryFilter.Other -> R.string.history_filter_other
    }

private val FAB_CLEARANCE = 88.dp
