package com.naury.chageun.ui

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
import com.naury.chageun.feature.onboarding.OnboardingRoute

@Composable
fun ChageunRoot(viewModel: AppViewModel = hiltViewModel()) {
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val windowSizeClass = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass

    ChageunTheme(spacing = spacingFor(windowSizeClass)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (entry) {
                // Room answers within a frame; an empty surface avoids flashing onboarding to returning users.
                AppEntry.Loading -> Box(Modifier.fillMaxSize())
                AppEntry.Onboarding -> OnboardingRoute()
                AppEntry.Main -> ChageunApp()
            }
        }
    }
}
