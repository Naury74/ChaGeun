package com.naury.chageun.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.core.ads.LocalAdsEnabled
import com.naury.chageun.core.ads.NativeAdSlot
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.analytics.HomeAction
import com.naury.chageun.core.model.InspectionState
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.ui.Hinge
import com.naury.chageun.core.ui.HingeAwarePanes
import com.naury.chageun.core.ui.LocalAnalyticsTracker
import com.naury.chageun.core.ui.VehicleHeroSection
import com.naury.chageun.core.ui.currentSeparatingHinge
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.labelRes

@Composable
fun HomeRoute(
    onRecordService: (MaintenanceItem) -> Unit,
    onOpenHistory: () -> Unit,
    onUpdateMileage: () -> Unit,
    onAskAi: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInspection: () -> Unit,
    onOpenItem: (MaintenanceItem) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val analytics = LocalAnalyticsTracker.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass
    HomeScreen(
        uiState = uiState,
        // Book 자세: 단일 Pane이 접히는 부분을 가로지르지 않게 한다.
        paneCount = homePaneCount(windowSizeClass).let {
            if (currentSeparatingHinge()?.isVertical == true) maxOf(it, TWO_PANES) else it
        },
        actions = HomeActions(
            onRecordService = { item ->
                analytics.track(AnalyticsEvent.HomeActionOpened(HomeAction.RecordService))
                onRecordService(item)
            },
            onUpdateMileage = analytics.tracking(HomeAction.UpdateMileage, onUpdateMileage),
            onOpenHistory = analytics.tracking(HomeAction.OpenHistory, onOpenHistory),
            onAskAi = analytics.tracking(HomeAction.AskAi, onAskAi),
            onOpenSettings = onOpenSettings,
            onOpenInspection = analytics.tracking(HomeAction.OpenInspection, onOpenInspection),
            onOpenItem = onOpenItem,
        ),
    )
}

private fun AnalyticsTracker.tracking(action: HomeAction, block: () -> Unit): () -> Unit = {
    track(AnalyticsEvent.HomeActionOpened(action))
    block()
}

data class HomeActions(
    val onRecordService: (MaintenanceItem) -> Unit,
    val onUpdateMileage: () -> Unit,
    val onOpenHistory: () -> Unit,
    val onAskAi: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenInspection: () -> Unit = {},
    val onOpenItem: (MaintenanceItem) -> Unit = {},
)

/** Medium 너비에서는 Pane 하나만 둔다. Rail 옆에 두 Pane을 놓으면 상세 영역이 최소 너비 360dp보다 좁아진다. */
fun homePaneCount(windowSizeClass: WindowSizeClass): Int = when {
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) -> THREE_PANES
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> TWO_PANES
    else -> SINGLE_PANE
}

private const val SINGLE_PANE = 1
private const val TWO_PANES = 2
private const val THREE_PANES = 3

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    paneCount: Int,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    hinge: Hinge? = currentSeparatingHinge(),
) {
    when (uiState) {
        HomeUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is HomeUiState.Content -> HomeContent(uiState, paneCount, actions, hinge, modifier)
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    paneCount: Int,
    actions: HomeActions,
    hinge: Hinge?,
    modifier: Modifier,
) {
    val spacing = ChageunTheme.spacing
    // Tabletop: 차량과 상태는 위쪽 절반에, 목록과 액션은 아래쪽 절반에 둔다.
    val isTabletop = hinge != null && !hinge.isVertical
    val showAd = LocalAdsEnabled.current
    val panes: List<LazyListScope.() -> Unit> = when {
        isTabletop -> listOf({ summaryPane(state, actions) }, {
            attentionPane(state, actions)
            missingPane(state, actions)
            recentPane(state, actions, showAd)
        })
        else -> homePanes(state, paneCount, actions, showAd)
    }
    HingeAwarePanes(
        weights = List(panes.size) { 1f },
        modifier = modifier.fillMaxSize(),
        stacked = isTabletop,
        hinge = hinge,
    ) {
        panes.forEach { pane ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                content = pane,
            )
        }
    }
}

