package com.naury.chageun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.R
import com.naury.chageun.core.ads.LocalAdsEnabled
import com.naury.chageun.core.designsystem.theme.ChageunGlass
import com.naury.chageun.core.designsystem.theme.LocalBottomBarPadding
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.notification.DeepLink
import com.naury.chageun.core.ui.LocalFeatureFlags
import com.naury.chageun.feature.account.AccountRoute as AccountScreenRoute
import com.naury.chageun.feature.account.DeleteAccountRoute
import com.naury.chageun.feature.account.DriveBackupRoute as DriveBackupScreenRoute
import com.naury.chageun.feature.account.EmailAuthRoute
import com.naury.chageun.feature.ai.AiHubRoute
import com.naury.chageun.feature.history.HistoryCalendarLinks
import com.naury.chageun.feature.history.HistoryRoute
import com.naury.chageun.feature.home.HomeRoute
import com.naury.chageun.feature.home.MileageUpdateHost
import com.naury.chageun.feature.manage.ManageRoute
import com.naury.chageun.feature.manage.record.RecordServiceHost
import com.naury.chageun.feature.settings.OpenSourceLicensesRoute
import com.naury.chageun.feature.settings.PrivacyNoticeScreen
import com.naury.chageun.feature.settings.ServiceNoticeScreen
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
import com.naury.chageun.navigation.ServiceNoticeRoute
import com.naury.chageun.navigation.SettingsRoute
import com.naury.chageun.navigation.TopLevelDestination
import com.naury.chageun.navigation.TopLevelRoute
import com.naury.kinetica.ui.KNavTransitions
import com.naury.kinetica.ui.KNavigationRail
import com.naury.kinetica.ui.KTab
import com.naury.kinetica.ui.KTabBar
import com.naury.kinetica.ui.KineticaScaffold
import com.naury.kinetica.ui.KineticaTheme
import com.naury.kinetica.ui.kTransform
import com.naury.kinetica.ui.rememberKNavTransition
import java.time.LocalDate

