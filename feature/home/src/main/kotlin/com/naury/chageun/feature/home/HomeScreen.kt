package com.naury.chageun.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.ui.VehicleHeroSection
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.labelRes

@Composable
fun HomeRoute(
    onRecordService: (MaintenanceItem) -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass
    var isUpdatingMileage by rememberSaveable { mutableStateOf(false) }
    HomeScreen(
        uiState = uiState,
        paneCount = homePaneCount(windowSizeClass),
        actions = HomeActions(
            onRecordService = onRecordService,
            onUpdateMileage = { isUpdatingMileage = true },
            onOpenHistory = onOpenHistory,
        ),
    )
    if (isUpdatingMileage) {
        MileageUpdateHost(
            isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND),
            onDismiss = { isUpdatingMileage = false },
        )
    }
}

data class HomeActions(
    val onRecordService: (MaintenanceItem) -> Unit,
    val onUpdateMileage: () -> Unit,
    val onOpenHistory: () -> Unit,
)

/** Medium widths keep one pane: next to a rail, two panes would fall below the 360dp minimum detail width. */
fun homePaneCount(windowSizeClass: WindowSizeClass): Int = when {
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) -> THREE_PANES
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> TWO_PANES
    else -> SINGLE_PANE
}

private const val SINGLE_PANE = 1
private const val TWO_PANES = 2
private const val THREE_PANES = 3

@Composable
fun HomeScreen(uiState: HomeUiState, paneCount: Int, actions: HomeActions, modifier: Modifier = Modifier) {
    when (uiState) {
        HomeUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is HomeUiState.Content -> HomeContent(uiState, paneCount, actions, modifier)
    }
}

@Composable
private fun HomeContent(state: HomeUiState.Content, paneCount: Int, actions: HomeActions, modifier: Modifier) {
    val spacing = ChageunTheme.spacing
    val panes: List<LazyListScope.() -> Unit> = when (paneCount) {
        SINGLE_PANE -> listOf({
            summaryPane(state, actions)
            attentionPane(state, actions)
            missingPane(state)
            recentPane(state, actions)
        })
        TWO_PANES -> listOf({ summaryPane(state, actions) }, {
            attentionPane(state, actions)
            missingPane(state)
            recentPane(state, actions)
        })
        else -> listOf({ summaryPane(state, actions) }, { attentionPane(state, actions) }, {
            missingPane(state)
            recentPane(state, actions)
        })
    }
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(spacing.paneGap),
    ) {
        panes.forEach { pane ->
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                content = pane,
            )
        }
    }
}

private fun LazyListScope.summaryPane(state: HomeUiState.Content, actions: HomeActions) {
    item(key = "hero") { HomeHero(state, actions.onUpdateMileage) }
    item(key = "health") {
        VehicleStatusSummary(
            health = state.overview.health,
            goodCount = state.goodCount,
            modifier = Modifier.padding(horizontal = ChageunTheme.spacing.gutter),
        )
    }
    if (state.needsMileageUpdate) {
        item(key = "mileage-prompt") {
            MileagePromptCard(actions.onUpdateMileage, Modifier.padding(horizontal = ChageunTheme.spacing.gutter))
        }
    }
}

private fun LazyListScope.attentionPane(state: HomeUiState.Content, actions: HomeActions) {
    statusSection("attention", R.string.home_section_attention, state.needsAttention, actions.onRecordService)
    statusSection("upcoming", R.string.home_section_upcoming, state.upcoming, actions.onRecordService)
}

private fun LazyListScope.statusSection(
    key: String,
    titleRes: Int,
    statuses: List<MaintenanceStatus>,
    onRecordService: (MaintenanceItem) -> Unit,
) {
    if (statuses.isEmpty()) return
    item(key = "$key-title") {
        SectionTitle(stringResource(titleRes), Modifier.padding(horizontal = ChageunTheme.spacing.gutter))
    }
    items(statuses, key = { "$key-${it.item}" }) { status ->
        MaintenanceStatusCard(status, onRecordService, Modifier.padding(horizontal = ChageunTheme.spacing.gutter))
    }
}

private fun LazyListScope.missingPane(state: HomeUiState.Content) {
    if (state.missingInfo.isEmpty()) return
    item(key = "missing-title") {
        SectionTitle(
            stringResource(R.string.home_section_missing),
            Modifier.padding(horizontal = ChageunTheme.spacing.gutter),
        )
    }
    items(state.missingInfo, key = { "missing-${it.item}" }) { status ->
        MissingInfoRow(status, Modifier.padding(horizontal = ChageunTheme.spacing.gutter))
    }
}

private fun LazyListScope.recentPane(state: HomeUiState.Content, actions: HomeActions) {
    item(key = "recent") {
        RecentRecords(
            records = state.recentRecords,
            onOpenHistory = actions.onOpenHistory,
            modifier = Modifier.padding(horizontal = ChageunTheme.spacing.gutter),
        )
    }
}

@Composable
private fun HomeHero(state: HomeUiState.Content, onUpdateMileage: () -> Unit) {
    val vehicle = state.vehicle
    val mileage = state.overview.currentMileage
    val subtitleParts = listOfNotNull(
        vehicle.modelYear?.toString(),
        vehicle.fuelType?.let { stringResource(it.labelRes) },
    )
    VehicleHeroSection(
        title = "${vehicle.maker} ${vehicle.model}",
        subtitle = when (subtitleParts.size) {
            2 -> stringResource(R.string.home_vehicle_subtitle, subtitleParts[0], subtitleParts[1])
            else -> subtitleParts.joinToString()
        },
        mileage = mileage?.let { stringResource(R.string.home_mileage, formatNumber(it.mileage.value)) },
        freshness = when {
            mileage == null -> stringResource(R.string.home_mileage_unknown)
            mileage.date == state.today -> stringResource(R.string.home_mileage_as_of_today)
            else -> stringResource(R.string.home_mileage_as_of, formatDate(mileage.date))
        },
        action = {
            TextButton(onClick = onUpdateMileage) { Text(stringResource(R.string.home_mileage_update)) }
        },
    )
}
