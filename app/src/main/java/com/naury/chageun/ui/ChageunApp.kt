package com.naury.chageun.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.R
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.feature.home.HomeRoute
import com.naury.chageun.feature.manage.ManageRoute
import com.naury.chageun.feature.manage.record.RecordServiceHost
import com.naury.chageun.navigation.TopLevelDestination
import com.naury.chageun.navigation.TopLevelRoute

@Composable
fun ChageunApp(
    destinationContent: @Composable (TopLevelDestination, onRecordService: (MaintenanceItem) -> Unit) -> Unit =
        { destination, onRecordService -> DestinationContent(destination, onRecordService) },
) {
    val windowSizeClass = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass
    var recordingItem by rememberSaveable { mutableStateOf<MaintenanceItem?>(null) }
    val onRecordService: (MaintenanceItem) -> Unit = { recordingItem = it }
    val backStack = rememberNavBackStack(TopLevelRoute.Home)
    val currentTopLevel = backStack.firstOrNull() as? TopLevelRoute ?: TopLevelRoute.Home

    NavigationSuiteScaffold(
        layoutType = navigationSuiteTypeFor(windowSizeClass),
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = destination.route == currentTopLevel,
                    onClick = {
                        if (destination.route != currentTopLevel) {
                            backStack.clear()
                            backStack.add(destination.route)
                        }
                    },
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
                    entry(destination.route) { destinationContent(destination, onRecordService) }
                }
            },
        )
    }

    recordingItem?.let { item ->
        RecordServiceHost(
            item = item,
            isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND),
            onDismiss = { recordingItem = null },
        )
    }
}

@Composable
private fun DestinationContent(destination: TopLevelDestination, onRecordService: (MaintenanceItem) -> Unit) {
    when (destination) {
        TopLevelDestination.Home -> HomeRoute(onRecordService = onRecordService)
        TopLevelDestination.Manage -> ManageRoute(onRecordService = onRecordService)
        else -> PendingDestination(destination)
    }
}

@Composable
private fun PendingDestination(destination: TopLevelDestination) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ChageunTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
    ) {
        Text(text = stringResource(destination.labelRes), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(R.string.destination_pending),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
