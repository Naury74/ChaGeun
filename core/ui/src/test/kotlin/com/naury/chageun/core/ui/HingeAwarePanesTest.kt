package com.naury.chageun.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w800dp-h600dp-mdpi")
class HingeAwarePanesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(hinge: Hinge?, stacked: Boolean = false) = composeRule.setContent {
        ChageunTheme {
            HingeAwarePanes(
                weights = listOf(1f, 1f),
                modifier = Modifier.fillMaxSize(),
                stacked = stacked,
                gap = 16.dp,
                hinge = hinge,
            ) {
                Box(Modifier.fillMaxSize().testTag("first"))
                Box(Modifier.fillMaxSize().testTag("second"))
            }
        }
    }

    @Test
    fun bookHinge_keepsPanesOnEitherSide() {
        show(Hinge(start = 300f, end = 300f, isVertical = true))

        assertThat(composeRule.onNodeWithTag("first").getBoundsInRoot().right.value).isAtMost(292f)
        assertThat(composeRule.onNodeWithTag("second").getBoundsInRoot().left.value).isAtLeast(308f)
    }

    @Test
    fun tabletopHinge_splitsStackedPanes() {
        show(Hinge(start = 290f, end = 310f, isVertical = false), stacked = true)

        assertThat(composeRule.onNodeWithTag("first").getBoundsInRoot().bottom.value).isAtMost(290f)
        assertThat(composeRule.onNodeWithTag("second").getBoundsInRoot().top.value).isAtLeast(310f)
    }

    @Test
    fun noHinge_splitsEvenly() {
        show(hinge = null)

        assertThat(composeRule.onNodeWithTag("first").getBoundsInRoot().right.value).isEqualTo(392f)
        assertThat(composeRule.onNodeWithTag("second").getBoundsInRoot().left.value).isEqualTo(408f)
    }

    @Test
    fun hingeAcrossOtherAxis_isIgnored() {
        show(Hinge(start = 100f, end = 100f, isVertical = false))

        assertThat(composeRule.onNodeWithTag("first").getBoundsInRoot().right.value).isEqualTo(392f)
    }
}
