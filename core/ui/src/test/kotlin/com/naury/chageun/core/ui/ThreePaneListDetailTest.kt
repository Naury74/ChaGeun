package com.naury.chageun.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1400dp-h900dp-mdpi")
class ThreePaneListDetailTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun placesFixedSupportingPane_thenSplitsTheRest() {
        composeRule.setContent {
            ChageunTheme {
                ThreePaneListDetail(
                    selected = null as String?,
                    supporting = { Box(Modifier.fillMaxSize().testTag("supporting")) },
                    list = { Box(Modifier.fillMaxSize().testTag("list")) },
                    detail = {},
                    emptyDetail = { Box(Modifier.fillMaxSize().testTag("empty")) },
                    supportingWidth = 300.dp,
                    listFraction = 0.5f,
                    gap = 20.dp,
                )
            }
        }

        // 1400 - 300 - 20 * 2 = 1060을 목록과 상세가 반씩 나눈다.
        val supporting = composeRule.onNodeWithTag("supporting").getBoundsInRoot()
        val list = composeRule.onNodeWithTag("list").getBoundsInRoot()
        val empty = composeRule.onNodeWithTag("empty").getBoundsInRoot()
        assertThat(supporting.right.value).isEqualTo(300f)
        assertThat(list.left.value).isEqualTo(320f)
        assertThat(list.right.value).isEqualTo(850f)
        assertThat(empty.left.value).isEqualTo(870f)
        assertThat(empty.right.value).isEqualTo(1400f)
    }

    @Test
    fun showsPlaceholder_untilSelection() {
        var selected by mutableStateOf<String?>(null)
        composeRule.setContent {
            ChageunTheme {
                ThreePaneListDetail(
                    selected = selected,
                    supporting = { Text("Filters") },
                    list = { Text("List") },
                    detail = { Text("Detail $it") },
                    emptyDetail = { Text("Pick one") },
                )
            }
        }
        composeRule.onNodeWithText("Pick one").assertIsDisplayed()

        selected = "A"
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Detail A").assertIsDisplayed()
        composeRule.onNodeWithText("Pick one").assertDoesNotExist()
        composeRule.onNodeWithText("Filters").assertIsDisplayed()
    }

    @Test
    fun threePanes_startAtLargeWidth() {
        assertThat(isListDetailThreePane(WindowSizeClass(minWidthDp = 1199, minHeightDp = 800))).isFalse()
        assertThat(isListDetailThreePane(WindowSizeClass(minWidthDp = 1200, minHeightDp = 800))).isTrue()
        assertThat(isListDetailThreePane(WindowSizeClass(minWidthDp = 1600, minHeightDp = 800))).isTrue()
    }
}