/** feature 모듈끼리 서로 의존하지 않도록 앱 셸이 처리하는 feature 간 액션. */
data class AppActions(
    val onRecordService: (MaintenanceItem) -> Unit,
    /** 저장된 정비 기록을 같은 교체 기록 시트로 고친다. */
    val onEditService: (MaintenanceItem, String) -> Unit = { _, _ -> },
    val onNavigate: (TopLevelDestination) -> Unit,
    val onUpdateMileage: () -> Unit = {},
    val onAskAi: (MaintenanceItem?) -> Unit = {},
    /** 기록 상세에서 그 기록을 AI에게 묻는다. */
    val onAskAiAboutRecord: (RecordRef) -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenAccount: () -> Unit = {},
    val onOpenAlbum: () -> Unit = {},
    /** 다른 탭에서 관리 탭의 항목 상세를 연다. */
    val onOpenManageItem: (MaintenanceItem) -> Unit = {},
    /** 알림이나 다른 탭에서 연 항목. Care 탭이 선택하고 나면 소비된다. */
    val pendingManageItem: MaintenanceItem? = null,
    val onPendingManageItemHandled: () -> Unit = {},
    /** 알림이나 홈에서 검사를 열면 내 차 탭이 검사 카드까지 내려가 보여 준다. */
    val onOpenInspection: () -> Unit = {},
    val pendingInspection: Boolean = false,
    val onPendingInspectionHandled: () -> Unit = {},
    /** 홈·앨범에서 기록 탭 달력을 그 날짜로 연다. */
    val onOpenCalendar: (LocalDate) -> Unit = {},
    val pendingCalendarDate: LocalDate? = null,
    val onPendingCalendarDateHandled: () -> Unit = {},
    /** 달력에서 누른 사진을 앨범에서 연다. */
    val onOpenAlbumPhoto: (String) -> Unit = {},
    /** 달력의 [이날 기록 추가]에서 교체 기록 창을 그 날짜로 연다. */
    val onRecordServiceOn: (MaintenanceItem, LocalDate) -> Unit = { item, _ -> onRecordService(item) },
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
    var recordingDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var calendarDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    // 입력 시트를 열 때마다 늘린다. 회전·접기에는 유지되고, 다시 열면 새 ViewModel을 쓴다.
    var recordSession by rememberSaveable { mutableIntStateOf(0) }
    var mileageSession by rememberSaveable { mutableIntStateOf(0) }
    var isUpdatingMileage by rememberSaveable { mutableStateOf(false) }
    var openedManageItem by rememberSaveable { mutableStateOf<MaintenanceItem?>(null) }
    var isOpeningInspection by rememberSaveable { mutableStateOf(false) }
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
            recordingDate = null
            recordSession++
            recordingItem = it
        },
        onEditService = { item, recordId ->
            editingServiceId = recordId
            recordingDate = null
            recordSession++
            recordingItem = item
        },
        onNavigate = navigateTo,
        onUpdateMileage = {
            mileageSession++
            isUpdatingMileage = true
        },
        onAskAi = { item -> backStack.add(AiRoute(item?.name)) },
        onAskAiAboutRecord = { ref -> backStack.add(AiRoute(focusRecord = "${ref.type.name}:${ref.id}")) },
        onOpenSettings = { backStack.add(SettingsRoute) },
        onOpenAccount = { backStack.add(AccountRoute) },
        onOpenAlbum = { backStack.add(AlbumRoute()) },
        onOpenManageItem = { item ->
            openedManageItem = item
            navigateTo(TopLevelDestination.Manage)
        },
        pendingManageItem = (deepLink as? DeepLink.Maintenance)?.item ?: openedManageItem,
        onPendingManageItemHandled = {
            openedManageItem = null
            onDeepLinkHandled()
        },
        onOpenInspection = {
            isOpeningInspection = true
            navigateTo(TopLevelDestination.Vehicle)
        },
        pendingInspection = isOpeningInspection,
        onPendingInspectionHandled = { isOpeningInspection = false },
        onOpenCalendar = { date ->
            calendarDate = date
            // 앨범처럼 탭 위에 겹쳐 연 화면에서 오면, 같은 탭이어도 겹친 화면을 닫아야 달력이 보인다.
            backStack.popToTopLevel()
            navigateTo(TopLevelDestination.History)
        },
        pendingCalendarDate = calendarDate,
        onPendingCalendarDateHandled = { calendarDate = null },
        onOpenAlbumPhoto = { id -> backStack.add(AlbumRoute(focusPhotoId = id)) },
        onRecordServiceOn = { item, date ->
            editingServiceId = null
            recordingDate = date
            recordSession++
            recordingItem = item
        },
    )
    val adFreeDestination = rememberAdFreeDestination(deepLink, currentTopLevel)
    LaunchedEffect(deepLink) {
        when (deepLink) {
            is DeepLink.Maintenance -> navigateTo(TopLevelDestination.Manage)
            DeepLink.Inspection -> {
                isOpeningInspection = true
                navigateTo(TopLevelDestination.Vehicle)
                onDeepLinkHandled()
            }
            DeepLink.MileageUpdate -> {
                navigateTo(TopLevelDestination.Home)
                mileageSession++
                isUpdatingMileage = true
                onDeepLinkHandled()
            }
            null -> Unit
        }
    }

    // 앨범 사진 보기처럼 화면 전체를 쓰는 동안에는 하단 탭 바를 숨긴다.
    var isImmersive by remember { mutableStateOf(false) }
    val suiteType = navigationSuiteTypeFor(windowSizeClass, isImmersive)
    val tabs = TopLevelDestination.entries.map { KTab(stringResource(it.labelRes), it.icon) }
    val selectedTab = TopLevelDestination.entries.indexOfFirst { it.route == currentTopLevel }.coerceAtLeast(0)
    val push = rememberKNavTransition(KNavTransitions.Push)
    val fade = rememberKNavTransition(KNavTransitions.FadeThrough)
    ChageunShell(
        suiteType = suiteType,
        tabs = tabs,
        selectedTab = selectedTab,
        onSelectTab = { navigateTo(TopLevelDestination.entries[it]) },
        overlays = {
            // 입력 시트도 스캐폴드 안에서 띄워야 뒤 화면을 블러로 비춘다.
            recordingItem?.let { item ->
                RecordServiceHost(
                    item = item,
                    isExpanded = isExpanded,
                    onDismiss = {
                        recordingItem = null
                        editingServiceId = null
                        recordingDate = null
                    },
                    editingRecordId = editingServiceId,
                    sessionKey = recordSession,
                    initialDate = recordingDate,
                )
            }
            if (isUpdatingMileage) {
                MileageUpdateHost(
                    isExpanded = isExpanded,
                    onDismiss = { isUpdatingMileage = false },
                    sessionKey = mileageSession,
                )
            }
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            // 탭을 바꾸면 스택이 탭 화면 하나로 바뀌므로 겹쳐 밀지 않고 옅게 바꾸고, 그 위에 연 화면은 iOS처럼 옆에서 밀어 넣는다.
            // 뒤로 가기 제스처 동안에는 손가락을 따라 되감긴다.
            transitionSpec = { kTransform(if (backStack.size <= 1) fade else push, forward = true) },
            popTransitionSpec = { kTransform(push, forward = false) },
            predictivePopTransitionSpec = { kTransform(push, forward = false) },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
                remember { topInsetDecorator() },
            ),
            entryProvider = entryProvider {
                TopLevelDestination.entries.forEach { destination ->
                    // 홈은 Hero의 하늘 바탕을 상태 표시줄 뒤까지 그리고, 상단 인셋은 화면 안에서 비운다.
                    val metadata = if (destination ==
                        TopLevelDestination.Home
                    ) {
                        DRAWS_BEHIND_STATUS_BAR
                    } else {
                        emptyMap()
                    }
                    entry(destination.route, metadata = metadata) {
                        WithoutAds(hidden = adFreeDestination == destination) {
                            destinationContent(destination, actions)
                        }
                    }
                }
                entry(SettingsRoute) {
                    SettingsRoute(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenLicenses = { backStack.add(OpenSourceLicensesRoute) },
                        onOpenPrivacy = { backStack.add(PrivacyNoticeRoute) },
                        onOpenServiceNotice = { backStack.add(ServiceNoticeRoute) },
                        onOpenAccount = { backStack.add(AccountRoute) },
                        onOpenDriveBackup = { backStack.add(DriveBackupRoute) },
                    )
                }
                entry(DriveBackupRoute) {
                    DriveBackupScreenRoute(onBack = { backStack.removeLastOrNull() })
                }
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
                // 앨범의 사진 보기는 검은 바탕을 상태 표시줄 뒤까지 그린다. 그리드는 화면 안에서 상단 인셋을 비운다.
                entry<AlbumRoute>(metadata = DRAWS_BEHIND_STATUS_BAR) { route ->
                    AlbumScreenRoute(
                        onBack = { backStack.removeLastOrNull() },
                        focusPhotoId = route.focusPhotoId,
                        onOpenCalendar = actions.onOpenCalendar,
                        onImmersiveChange = { isImmersive = it },
                    )
                }
                entry(PrivacyNoticeRoute) { PrivacyNoticeScreen(onBack = { backStack.removeLastOrNull() }) }
                entry(ServiceNoticeRoute) { ServiceNoticeScreen(onBack = { backStack.removeLastOrNull() }) }
                entry<AiRoute> { route ->
                    AiHubRoute(
                        focusItem = route.focusItem?.let { name ->
                            MaintenanceItem.entries.firstOrNull { it.name == name }
                        },
                        onBack = { backStack.removeLastOrNull() },
                        focusRecord = route.focusRecord?.let { encoded ->
                            TimelineEventType.entries.firstOrNull {
                                it.name == encoded.substringBefore(':')
                            }
                                ?.let { type -> RecordRef(type, encoded.substringAfter(':')) }
                        },
                    )
                }
            },
        )
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
            onOpenInspection = actions.onOpenInspection,
            onOpenItem = actions.onOpenManageItem,
            onOpenAccount = actions.onOpenAccount,
            onOpenCalendar = actions.onOpenCalendar,
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
            onAskAi = actions.onAskAiAboutRecord.takeIf { LocalFeatureFlags.current.aiShareEnabled },
            calendarLinks = HistoryCalendarLinks(
                onOpenManageItem = actions.onOpenManageItem,
                onOpenInspection = actions.onOpenInspection,
                onUpdateMileage = actions.onUpdateMileage,
                onOpenPhoto = actions.onOpenAlbumPhoto,
                onRecordServiceOn = actions.onRecordServiceOn,
                pendingDate = actions.pendingCalendarDate,
                onPendingDateHandled = actions.onPendingCalendarDateHandled,
            ),
        )
        TopLevelDestination.Vehicle -> VehicleRoute(
            onUpdateMileage = actions.onUpdateMileage,
            onOpenSettings = actions.onOpenSettings,
            onOpenAlbum = actions.onOpenAlbum,
            showInspection = actions.pendingInspection,
            onInspectionShown = actions.onPendingInspectionHandled,
        )
    }
}

