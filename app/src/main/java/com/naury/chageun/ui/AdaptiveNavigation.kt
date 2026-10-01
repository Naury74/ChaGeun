package com.naury.chageun.ui

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.core.designsystem.theme.ChageunSpacing
import com.naury.chageun.core.designsystem.theme.Gutters

/** Navigation follows the current window width, never the device model, so split-screen tablets fall back to a bar. */
fun navigationSuiteTypeFor(windowSizeClass: WindowSizeClass): NavigationSuiteType = when {
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) ->
        NavigationSuiteType.NavigationDrawer

    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
        NavigationSuiteType.NavigationRail

    else -> NavigationSuiteType.NavigationBar
}

fun spacingFor(windowSizeClass: WindowSizeClass): ChageunSpacing = when {
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> Gutters.Expanded
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> Gutters.Medium
    else -> Gutters.Compact
}
