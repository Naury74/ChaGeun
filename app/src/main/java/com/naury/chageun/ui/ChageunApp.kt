package com.naury.chageun.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.naury.chageun.feature.account.AccountRoute as AccountScreenRoute
import com.naury.chageun.feature.account.DeleteAccountRoute
import com.naury.chageun.feature.account.DriveBackupRoute as DriveBackupScreenRoute
import com.naury.chageun.feature.account.EmailAuthRoute
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
import com.naury.chageun.feature.vehicle.album.AlbumRoute as AlbumScreenRoute
import com.naury.chageun.navigation.AccountDeleteRoute
import com.naury.chageun.navigation.AccountEmailRoute
import com.naury.chageun.navigation.AccountRoute
import com.naury.chageun.navigation.AiRoute
import com.naury.chageun.navigation.AlbumRoute
import com.naury.chageun.navigation.DriveBackupRoute
import com.naury.chageun.navigation.OpenSourceLicensesRoute
import com.naury.chageun.navigation.PrivacyNoticeRoute
import com.naury.chageun.navigation.SettingsRoute
import com.naury.chageun.navigation.TopLevelDestination
import com.naury.chageun.navigation.TopLevelRoute

/** feature 모듈끼리 서로 의존하지 않도록 앱 셸이 처리하는 feature 간 액션. */
data class AppActions(
    val onRecordService: (MaintenanceItem) -> Unit,
    /** 저장된 정비 기록을 같은 교체 기록 시트로 고친다. */
    val onEditService: (MaintenanceItem, String) -> Unit = { _, _ -> },
    val onNavigate: (TopLevelDestination) -> Unit,
    val onUpdateMileage: () -> Unit = {},
    val onAskAi: (MaintenanceItem?) -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenAccount: () -> Unit = {},
    val onOpenAlbum: () -> Unit = {},
    /** 다른 탭에서 관리 탭의 항목 상세를 연다. */
    val onOpenManageItem: (MaintenanceItem) -> Unit = {},
    /** 알림이나 다른 탭에서 연 항목. Care 탭이 선택하고 나면 소비된다. */
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
    var editingServiceId by rememberSaveable { mutableStateOf<String?>(null) }
    var isUpdatingMileage by rememberSaveable { mutableStateOf(false) }
    var openedManageItem by rememberSaveable { mutableStateOf<MaintenanceItem?>(null) }
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
        onRecordService = {
            editingServiceId = null
            recordingItem = it
        },
        onEditService = { item, recordId ->
            editingServiceId = recordId
            recordingItem = item
        },
        onNavigate = navigateTo,
        onUpdateMileage = { isUpdatingMileage = true },
        onAskAi = { item -> backStack.add(AiRoute(item?.name)) },
        onOpenSettings = { backStack.add(SettingsRoute) },
        onOpenAccount = { backStack.add(AccountRoute) },
        onOpenAlbum = { backStack.add(AlbumRoute) },
        onOpenManageItem = { item ->
            openedManageItem = item
            navigateTo(TopLevelDestination.Manage)
        },
        pendingManageItem = (deepLink as? DeepLink.Maintenance)?.item ?: openedManageItem,
        onPendingManageItemHandled = {
            openedManageItem = null
            onDeepLinkHandled()
        },
    )
    LaunchedEffect(deepLink) {
        when (deepLink) {
            is DeepLink.Maintenance -> navigateTo(TopLevelDestination.Manage)
            DeepLink.Inspection -> {
                navigateTo(TopLevelDestination.Vehicle)
                onDeepLinkHandled()
            }
            DeepLink.MileageUpdate -> {
                navigateTo(TopLevelDestination.Home)
                isUpdatingMileage = true
                onDeepLinkHandled()
            }
            null -> Unit
        }
    }

    val itemColors = iosStyleItemColors()
    NavigationSuiteScaffold(
        layoutType = navigationSuiteTypeFor(windowSizeClass),
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surface,
            navigationRailContainerColor = MaterialTheme.colorScheme.background,
        ),
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = destination.route == currentTopLevel,
                    onClick = { navigateTo(destination) },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(stringResource(destination.labelRes)) },
                    colors = itemColors,
                )
            }
        },
    ) {
        // NavigationSuiteScaffold는 하단 바·레일 쪽 인셋만 처리하므로 상단은 여기서 한 번에 비운다.
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
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
                        onOpenAccount = { backStack.add(AccountRoute) },
                        onOpenDriveBackup = { backStack.add(DriveBackupRoute) },
                    )
                }
                entry(DriveBackupRoute) { DriveBackupScreenRoute(onBack = { backStack.removeLastOrNull() }) }
                entry(AccountRoute) {
                    AccountScreenRoute(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenEmail = { backStack.add(AccountEmailRoute) },
                        onOpenDeleteAccount = { backStack.add(AccountDeleteRoute) },
                    )
                }
                entry(AccountDeleteRoute) {
                    DeleteAccountRoute(
                        onBack = { backStack.removeLastOrNull() },
                        onDeleted = { backStack.remove(AccountDeleteRoute) },
                    )
                }
                entry(AccountEmailRoute) {
                    // 로그인이 끝나면 계정 화면으로 돌아가 로그인한 상태를 보여 준다.
                    EmailAuthRoute(
                        onBack = { backStack.removeLastOrNull() },
                        onCompleted = { backStack.remove(AccountEmailRoute) },
                    )
                }
                entry(OpenSourceLicensesRoute) {
                    OpenSourceLicensesRoute(R.raw.aboutlibraries, onBack = { backStack.removeLastOrNull() })
                }
                entry(AlbumRoute) { AlbumScreenRoute(onBack = { backStack.removeLastOrNull() }) }
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
        RecordServiceHost(
            item = item,
            isExpanded = isExpanded,
            onDismiss = {
                recordingItem = null
                editingServiceId = null
            },
            editingRecordId = editingServiceId,
        )
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
            onOpenItem = actions.onOpenManageItem,
            onOpenAccount = actions.onOpenAccount,
        )
        TopLevelDestination.Manage -> ManageRoute(
            onRecordService = actions.onRecordService,
            onAskAi = { actions.onAskAi(it) },
            pendingSelection = actions.pendingManageItem,
            onPendingSelectionHandled = actions.onPendingManageItemHandled,
        )
        TopLevelDestination.History -> HistoryRoute(
            onRecordService = actions.onRecordService,
            onEditService = actions.onEditService,
        )
        TopLevelDestination.Vehicle -> VehicleRoute(
            onUpdateMileage = actions.onUpdateMileage,
            onOpenSettings = actions.onOpenSettings,
            onOpenAlbum = actions.onOpenAlbum,
        )
    }
}

/** iOS 탭 바처럼 선택 표시 알약 없이 아이콘과 글자 색만으로 현재 탭을 나타낸다. */
@Composable
private fun iosStyleItemColors(): NavigationSuiteItemColors {
    val colors = MaterialTheme.colorScheme
    return NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedIconColor = colors.primary,
            selectedTextColor = colors.primary,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            indicatorColor = Color.Transparent,
            selectedIconColor = colors.primary,
            selectedTextColor = colors.primary,
            unselectedIconColor = colors.onSurfaceVariant,
            unselectedTextColor = colors.onSurfaceVariant,
        ),
    )
}
