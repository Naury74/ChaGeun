package com.naury.chageun.feature.home

import androidx.window.core.layout.WindowSizeClass
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomePaneCountTest {

    private fun paneCountAt(widthDp: Int) = homePaneCount(WindowSizeClass(minWidthDp = widthDp, minHeightDp = 800))

    @Test
    fun usesSinglePane_belowExpandedWidth() {
        assertThat(paneCountAt(360)).isEqualTo(1)
        assertThat(paneCountAt(839)).isEqualTo(1)
    }

    @Test
    fun usesTwoPanes_onExpandedWidth() {
        assertThat(paneCountAt(840)).isEqualTo(2)
        assertThat(paneCountAt(1199)).isEqualTo(2)
    }

    @Test
    fun usesThreePanes_fromLargeWidth() {
        assertThat(paneCountAt(1200)).isEqualTo(3)
    }
}