private fun homePanes(
    state: HomeUiState.Content,
    paneCount: Int,
    actions: HomeActions,
    showAd: Boolean,
): List<LazyListScope.() -> Unit> = when (paneCount) {
    SINGLE_PANE -> listOf({
        summaryPane(state, actions)
        attentionPane(state, actions)
        missingPane(state, actions)
        recentPane(state, actions, showAd)
    })
    TWO_PANES -> listOf({ summaryPane(state, actions) }, {
        attentionPane(state, actions)
        missingPane(state, actions)
        recentPane(state, actions, showAd)
    })
    else -> listOf({ summaryPane(state, actions) }, { attentionPane(state, actions) }, {
        missingPane(state, actions)
        recentPane(state, actions, showAd)
    })
}

private fun LazyListScope.summaryPane(state: HomeUiState.Content, actions: HomeActions) {
    item(key = "brand") { BrandAppBar(actions.onOpenSettings) }
    item(key = "hero") { HomeHero(state, actions.onUpdateMileage) }
    item(key = "health") {
        VehicleStatusSummary(
            health = state.overview.health,
            goodCount = state.goodCount,
            attentionCount = state.attentionCount,
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
    val inspection = state.overview.inspection
    statusSection(
        "attention",
        R.string.home_section_attention,
        state.needsAttention,
        state.overview.rules,
        actions,
        inspection.takeIf { it.state == InspectionState.Overdue },
    )
    statusSection(
        "upcoming",
        R.string.home_section_upcoming,
        state.upcoming,
        state.overview.rules,
        actions,
        inspection.takeIf { it.state == InspectionState.DueSoon },
    )
}

private fun LazyListScope.statusSection(
    key: String,
    titleRes: Int,
    statuses: List<MaintenanceStatus>,
    rules: Map<MaintenanceItem, MaintenanceRule>,
    actions: HomeActions,
    inspection: InspectionStatus?,
) {
    if (statuses.isEmpty() && inspection == null) return
    val onRecordService = actions.onRecordService
    item(key = "$key-title") {
        SectionTitle(stringResource(titleRes), Modifier.padding(horizontal = ChageunTheme.spacing.gutter))
    }
    if (inspection != null) {
        item(key = "$key-inspection") {
            InspectionStatusCard(
                inspection,
                actions.onOpenInspection,
                Modifier.padding(horizontal = ChageunTheme.spacing.gutter),
            )
        }
    }
    items(statuses, key = { "$key-${it.item}" }) { status ->
        MaintenanceStatusCard(
            status = status,
            rule = rules[status.item],
            onRecordService = onRecordService,
            onOpenDetail = actions.onOpenItem,
            modifier = Modifier.padding(horizontal = ChageunTheme.spacing.gutter),
        )
    }
}

private fun LazyListScope.missingPane(state: HomeUiState.Content, actions: HomeActions) {
    if (state.missingInfo.isEmpty()) return
    item(key = "missing-title") {
        SectionTitle(
            stringResource(R.string.home_section_missing),
            Modifier.padding(horizontal = ChageunTheme.spacing.gutter),
        )
    }
    item(key = "missing-list") {
        MissingInfoCard(
            state.missingInfo,
            actions.onOpenItem,
            Modifier.padding(horizontal = ChageunTheme.spacing.gutter),
        )
    }
}

private fun LazyListScope.recentPane(state: HomeUiState.Content, actions: HomeActions, showAd: Boolean) {
    // 기획서 UI 6.1: 다가오는 관리 다음, 최근 기록 앞에 한 칸만 둔다. 광고를 쓰지 않으면 목록 간격도 남기지 않는다.
    if (showAd) item(key = "ad") { NativeAdSlot(Modifier.padding(horizontal = ChageunTheme.spacing.gutter)) }
    item(key = "recent") {
        RecentRecords(
            records = state.recentRecords,
            onOpenHistory = actions.onOpenHistory,
            modifier = Modifier.padding(horizontal = ChageunTheme.spacing.gutter),
        )
    }
    item(key = "ai") { AiQuestionCard(actions.onAskAi, Modifier.padding(horizontal = ChageunTheme.spacing.gutter)) }
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
        photoPath = state.photoPath,
        mileage = mileage?.let { stringResource(R.string.home_mileage, formatNumber(it.mileage.value)) },
        freshness = when {
            mileage == null -> stringResource(R.string.home_mileage_unknown)
            mileage.date == state.today -> stringResource(R.string.home_mileage_as_of_today)
            else -> stringResource(R.string.home_mileage_as_of, formatDate(mileage.date))
        },
        action = {
            FilledTonalButton(onClick = onUpdateMileage) {
                Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(ChageunTheme.spacing.xs))
                Text(stringResource(R.string.home_mileage_update))
            }
        },
    )
}
