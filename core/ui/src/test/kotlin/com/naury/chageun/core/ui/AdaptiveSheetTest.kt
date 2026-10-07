package com.naury.chageun.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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

/** 반쯤 접은 폴드에서 입력 창이 접히는 선을 가로지르지 않는지(기획 §18.3·§18.4). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w800dp-h800dp-mdpi")
class AdaptiveSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(hinge: Hinge) = composeRule.setContent {
        ChageunTheme {
            AdaptiveSheet(isExpanded = true, onDismiss = {}, hinge = hinge) {
                Box(Modifier.size(300.dp).testTag("form"))
            }
        }
    }

    @Test
    fun bookPosture_putsTheFormBesideTheFold() {
        show(Hinge(start = 400f, end = 400f, isVertical = true))

        assertThat(
            composeRule.onNodeWithTag("form", useUnmergedTree = true).getBoundsInRoot().left.value,
        ).isAtLeast(400f)
    }

    @Test
    fun tabletopPosture_putsTheFormBelowTheFold() {
        show(Hinge(start = 380f, end = 420f, isVertical = false))

        assertThat(
            composeRule.onNodeWithTag("form", useUnmergedTree = true).getBoundsInRoot().top.value,
        ).isAtLeast(420f)
    }
}
