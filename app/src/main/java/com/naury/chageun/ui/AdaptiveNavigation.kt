package com.naury.chageun.ui

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.core.designsystem.theme.ChageunSpacing
import com.naury.chageun.core.designsystem.theme.Gutters

/** 내비게이션은 기기 모델이 아니라 현재 창 너비를 따른다. 그래서 화면 분할 중인 태블릿은 bar로 돌아간다. */
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
