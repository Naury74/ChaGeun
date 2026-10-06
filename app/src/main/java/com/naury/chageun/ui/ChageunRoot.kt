package com.naury.chageun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.ThemeMode
import com.naury.chageun.core.notification.DeepLink
import com.naury.chageun.feature.account.AccountRoute
import com.naury.chageun.feature.account.EmailAuthRoute
import com.naury.chageun.feature.onboarding.OnboardingRoute

@Composable
fun ChageunRoot(
    deepLink: DeepLink?,
    onDeepLinkHandled: () -> Unit,
    onMainShown: () -> Unit = {},
    viewModel: AppViewModel = hiltViewModel(),
) {
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val windowSizeClass = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass

    ChageunTheme(darkTheme = darkTheme, spacing = spacingFor(windowSizeClass)) {
        // 화면 바탕은 background, 카드와 Hero는 surface를 써서 바탕과 구분한다.
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (entry) {
                // Room은 한 프레임 안에 응답한다. 빈 surface를 두어 기존 사용자에게 온보딩이 잠깐 비치지 않게 한다.
                AppEntry.Loading -> Box(Modifier.fillMaxSize())
                AppEntry.Onboarding -> OnboardingEntry()
                AppEntry.Main -> {
                    LaunchedEffect(Unit) { onMainShown() }
                    ChageunApp(deepLink = deepLink, onDeepLinkHandled = onDeepLinkHandled)
                }
            }
        }
    }
}

/** 온보딩 중 "백업에서 복원하기"로 들어가는 로그인·복원 화면. 복원으로 차량이 생기면 AppEntry가 Main으로 바뀐다. */
private enum class RestoreScreen { None, Account, Email }

@Composable
private fun OnboardingEntry() {
    var screen by rememberSaveable { mutableStateOf(RestoreScreen.None) }
    BackHandler(enabled = screen != RestoreScreen.None) {
        screen = if (screen == RestoreScreen.Email) RestoreScreen.Account else RestoreScreen.None
    }
    // 온보딩은 스스로 시스템 영역을 비운다. 계정 화면은 앱 셸이 비워 주던 것을 여기서 대신한다.
    val insets = if (screen == RestoreScreen.None) Modifier else Modifier.safeDrawingPadding()
    Box(Modifier.fillMaxSize().then(insets)) {
        when (screen) {
            RestoreScreen.None -> OnboardingRoute(onRestoreFromBackup = { screen = RestoreScreen.Account })
            RestoreScreen.Account -> AccountRoute(
                onBack = { screen = RestoreScreen.None },
                onOpenEmail = { screen = RestoreScreen.Email },
                isRestoreMode = true,
            )
            RestoreScreen.Email -> EmailAuthRoute(
                onBack = { screen = RestoreScreen.Account },
                onCompleted = { screen = RestoreScreen.Account },
            )
        }
    }
}
