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
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.R
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.notification.DeepLink
import com.naury.chageun.feature.ai.AiHubRoute
import com.naury.chageun.feature.history.HistoryRoute
import com.naury.chageun.feature.home.HomeRoute
import com.naury.chageun.feature.home.MileageUpdateHost
import com.naury.chageun.feature.manage.ManageRoute
import com.naury.chageun.feature.manage.record.RecordServiceHost
import com.naury.chageun.feature.settings.OpenSourceLicensesRoute
import com.naury.chageun.feature.settings.PrivacyNoticeScreen
import com.naury.chageun.feature.settings.SettingsRoute
import com.naury.chageun.feature.vehicle.VehicleRoute
import com.naury.chageun.navigation.AiRoute
import com.naury.chageun.navigation.OpenSourceLicensesRoute
import com.naury.chageun.navigation.PrivacyNoticeRoute
import com.naury.chageun.navigation.SettingsRoute
import com.naury.chageun.navigation.TopLevelDestination
import com.naury.chageun.navigation.TopLevelRoute

/** Cross-feature actions handled by the app shell so feature modules never depend on each other. */
data class AppActions(
    val onRecordService: (MaintenanceItem) -> Unit,
    val onNavigate: (TopLevelDestination) -> Unit,
    val onUpdateMileage: () -> Unit = {},
    val onAskAi: (MaintenanceItem?) -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    /** Item opened from a notification, consumed once the Care tab has selected it. */
    val pendingManageItem: MaintenanceItem? = null,
    val onPendingManageItemHandled: () -> Unit = {},
)

@Composable
fun ChageunApp(
    deepLink: DeepLink? = null,
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
        onAskAi = { item -> backStack.add(AiRoute(item?.name)) },
        onOpenSettings = { backStack.add(SettingsRoute) },
        pendingManageItem = (deepLink as? DeepLink.Maintenance)?.item,
        onPendingManageItemHandled = onDeepLinkHandled,
    )
    LaunchedEffect(deepLink) {
        when (deepLink) {
            is DeepLink.Maintenance -> navigateTo(TopLevelDestination.Manage)
            DeepLink.Inspection -> {
                navigateTo(TopLevelDestination.Vehicle)
                onDeepLinkHandled()
            }
            null -> Unit
        }
    }

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
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                TopLevelDestination.entries.forEach { destination ->
                    entry(destination.route) { destinationContent(destination, actions) }
                }
                entry(SettingsRoute) {
                    SettingsRoute(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenLicenses = { backStack.add(OpenSourceLicensesRoute) },
                        onOpenPrivacy = { backStack.add(PrivacyNoticeRoute) },
                    )
                }
                entry(OpenSourceLicensesRoute) {
                    OpenSourceLicensesRoute(R.raw.aboutlibraries, onBack = { backStack.removeLastOrNull() })
                }
                entry(PrivacyNoticeRoute) { PrivacyNoticeScreen(onBack = { backStack.removeLastOrNull() }) }
                entry<AiRoute> { route ->
                    AiHubRoute(
                        focusItem = route.focusItem?.let { name ->
                            MaintenanceItem.entries.firstOrNull { it.name == name }
                        },
                        onBack = { backStack.removeLastOrNull() },
                    )
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
            onAskAi = { actions.onAskAi(null) },
            onOpenSettings = actions.onOpenSettings,
            onOpenInspection = { actions.onNavigate(TopLevelDestination.Vehicle) },
        )
        TopLevelDestination.Manage -> ManageRoute(
            onRecordService = actions.onRecordService,
            onAskAi = { actions.onAskAi(it) },
            pendingSelection = actions.pendingManageItem,
            onPendingSelectionHandled = actions.onPendingManageItemHandled,
        )
        TopLevelDestination.History -> HistoryRoute(onRecordService = actions.onRecordService)
        TopLevelDestination.Vehicle -> VehicleRoute(
            onUpdateMileage = actions.onUpdateMileage,
            onOpenSettings = actions.onOpenSettings,
        )
    }
}
