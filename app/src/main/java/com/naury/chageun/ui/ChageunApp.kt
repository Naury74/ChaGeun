package com.naury.chageun.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.feature.history.HistoryRoute
import com.naury.chageun.feature.home.HomeRoute
import com.naury.chageun.feature.home.MileageUpdateHost
import com.naury.chageun.feature.manage.ManageRoute
import com.naury.chageun.feature.manage.record.RecordServiceHost
import com.naury.chageun.feature.vehicle.VehicleRoute
import com.naury.chageun.navigation.TopLevelDestination
import com.naury.chageun.navigation.TopLevelRoute

/** Cross-feature actions handled by the app shell so feature modules never depend on each other. */
data class AppActions(
    val onRecordService: (MaintenanceItem) -> Unit,
    val onNavigate: (TopLevelDestination) -> Unit,
    val onUpdateMileage: () -> Unit = {},
    /** Item opened from a notification, consumed once the Care tab has selected it. */
    val pendingManageItem: MaintenanceItem? = null,
    val onPendingManageItemHandled: () -> Unit = {},
)

@Composable
fun ChageunApp(
    deepLinkItem: MaintenanceItem? = null,
    onDeepLinkHandled: () -> Unit = {},
    destinationContent: @Composable (TopLevelDestination, AppActions) -> Unit =
        { destination, actions -> DestinationContent(destination, actions) },
) {
    val windowSizeClass = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass
    var recordingItem by rememberSaveable { mutableStateOf<MaintenanceItem?>(null) }
    var isUpdatingMileage by rememberSaveable { mutableStateOf(false) }
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val backStack = rememberNavBackStack(TopLevelRoute.Home)
    val currentTopLevel = backStack.firstOrNull() as? TopLevelRoute ?: TopLevelRoute.Home
    val navigateTo: (TopLevelDestination) -> Unit = { destination ->
        if (destination.route != currentTopLevel) {
            backStack.clear()
            backStack.add(destination.route)
        }
    }
    val actions = AppActions(
        onRecordService = { recordingItem = it },
        onNavigate = navigateTo,
        onUpdateMileage = { isUpdatingMileage = true },
        pendingManageItem = deepLinkItem,
        onPendingManageItemHandled = onDeepLinkHandled,
    )
    LaunchedEffect(deepLinkItem) { if (deepLinkItem != null) navigateTo(TopLevelDestination.Manage) }

    NavigationSuiteScaffold(
        layoutType = navigationSuiteTypeFor(windowSizeClass),
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = destination.route == currentTopLevel,
                    onClick = { navigateTo(destination) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(stringResource(destination.labelRes)) },
                )
            }
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                TopLevelDestination.entries.forEach { destination ->
                    entry(destination.route) { destinationContent(destination, actions) }
                }
            },
        )
    }

    recordingItem?.let { item ->
        RecordServiceHost(item = item, isExpanded = isExpanded, onDismiss = { recordingItem = null })
    }
    if (isUpdatingMileage) {
        MileageUpdateHost(isExpanded = isExpanded, onDismiss = { isUpdatingMileage = false })
    }
}

@Composable
private fun DestinationContent(destination: TopLevelDestination, actions: AppActions) {
    when (destination) {
        TopLevelDestination.Home -> HomeRoute(
            onRecordService = actions.onRecordService,
            onOpenHistory = { actions.onNavigate(TopLevelDestination.History) },
            onUpdateMileage = actions.onUpdateMileage,
        )
        TopLevelDestination.Manage -> ManageRoute(
            onRecordService = actions.onRecordService,
            pendingSelection = actions.pendingManageItem,
            onPendingSelectionHandled = actions.onPendingManageItemHandled,
        )
        TopLevelDestination.History -> HistoryRoute(onRecordService = actions.onRecordService)
        TopLevelDestination.Vehicle -> VehicleRoute(onUpdateMileage = actions.onUpdateMileage)
    }
}
