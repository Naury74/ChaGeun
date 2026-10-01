package com.naury.chageun.ui

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.Gutters
import org.junit.Test

class AdaptiveNavigationTest {

    private fun widthOf(dp: Int) = WindowSizeClass(minWidthDp = dp, minHeightDp = 800)

    @Test
    fun usesBottomBar_belowMediumWidth() {
        assertThat(navigationSuiteTypeFor(widthOf(360))).isEqualTo(NavigationSuiteType.NavigationBar)
        assertThat(navigationSuiteTypeFor(widthOf(599))).isEqualTo(NavigationSuiteType.NavigationBar)
    }

    @Test
    fun usesRail_fromMediumUntilLargeWidth() {
        assertThat(navigationSuiteTypeFor(widthOf(600))).isEqualTo(NavigationSuiteType.NavigationRail)
        assertThat(navigationSuiteTypeFor(widthOf(1199))).isEqualTo(NavigationSuiteType.NavigationRail)
    }

    @Test
    fun usesPermanentDrawer_fromLargeWidth() {
        assertThat(navigationSuiteTypeFor(widthOf(1200))).isEqualTo(NavigationSuiteType.NavigationDrawer)
    }

    @Test
    fun widensGutter_withWindowWidth() {
        assertThat(spacingFor(widthOf(360))).isEqualTo(Gutters.Compact)
        assertThat(spacingFor(widthOf(700))).isEqualTo(Gutters.Medium)
        assertThat(spacingFor(widthOf(840))).isEqualTo(Gutters.Expanded)
    }
}
