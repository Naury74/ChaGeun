package com.naury.chageun.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.ThemeMode
import com.naury.chageun.core.notification.DeepLink
import com.naury.chageun.feature.onboarding.OnboardingRoute

@Composable
fun ChageunRoot(deepLink: DeepLink?, onDeepLinkHandled: () -> Unit, viewModel: AppViewModel = hiltViewModel()) {
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val windowSizeClass = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass

    ChageunTheme(darkTheme = darkTheme, spacing = spacingFor(windowSizeClass)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (entry) {
                // Room answers within a frame; an empty surface avoids flashing onboarding to returning users.
                AppEntry.Loading -> Box(Modifier.fillMaxSize())
                AppEntry.Onboarding -> OnboardingRoute()
                AppEntry.Main -> ChageunApp(deepLink = deepLink, onDeepLinkHandled = onDeepLinkHandled)
            }
        }
    }
}