private val DRAWS_BEHIND_STATUS_BAR = mapOf("drawsBehindStatusBar" to true)

/**
 * NavigationSuiteScaffold는 하단 바·레일 쪽 인셋만 처리하므로 상단 인셋은 화면마다 비운다.
 * [DRAWS_BEHIND_STATUS_BAR]가 있는 화면은 바탕을 상태 표시줄 뒤까지 그리고 인셋을 직접 처리한다.
 */
private fun <T : Any> topInsetDecorator() = NavEntryDecorator<T> { entry ->
    val drawsBehind = entry.metadata.keys.containsAll(DRAWS_BEHIND_STATUS_BAR.keys)
    Box(
        modifier = if (drawsBehind) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
        },
    ) { entry.Content() }
}

/** 알림이 여는 탭. */
private val DeepLink.destination: TopLevelDestination
    get() = when (this) {
        is DeepLink.Maintenance -> TopLevelDestination.Manage
        DeepLink.Inspection -> TopLevelDestination.Vehicle
        DeepLink.MileageUpdate -> TopLevelDestination.Home
    }

/** 알림을 눌러 들어온 탭에는 광고를 띄우지 않는다(기획 §17.1). 다른 탭으로 옮기면 다시 보인다. */
@Composable
private fun rememberAdFreeDestination(deepLink: DeepLink?, currentTopLevel: TopLevelRoute): TopLevelDestination? {
    var adFreeDestination by rememberSaveable { mutableStateOf<TopLevelDestination?>(null) }
    LaunchedEffect(deepLink) {
        deepLink?.let { adFreeDestination = it.destination }
    }
    // 알림이 탭을 여는 중에도 탭이 바뀌므로, 그 탭에 있다가 다른 탭으로 옮겼을 때만 푼다.
    var lastTopLevel by remember { mutableStateOf(currentTopLevel) }
    LaunchedEffect(currentTopLevel) {
        if (lastTopLevel == adFreeDestination?.route && currentTopLevel != lastTopLevel) adFreeDestination = null
        lastTopLevel = currentTopLevel
    }
    return adFreeDestination
}

