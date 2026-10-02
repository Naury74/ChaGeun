package com.naury.chageun.feature.manage

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.naury.chageun.core.ads.LocalAdsEnabled
import com.naury.chageun.core.ads.NativeAdSlot
import com.naury.chageun.core.designsystem.component.StatusBadge
import com.naury.chageun.core.designsystem.component.colors
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.missingInputText
import com.naury.chageun.core.ui.remainingText
import com.naury.chageun.core.ui.tone

@Composable
internal fun ManageList(
    uiState: ManageUiState,
    onFilterSelected: (ManageFilter) -> Unit,
    onItemSelected: (MaintenanceItem) -> Unit,
    onEditRule: (MaintenanceItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gutter = ChageunTheme.spacing.gutter
    val showAd = LocalAdsEnabled.current
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = ChageunTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        item(key = "filters") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = gutter),
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            ) {
                items(ManageFilter.entries) { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Text(
                                stringResource(
                                    R.string.manage_filter_with_count,
                                    stringResource(filter.labelRes),
                                    uiState.counts[filter] ?: 0,
                                ),
                            )
                        },
                    )
                }
            }
        }
        if (uiState.items.isEmpty() && !uiState.isLoading) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.manage_empty_filter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }
        }
        items(uiState.items, key = { it.item }) { status ->
            MaintenanceItemCard(
                status = status,
                rule = uiState.rules[status.item],
                isSelected = status.item == uiState.selectedItem,
                onClick = { onItemSelected(status.item) },
                modifier = Modifier.padding(horizontal = gutter),
            )
        }
        if (uiState.filter == ManageFilter.All && uiState.disabledItems.isNotEmpty()) {
            item(key = "disabled-title") {
                Column(Modifier.padding(horizontal = gutter, vertical = ChageunTheme.spacing.xs)) {
                    Text(stringResource(R.string.manage_disabled_items), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.manage_disabled_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(uiState.disabledItems, key = { "disabled-$it" }) { item ->
                Text(
                    text = stringResource(item.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onEditRule(item) }
                        .padding(horizontal = gutter, vertical = ChageunTheme.spacing.sm),
                )
            }
        }
        // 기획서 17.2: 관리 목록의 마지막에 한 칸. 빈 목록이나 광고를 쓰지 않을 때는 간격도 남기지 않는다.
        if (showAd && uiState.items.isNotEmpty()) {
            item(key = "ad") { NativeAdSlot(Modifier.padding(horizontal = gutter)) }
        }
    }
}

@Composable
private fun MaintenanceItemCard(
    status: MaintenanceStatus,
    rule: MaintenanceRule?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(status.item.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(tone = status.state.tone, label = stringResource(status.state.labelRes))
            }
            val supporting = if (status.state ==
                MaintenanceState.Unknown
            ) {
                missingInputText(status)
            } else {
                remainingText(status)
            }
            supporting?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            usedFraction(status, rule)?.let { fraction ->
                LinearProgressIndicator(
                    progress = { fraction },
                    color = status.state.tone.colors.content,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private val ManageFilter.labelRes: Int
    get() = when (this) {
        ManageFilter.All -> R.string.manage_filter_all
        ManageFilter.NeedsAttention -> R.string.manage_filter_attention
        ManageFilter.Upcoming -> R.string.manage_filter_upcoming
    }
