package com.naury.chageun.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.SelectableListRow
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.icon

/**
 * 세 칸 배치의 왼쪽 Pane. 타임라인 위에 있던 검색·종류 필터·고급 필터를 옮겨 와, 조건을 바꿔도 타임라인과
 * 상세가 자리를 지킨다. 고급 필터는 적용 중인 조건을 펼쳐 보여 주므로 시트를 열지 않고도 확인할 수 있다.
 */
@Composable
internal fun HistoryFilterPane(
    uiState: HistoryUiState,
    onKeywordChanged: (String) -> Unit,
    onFilterSelected: (HistoryFilter) -> Unit,
    onOpenAdvancedFilter: () -> Unit,
    onClearAdvancedFilter: () -> Unit,
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
        OutlinedTextField(
            value = uiState.keyword,
            onValueChange = onKeywordChanged,
            placeholder = { Text(stringResource(R.string.history_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        CardGroup(title = stringResource(R.string.history_pane_type)) {
            HistoryFilter.entries.forEachIndexed { index, filter ->
                if (index > 0) GroupDivider()
                SelectableListRow(
                    icon = filter.icon,
                    title = stringResource(filter.labelRes),
                    selected = uiState.filter == filter,
                    onClick = { onFilterSelected(filter) },
                )
            }
        }
        val advanced = uiState.advanced
        CardGroup(title = stringResource(R.string.history_pane_more)) {
            ListRow(
                icon = Icons.Filled.Tune,
                title = if (advanced.activeCount > 0) {
                    stringResource(R.string.history_filter_count, advanced.activeCount)
                } else {
                    stringResource(R.string.history_filter)
                },
                body = advancedSummary(advanced),
                tone = if (advanced.activeCount > 0) ChageunTheme.colors.upcoming else ChageunTheme.colors.unknown,
                onClick = onOpenAdvancedFilter,
            )
            if (advanced.activeCount > 0) {
                GroupDivider()
                ListRow(
                    icon = Icons.Filled.RestartAlt,
                    title = stringResource(R.string.history_filter_reset),
                    onClick = onClearAdvancedFilter,
                    trailing = {},
                )
            }
        }
    }
}

@Composable
private fun advancedSummary(filter: AdvancedFilter): String {
    val period = when (filter.period) {
        HistoryPeriod.All -> null
        HistoryPeriod.Custom -> filter.customFrom?.let { from ->
            filter.customTo?.let { to ->
                stringResource(R.string.history_filter_range, formatDate(from), formatDate(to))
            }
        } ?: stringResource(HistoryPeriod.Custom.labelRes)
        else -> stringResource(filter.period.labelRes)
    }
    val min = filter.minCostWon?.let { stringResource(R.string.history_won, formatNumber(it)) }
    val max = filter.maxCostWon?.let { stringResource(R.string.history_won, formatNumber(it)) }
    val cost = when {
        min != null && max != null -> stringResource(R.string.history_filter_range, min, max)
        min != null -> stringResource(R.string.history_filter_cost_from, min)
        max != null -> stringResource(R.string.history_filter_cost_to, max)
        else -> null
    }
    val attachments = stringResource(R.string.history_filter_attachments_short).takeIf { filter.withAttachmentsOnly }
    return listOfNotNull(period, cost, attachments).joinToString(", ")
        .ifEmpty { stringResource(R.string.history_filter_none) }
}

private val HistoryFilter.icon: ImageVector
    get() = when (this) {
        HistoryFilter.All -> Icons.AutoMirrored.Filled.EventNote
        HistoryFilter.Maintenance -> TimelineEventType.Maintenance.icon
        HistoryFilter.Fuel -> TimelineEventType.Fuel.icon
        HistoryFilter.Check -> TimelineEventType.Inspection.icon
        HistoryFilter.Other -> TimelineEventType.Note.icon
        HistoryFilter.Mileage -> TimelineEventType.Mileage.icon
    }