@Composable
private fun WithoutAds(hidden: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAdsEnabled provides (LocalAdsEnabled.current && !hidden), content = content)
}

/** 탭 화면 위에 겹쳐 연 화면(앨범, 설정 등)을 모두 닫는다. */
private fun MutableList<NavKey>.popToTopLevel() {
    while (size > 1) removeAt(lastIndex)
}

/** 화면 아래 떠 있는 탭 바를 쓰는 배치인지. 그 밖의 배치(레일, 서랍)는 왼쪽 세로 레일로 그린다. */
private fun NavigationSuiteType.isBottomBar(): Boolean = this == NavigationSuiteType.NavigationBar ||
    this == NavigationSuiteType.ShortNavigationBarCompact ||
    this == NavigationSuiteType.ShortNavigationBarMedium

/**
 * 앱 골격. 탭 바, 시트, 대화상자, 메뉴, 토스트는 글래스로 뒤 화면 전체를 블러로 비추고, 목록과 카드는 차근 단색 그대로다.
 * 스캐폴드 안에서 그리므로 시스템 창 블러가 꺼진 기기에서도 블러가 보인다. [overlays]의 입력 시트도 같은 이유로 안에서 띄운다.
 */
@Composable
private fun ChageunShell(
    suiteType: NavigationSuiteType,
    tabs: List<KTab>,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    overlays: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val isBar = suiteType.isBottomBar()
    KineticaScaffold(
        surface = ChageunGlass.chrome,
        contentSurface = KineticaTheme.surface,
        // 상태 표시줄 아이콘은 앱 테마에 맞춰 ChageunRoot가 정한다.
        systemBarIcons = false,
        bottomBar = { if (isBar) KTabBar(tabs, selectedTab, onSelectTab, Modifier.fillMaxWidth()) },
    ) { scaffoldPadding ->
        // 목록은 탭 바 뒤까지 그려 탭 바 너머로 비치게 하고, 끝 여백만 탭 바 높이만큼 둔다.
        CompositionLocalProvider(LocalBottomBarPadding provides scaffoldPadding.calculateBottomPadding()) {
            // 레일 칸은 늘 같은 자리에 두어, 접고 펼 때 화면 상태가 처음부터 다시 만들어지지 않게 한다.
            Row(Modifier.fillMaxSize()) {
                Box {
                    if (!isBar && suiteType != NavigationSuiteType.None) {
                        KNavigationRail(tabs, selectedTab, onSelectTab, Modifier.fillMaxHeight())
                    }
                }
                Box(Modifier.weight(1f)) { content() }
            }
            overlays()
        }
    }
}
