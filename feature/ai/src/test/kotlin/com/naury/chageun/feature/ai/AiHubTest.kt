package com.naury.chageun.feature.ai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.ai.AiContextFacts
import com.naury.chageun.core.domain.ai.AiContextOptions
import com.naury.chageun.core.domain.ai.BuildAiContextUseCase
import com.naury.chageun.core.domain.maintenance.DrivingPaceEstimator
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.maintenance.RuleBasedMaintenanceEngine
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeInspectionRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h3000dp")
class AiHubTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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

    private fun viewModel(): AiHubViewModel {
        val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
        val maintenance = FakeMaintenanceRepository()
        return AiHubViewModel(
            focusItem = null,
            savedStateHandle = SavedStateHandle(),
            buildContext = BuildAiContextUseCase(
                FakeVehicleRepository(),
                ObserveMaintenanceOverviewUseCase(
                    maintenance,
                    FakeInspectionRepository(),
                    RuleBasedMaintenanceEngine(DrivingPaceEstimator()),
                    VehicleHealthAggregator(),
                    clock,
                ),
                FakeHistoryRepository(),
                clock,
            ),
        )
    }

    @Test
    fun blocksSharing_untilQuestionIsEntered() {
        val vm = viewModel()

        assertThat(vm.validateBeforeShare()).isFalse()
        vm.onQuestionChanged("지금 내 차 상태 어때?")
        assertThat(vm.validateBeforeShare()).isTrue()
    }

    @Test
    fun previewListsExcludedData_andPromptCarriesQuestionWithoutPlate() {
        var shared: AiProvider? = null
        var prompt = ""
        composeRule.setContent {
            ChageunTheme {
                val state = AiHubUiState(question = "How is my car?", options = AiContextOptions(), facts = facts)
                prompt = aiPromptText(facts, state.question)
                AiHubScreen(
                    uiState = state,
                    promptText = prompt,
                    actions = AiHubActions({}, {}, {}, {}, onShare = { shared = it }),
                )
            }
        }

        composeRule.onNodeWithText("Plate number").assertIsDisplayed()
        composeRule.onNodeWithText("Owner name").assertIsDisplayed()
        composeRule.onNodeWithText("Claude").performClick()

        assertThat(shared).isEqualTo(AiProvider.Claude)
        assertThat(prompt).contains("Vehicle: KG Mobility Torres · 2023 · Gasoline")
        assertThat(prompt).contains("Mileage: 42,180 km")
        assertThat(prompt).endsWith("Question: How is my car?")
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun largeFont_keepsPreviewAndProvidersVisible() {
        composeRule.setContent {
            ChageunTheme {
                val state = AiHubUiState(question = "How is my car?", options = AiContextOptions(), facts = facts)
                AiHubScreen(
                    uiState = state,
                    promptText = aiPromptText(facts, state.question),
                    actions = AiHubActions({}, {}, {}, {}, onShare = {}),
                )
            }
        }

        composeRule.assertNoClippedText()
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_phone() {
        composeRule.setContent {
            AppFrame {
                val state = AiHubUiState(question = "How is my car?", options = AiContextOptions(), facts = facts)
                AiHubScreen(
                    uiState = state,
                    promptText = aiPromptText(facts, state.question),
                    actions = AiHubActions({
                    }, {}, {}, {}, onShare = {}),
                )
            }
        }
        composeRule.captureScreen("ai_phone")
    }
}
