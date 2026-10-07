package com.naury.chageun.feature.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.ads.LocalAdsEnabled
import com.naury.chageun.core.ads.NativeAdSlot
import com.naury.chageun.core.designsystem.component.PressStyle
import com.naury.chageun.core.designsystem.component.SegmentedControl
import com.naury.chageun.core.designsystem.component.StatusBadge
import com.naury.chageun.core.designsystem.component.colors
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.ui.EmptyState
import com.naury.chageun.core.ui.MaintenanceItemIcon
import com.naury.chageun.core.ui.MaintenanceProgressBar
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.missingInputText
import com.naury.chageun.core.ui.remainingText
import com.naury.chageun.core.ui.tone
import com.naury.chageun.core.ui.usedFraction

@Composable
internal fun ManageList(
    uiState: ManageUiState,
    onFilterSelected: (ManageFilter) -> Unit,
    onItemSelected: (MaintenanceItem) -> Unit,
    onEditRule: (MaintenanceItem) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    showFilters: Boolean = true,
) {
    val gutter = ChageunTheme.spacing.gutter
    val showAd = LocalAdsEnabled.current
    // 넓으면 카드를 여러 열로 놓고, 상세가 열려 칸이 좁아지면 한 열로 다시 배치된다.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(CARD_MIN_WIDTH),
        modifier = modifier,
        state = gridState,
        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = ChageunTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        // 세 칸 배치에서는 왼쪽 Pane이 같은 필터를 보여 주므로 목록 위에는 두지 않는다.
        if (showFilters) {
            item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                // 넓은 화면에서 끝까지 늘어나면 눌러야 할 곳이 멀어지므로 폭을 제한한다.
                SegmentedControl(
                    options = ManageFilter.entries.map { filter ->
                        stringResource(
                            R.string.manage_filter_with_count,
                            stringResource(filter.labelRes),
                            uiState.counts[filter] ?: 0,
                        )
                    },
                    selectedIndex = ManageFilter.entries.indexOf(uiState.filter),
                    onSelect = { onFilterSelected(ManageFilter.entries[it]) },
                    modifier = Modifier.widthIn(max = SEGMENT_MAX_WIDTH),
                )
            }
        }
        if (uiState.items.isEmpty() && !uiState.isLoading) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    icon = Icons.Filled.TaskAlt,
                    title = stringResource(R.string.manage_empty_filter),
                    body = stringResource(R.string.manage_empty_filter_body),
                    tone = ChageunTheme.colors.good,
                )
            }
        }
        items(uiState.items, key = { it.item }) { status ->
            MaintenanceItemCard(
                status = status,
                rule = uiState.rules[status.item],
                isSelected = status.item == uiState.selectedItem,
                onClick = { onItemSelected(status.item) },
                modifier = Modifier.animateItem(placementSpec = motionSpec(PLACEMENT_MS)),
            )
        }
        if (uiState.filter == ManageFilter.All && uiState.disabledItems.isNotEmpty()) {
            item(key = "disabled-title", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(vertical = ChageunTheme.spacing.xs)) {
                    Text(stringResource(R.string.manage_disabled_items), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.manage_disabled_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(uiState.disabledItems, key = { "disabled-$it" }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .pressable(onClick = { onEditRule(item) }, style = PressStyle.Highlight)
                        .padding(horizontal = ChageunTheme.spacing.xs, vertical = ChageunTheme.spacing.xs)
                        .alpha(DISABLED_ALPHA),
                    horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MaintenanceItemIcon(item, size = 36.dp)
                    Text(
                        text = stringResource(item.labelRes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // 기획서 17.2: 관리 목록의 마지막에 한 칸. 빈 목록이나 광고를 쓰지 않을 때는 간격도 남기지 않는다.
        if (showAd && uiState.items.isNotEmpty()) {
            item(key = "ad", span = { GridItemSpan(maxLineSpan) }) { NativeAdSlot() }
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
            .pressable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        // 펼친 폴더블의 목록 칸처럼 좁은 곳에서는 배지가 이름 자리를 빼앗지 않도록 이름 아래로 내린다.
        BoxWithConstraints(Modifier.padding(ChageunTheme.spacing.md)) {
            val isNarrow = maxWidth < NARROW_CARD_WIDTH
            val badge = @Composable {
                StatusBadge(tone = status.state.tone, label = stringResource(status.state.labelRes))
            }
            Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
                ) {
                    MaintenanceItemIcon(status.item)
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs),
                    ) {
                        Text(stringResource(status.item.labelRes), style = MaterialTheme.typography.titleMedium)
                        val supporting = if (status.state == MaintenanceState.Unknown) {
                            missingInputText(status)
                        } else {
                            remainingText(status)
                        }
                        supporting?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isNarrow) badge()
                    }
                    if (!isNarrow) badge()
                }
                usedFraction(status, rule)?.let { fraction ->
                    MaintenanceProgressBar(fraction, status.state.tone.colors.content)
                }
            }
        }
    }
}

internal val ManageFilter.labelRes: Int
    get() = when (this) {
        ManageFilter.All -> R.string.manage_filter_all
        ManageFilter.NeedsAttention -> R.string.manage_filter_attention
        ManageFilter.Upcoming -> R.string.manage_filter_upcoming
    }

private const val DISABLED_ALPHA = 0.6f

private val CARD_MIN_WIDTH = 300.dp
private val SEGMENT_MAX_WIDTH = 480.dp

// 상세가 열리며 칸 너비가 바뀔 때 카드가 새 자리로 미끄러지는 시간. 칸 애니메이션과 맞춘다.
private const val PLACEMENT_MS = 360

// 아이콘·이름·배지가 한 줄에 들어가려면 이 정도 너비가 필요하다.
private val NARROW_CARD_WIDTH = 320.dp
