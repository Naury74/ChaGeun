package com.naury.chageun.feature.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.ToneColors
import com.naury.chageun.core.model.MaintenanceCategory
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.SelectableListRow
import com.naury.chageun.core.ui.icon
import com.naury.chageun.core.ui.tone

/** 세 칸 배치의 왼쪽 Pane. 목록 위의 상태 필터를 세로로 옮기고, 분류로도 좁혀 볼 수 있게 한다. */
@Composable
internal fun ManageFilterPane(
    uiState: ManageUiState,
    onFilterSelected: (ManageFilter) -> Unit,
    onCategorySelected: (MaintenanceCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = ChageunTheme.spacing.gutter, bottom = ChageunTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
    ) {
        CardGroup(title = stringResource(R.string.manage_pane_status)) {
            ManageFilter.entries.forEachIndexed { index, filter ->
                if (index > 0) GroupDivider()
                SelectableListRow(
                    icon = filter.icon,
                    title = stringResource(filter.labelRes),
                    selected = uiState.filter == filter,
                    onClick = { onFilterSelected(filter) },
                    tone = filter.tone(),
                    trailingText = (uiState.counts[filter] ?: 0).toString(),
                )
            }
        }
        CardGroup(title = stringResource(R.string.manage_pane_category)) {
            SelectableListRow(
                icon = Icons.Filled.Widgets,
                title = stringResource(R.string.manage_category_all),
                selected = uiState.category == null,
                onClick = { onCategorySelected(null) },
                trailingText = uiState.categoryCounts.values.sum().toString(),
            )
            uiState.categoryCounts.forEach { (category, count) ->
                GroupDivider()
                SelectableListRow(
                    icon = category.icon,
                    title = stringResource(category.labelRes),
                    selected = uiState.category == category,
                    // 고른 분류를 다시 누르면 모든 분류로 돌아간다.
                    onClick = { onCategorySelected(category.takeUnless { it == uiState.category }) },
                    tone = category.tone(),
                    trailingText = count.toString(),
                )
            }
        }
    }
}

private val ManageFilter.icon: ImageVector
    get() = when (this) {
        ManageFilter.All -> Icons.Filled.SelectAll
        ManageFilter.NeedsAttention -> Icons.Filled.ErrorOutline
        ManageFilter.Upcoming -> Icons.Filled.Schedule
    }

@Composable
private fun ManageFilter.tone(): ToneColors = when (this) {
    ManageFilter.All -> ChageunTheme.colors.unknown
    ManageFilter.NeedsAttention -> ChageunTheme.colors.critical
    ManageFilter.Upcoming -> ChageunTheme.colors.upcoming
}

// 분류마다 따로 그린 아이콘이 없어, 그 분류의 첫 항목 아이콘을 대표로 쓴다.
private val MaintenanceCategory.icon: ImageVector
    get() = MaintenanceItem.entries.first { it.category == this }.icon

private val MaintenanceCategory.labelRes: Int
    get() = when (this) {
        MaintenanceCategory.Engine -> R.string.manage_category_engine
        MaintenanceCategory.Climate -> R.string.manage_category_climate
        MaintenanceCategory.Brake -> R.string.manage_category_brake
        MaintenanceCategory.Drivetrain -> R.string.manage_category_drivetrain
        MaintenanceCategory.Electrical -> R.string.manage_category_electrical
        MaintenanceCategory.Chassis -> R.string.manage_category_chassis
        MaintenanceCategory.Visibility -> R.string.manage_category_visibility
    }
