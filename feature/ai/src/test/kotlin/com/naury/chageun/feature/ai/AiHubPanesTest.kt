package com.naury.chageun.feature.ai

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.window.core.layout.WindowSizeClass
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.ai.AiContextFacts
import com.naury.chageun.core.domain.ai.AiContextOptions
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.captureScreen
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
class AiHubPanesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val facts = AiContextFacts(
        asOf = LocalDate.of(2026, 10, 1),
        maker = "KG Mobility",
        model = "Torres",
        modelYear = 2023,
        fuelType = FuelType.Gasoline,
        mileage = MileageReading(LocalDate.of(2026, 9, 30), Kilometers(42_180)),
        health = VehicleHealthLevel.Good,
        maintenance = emptyList(),
        missingInfo = emptyList(),
        recentRecords = emptyList(),
    )

    private fun setScreen(
        question: String = "How is my car?",
        installed: Set<AiProvider>? = null,
        onShare: (AiProvider) -> Unit = {},
    ) {
        composeRule.setContent {
            AppFrame {
                val state = AiHubUiState(question = question, options = AiContextOptions(), facts = facts)
                AiHubScreen(
                    uiState = state,
                    promptText = aiPromptText(facts, state.question),
                    actions = AiHubActions({}, {}, {}, {}, onShare = onShare),
                    installedProviders = installed,
                )
            }
        }
    }

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(tag).getBoundsInRoot()

    private fun textBounds(text: String): DpRect = composeRule.onNodeWithText(text).getBoundsInRoot()

    private fun assertInside(inner: DpRect, outer: DpRect) {
        assertThat(inner.left.value).isAtLeast(outer.left.value)
        assertThat(inner.right.value).isAtMost(outer.right.value)
    }

    private fun assertEachProviderOnce() {
        listOf("ChatGPT", "Claude", "Gemini", "Other app").forEach {
            composeRule.onAllNodesWithText(it).assertCountEquals(1)
        }
    }

    @Test
    fun paneCount_followsWidthBreakpoints() {
        fun countAt(widthDp: Int) = aiPaneCount(WindowSizeClass(minWidthDp = widthDp, minHeightDp = 800))

        assertThat(countAt(839)).isEqualTo(1)
        assertThat(countAt(840)).isEqualTo(2)
        assertThat(countAt(1599)).isEqualTo(2)
        assertThat(countAt(1600)).isEqualTo(3)
    }

    @Test
    @Config(qualifiers = "w1000dp-h800dp")
    fun expandedWidth_putsQuestionAndContextSideBySide() {
        var shared: AiProvider? = null
        setScreen(onShare = { shared = it })

        val question = bounds(AiHubTags.QUESTION_PANE)
        val context = bounds(AiHubTags.CONTEXT_PANE)
        assertThat(question.right.value).isAtMost(context.left.value)
        assertThat(question.top).isEqualTo(context.top)
        composeRule.onNodeWithTag(AiHubTags.PROVIDER_PANE).assertDoesNotExist()

        assertInside(textBounds("Pick a question"), question)
        assertInside(textBounds("Claude"), question)
        assertInside(textBounds("Plate number"), context)
        // 넓은 창에서는 보낼 글이 처음부터 펼쳐져 있다.
        composeRule.onNodeWithText("Hide full text").assertIsDisplayed()
        composeRule.onNodeWithText("Vehicle: KG Mobility Torres", substring = true).assertExists()
        assertEachProviderOnce()

        composeRule.onNodeWithText("Claude").performClick()
        assertThat(shared).isEqualTo(AiProvider.Claude)
    }

    @Test
    @Config(qualifiers = "w1700dp-h900dp")
    fun largeWidth_addsProviderPane_withEachActionOnce() {
        var shared: AiProvider? = null
        setScreen(installed = setOf(AiProvider.Claude), onShare = { shared = it })

        val question = bounds(AiHubTags.QUESTION_PANE)
        val context = bounds(AiHubTags.CONTEXT_PANE)
        val provider = bounds(AiHubTags.PROVIDER_PANE)
        assertThat(question.right.value).isAtMost(context.left.value)
        assertThat(context.right.value).isAtMost(provider.left.value)
        assertThat(provider.top).isEqualTo(question.top)

        assertInside(textBounds("Pick a question"), question)
        assertInside(textBounds("Owner name"), context)
        assertInside(textBounds("Gemini"), provider)
        assertInside(textBounds(DISCLAIMER), provider)
        assertEachProviderOnce()
        composeRule.onAllNodesWithText(DISCLAIMER).assertCountEquals(1)
        composeRule.onAllNodesWithText("Opens the app directly").assertCountEquals(1)
        composeRule.onAllNodesWithText("Not installed. Pick an app from the share sheet.").assertCountEquals(2)

        composeRule.onNodeWithText("Gemini").performClick()
        assertThat(shared).isEqualTo(AiProvider.Gemini)
    }

    @Test
    @Config(qualifiers = TWO_PANES_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_twoPanes_ko() {
        setScreen(question = "지금 내 차 상태 어때?")
        composeRule.captureScreen("ai_two_panes_ko")
    }

    @Test
    @Config(qualifiers = THREE_PANES_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_threePanes_ko() {
        setScreen(question = "지금 내 차 상태 어때?", installed = setOf(AiProvider.ChatGpt, AiProvider.Claude))
        composeRule.captureScreen("ai_three_panes_ko")
    }

    @Test
    @Config(qualifiers = "w1700dp-h900dp")
    fun widePanes_keepThemeSpacing() {
        // 칸 사이 간격이 테마의 paneGap보다 좁아지지 않는다.
        var paneGap = 0f
        composeRule.setContent {
            ChageunTheme {
                paneGap = ChageunTheme.spacing.paneGap.value
                AiHubScreen(
                    uiState = AiHubUiState(question = "How is my car?", facts = facts),
                    promptText = aiPromptText(facts, "How is my car?"),
                    actions = AiHubActions({}, {}, {}, {}, onShare = {}),
                )
            }
        }

        val gap = bounds(AiHubTags.CONTEXT_PANE).left - bounds(AiHubTags.QUESTION_PANE).right
        assertThat(gap.value).isAtLeast(paneGap)
    }

    private companion object {
        const val DISCLAIMER =
            "The AI answers from what's shared. Values are calculated by Chageun and aren't a mechanical diagnosis."
        const val TWO_PANES_KO = "ko-w1000dp-h800dp-hdpi"
        const val THREE_PANES_KO = "ko-w1700dp-h900dp-hdpi"
    }
}
