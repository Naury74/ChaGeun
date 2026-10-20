package com.naury.chageun.feature.ai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.ai.AiContextFacts
import com.naury.chageun.core.domain.ai.AiContextOptions
import com.naury.chageun.core.domain.ai.BuildAiContextUseCase
import com.naury.chageun.core.domain.ai.OnDeviceAiStatus
import com.naury.chageun.core.domain.ai.SharedRecord
import com.naury.chageun.core.domain.analytics.AiTarget
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.maintenance.DrivingPaceEstimator
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.maintenance.RuleBasedMaintenanceEngine
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeAnalyticsTracker
import com.naury.chageun.core.testing.FakeHistoryRepository
import com.naury.chageun.core.testing.FakeInspectionRepository
import com.naury.chageun.core.testing.FakeMaintenanceRepository
import com.naury.chageun.core.testing.FakeOnDeviceAssistant
import com.naury.chageun.core.testing.FakeVehicleRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertAccessibleControls
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
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
    private val analytics = FakeAnalyticsTracker()
    private val vehicles = FakeVehicleRepository()

    @Before
    fun registerVehicle() = runTest {
        vehicles.register(VehicleRegistration("KG Mobility", "Torres", 2023, FuelType.Gasoline, Kilometers(42_180)))
    }

    private fun viewModel(onDevice: FakeOnDeviceAssistant = FakeOnDeviceAssistant()): AiHubViewModel {
        val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
        val maintenance = FakeMaintenanceRepository()
        return AiHubViewModel(
            focusItem = null,
            focusRecord = null,
            savedStateHandle = SavedStateHandle(),
            buildContext = BuildAiContextUseCase(
                vehicles,
                ObserveMaintenanceOverviewUseCase(
                    maintenance,
                    FakeInspectionRepository(),
                    RuleBasedMaintenanceEngine(DrivingPaceEstimator()),
                    VehicleHealthAggregator(),
                    clock,
                ),
                FakeHistoryRepository(),
                FakeInspectionRepository(),
                clock,
            ),
            onDeviceAssistant = onDevice,
            analytics = analytics,
        )
    }

    @Test
    fun blocksSending_untilQuestionIsEntered() {
        val vm = viewModel()

        assertThat(vm.validateBeforeShare()).isFalse()
        vm.onQuestionChanged("지금 내 차 상태 어때?")
        assertThat(vm.validateBeforeShare()).isTrue()
    }

    @Test
    fun startsWithOnDeviceAi_whenSupported_otherwiseChatGpt() = runTest {
        val supported = viewModel(FakeOnDeviceAssistant(OnDeviceAiStatus.Downloadable))
        backgroundScope.launch { supported.uiState.collect {} }
        assertThat(supported.uiState.first { it.onDevice.status != null }.destination).isEqualTo(AiDestination.OnDevice)

        val unsupported = viewModel(FakeOnDeviceAssistant(OnDeviceAiStatus.Unavailable))
        backgroundScope.launch { unsupported.uiState.collect {} }
        assertThat(
            unsupported.uiState.first {
                it.onDevice.status != null
            }.destination,
        ).isEqualTo(AiDestination.ChatGpt)

        unsupported.selectDestination(AiDestination.Claude)
        assertThat(unsupported.uiState.first { it.destination == AiDestination.Claude }).isNotNull()
    }

    @Test
    fun onDeviceAnswer_streamsIntoConversation_andFollowUpCarriesHistory() = runTest {
        val assistant = FakeOnDeviceAssistant()
        val vm = viewModel(assistant)
        backgroundScope.launch { vm.uiState.collect {} }
        vm.uiState.first { it.onDevice.status != null }

        vm.onQuestionChanged("When should I change the oil?")
        vm.ask("CONTEXT 1", "INSTRUCTION", "History:")
        val answered = vm.uiState.first { it.messages.size == 2 && !it.isAnswering }
        assertThat(answered.messages.map { it.role }).containsExactly(ChatRole.User, ChatRole.Assistant).inOrder()
        assertThat(answered.messages.last().text).isEqualTo(assistant.reply)
        assertThat(answered.question).isEmpty()
        assertThat(assistant.lastPrompt).isEqualTo("CONTEXT 1")
        assertThat(analytics.events).contains(AnalyticsEvent.AiShareCompleted(AiTarget.OnDevice))

        vm.onQuestionChanged("And the tires?")
        vm.ask("CONTEXT 2", "INSTRUCTION", "History:")
        vm.uiState.first { it.messages.size == 4 && !it.isAnswering }
        assertThat(assistant.lastPrompt).startsWith("History:\nQ: When should I change the oil?\nA: ${assistant.reply}")
        assertThat(assistant.lastPrompt).endsWith("CONTEXT 2")

        vm.clearConversation()
        assertThat(vm.uiState.first { it.messages.isEmpty() }.messages).isEmpty()
    }

    @Test
    fun setupCard_downloadsOnlyWhenTapped_thenAnswers() = runTest {
        val assistant = FakeOnDeviceAssistant(OnDeviceAiStatus.Downloadable)
        val vm = viewModel(assistant)
        backgroundScope.launch { vm.uiState.collect {} }
        assertThat(vm.uiState.first { it.onDevice.status != null }.needsOnDeviceSetup).isTrue()

        // 준비 전에는 보내도 아무 일도 없고 몰래 내려받지도 않는다.
        vm.onQuestionChanged("How is my car?")
        vm.ask("CONTEXT", "INSTRUCTION", "History:")
        assertThat(vm.uiState.value.messages).isEmpty()
        assertThat(assistant.downloads).isEqualTo(0)

        vm.downloadModel()
        vm.downloadModel()
        val ready = vm.uiState.first { it.onDevice.status == OnDeviceAiStatus.Available }
        assertThat(assistant.downloads).isEqualTo(1)
        assertThat(ready.needsOnDeviceSetup).isFalse()

        vm.ask("CONTEXT", "INSTRUCTION", "History:")
        assertThat(vm.uiState.first { it.messages.size == 2 && !it.isAnswering }.messages.last().isFailed).isFalse()

        assistant.failAnswer = true
        vm.onQuestionChanged("Again?")
        vm.ask("CONTEXT", "INSTRUCTION", "History:")
        assertThat(vm.uiState.first { it.messages.size == 4 && !it.isAnswering }.messages.last().isFailed).isTrue()
    }

    @Test
    fun failedDownload_canBeRetried() = runTest {
        val assistant = FakeOnDeviceAssistant(OnDeviceAiStatus.Downloadable).apply { downloadSucceeds = false }
        val vm = viewModel(assistant)
        backgroundScope.launch { vm.uiState.collect {} }
        vm.uiState.first { it.onDevice.status != null }

        vm.downloadModel()
        val failed = vm.uiState.first { it.onDevice.hasDownloadFailed }
        assertThat(failed.onDevice.isDownloading).isFalse()
        assertThat(failed.needsOnDeviceSetup).isTrue()

        assistant.downloadSucceeds = true
        vm.downloadModel()
        assertThat(vm.uiState.first { it.onDevice.status == OnDeviceAiStatus.Available }.onDevice.hasDownloadFailed)
            .isFalse()
        assertThat(assistant.downloads).isEqualTo(2)
    }

    @Test
    fun systemAlreadyDownloading_showsProgressRightAway() = runTest {
        val assistant = FakeOnDeviceAssistant(OnDeviceAiStatus.Downloading)
        val vm = viewModel(assistant)
        backgroundScope.launch { vm.uiState.collect {} }

        assertThat(vm.uiState.first { it.onDevice.status == OnDeviceAiStatus.Available }).isNotNull()
        assertThat(assistant.downloads).isEqualTo(1)
    }

    @Test
    fun contextSheet_listsExcludedData_andPromptCarriesQuestionWithoutPlate() {
        var sent = false
        var prompt = ""
        composeRule.setContent {
            ChageunTheme {
                val state = AiHubUiState(question = "How is my car?", options = AiContextOptions(), facts = facts)
                prompt = aiPromptText(facts, state.question)
                AiHubScreen(uiState = state, promptText = prompt, actions = AiHubActions(onSend = { sent = true }))
            }
        }

        composeRule.onNodeWithText("Shared info").performClick()
        composeRule.onNodeWithText("Plate number").assertIsDisplayed()
        composeRule.onNodeWithText("Owner name").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Send").performClick()

        assertThat(sent).isTrue()
        assertThat(prompt).contains("Vehicle: KG Mobility Torres · 2023 · Gasoline")
        assertThat(prompt).contains("Mileage: 42,180 km")
        assertThat(prompt).endsWith("Question: How is my car?")
    }

    @Test
    fun suggestionCard_asksRightAway() {
        var question = ""
        composeRule.setContent {
            ChageunTheme {
                AiHubScreen(
                    uiState = AiHubUiState(facts = facts),
                    promptText = aiPromptText(facts, ""),
                    actions = AiHubActions(onAskSuggestion = { question = it }),
                )
            }
        }

        composeRule.onNodeWithText("How is my car doing?").performClick()
        assertThat(question).isEqualTo("How is my car doing?")
    }

    @Test
    fun controls_haveLabelsAndLargeTouchTargets() {
        composeRule.setContent {
            ChageunTheme {
                val state = AiHubUiState(question = "How is my car?", options = AiContextOptions(), facts = facts)
                AiHubScreen(uiState = state, promptText = aiPromptText(facts, state.question), actions = AiHubActions())
            }
        }

        composeRule.assertAccessibleControls()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun largeFont_keepsWelcomeAndComposerVisible() {
        composeRule.setContent {
            ChageunTheme {
                val state = AiHubUiState(question = "How is my car?", options = AiContextOptions(), facts = facts)
                AiHubScreen(uiState = state, promptText = aiPromptText(facts, state.question), actions = AiHubActions())
            }
        }

        composeRule.assertNoClippedText()
    }

    @Test
    fun focusRecord_getsRecordSuggestions_andPromptCarriesRecord() {
        val fuel = SharedRecord(
            type = TimelineEventType.Fuel,
            date = LocalDate.of(2026, 9, 28),
            maintenanceItem = null,
            title = null,
            mileage = Kilometers(42_000),
            costWon = null,
            fuelVolumeMl = 41_176,
            fuelUnitPriceWon = null,
            isFullTank = true,
        )
        val withRecord = facts.copy(focusRecord = fuel)
        var prompt = ""
        composeRule.setContent {
            ChageunTheme {
                prompt = aiPromptText(withRecord, "Summarize this")
                AiHubScreen(uiState = AiHubUiState(facts = withRecord), promptText = prompt, actions = AiHubActions())
            }
        }

        composeRule.onNodeWithText("Summarize this record in simple terms").assertExists()
        composeRule.onNodeWithText("What can this fuel record tell me about my fuel economy?").assertExists()
        assertThat(prompt).contains("The question is about this record:")
        assertThat(prompt).contains("41.18 L")
        assertThat(prompt).contains("full tank")
    }

    private val conversation = listOf(
        ChatMessage(ChatRole.User, "엔진오일 언제 갈아야 해?"),
        ChatMessage(
            ChatRole.Assistant,
            "마지막 교체 뒤 4,200km를 달렸어요. 권장 주기(7,000km)까지 약 2,800km 남았으니 다음 달쯤 교체하면 좋아요.",
        ),
        ChatMessage(ChatRole.User, "타이어는?"),
        ChatMessage(ChatRole.Assistant, "", isStreaming = true),
    )

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_phone() {
        composeRule.setContent {
            AppFrame {
                val state =
                    AiHubUiState(
                        facts = facts,
                        onDevice = OnDeviceState(OnDeviceAiStatus.Available),
                        destination = AiDestination.OnDevice,
                    )
                AiHubScreen(uiState = state, promptText = aiPromptText(facts, ""), actions = AiHubActions())
            }
        }
        composeRule.captureScreen("ai_phone")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_setup_ko() {
        composeRule.setContent {
            AppFrame {
                val state = AiHubUiState(
                    facts = facts,
                    destination = AiDestination.OnDevice,
                    onDevice = OnDeviceState(OnDeviceAiStatus.Downloadable),
                )
                AiHubScreen(uiState = state, promptText = aiPromptText(facts, ""), actions = AiHubActions())
            }
        }
        composeRule.onNodeWithText("AI 모델 내려받기").assertExists()
        composeRule.captureScreen("ai_setup_phone_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_setupDownloading_ko() {
        composeRule.setContent {
            AppFrame {
                val state = AiHubUiState(
                    facts = facts,
                    destination = AiDestination.OnDevice,
                    onDevice = OnDeviceState(
                        OnDeviceAiStatus.Downloadable,
                        isDownloading = true,
                        downloadProgress = 0.42f,
                    ),
                )
                AiHubScreen(uiState = state, promptText = aiPromptText(facts, ""), actions = AiHubActions())
            }
        }
        composeRule.onNodeWithText("AI 모델 내려받는 중… 42%").assertIsDisplayed()
        composeRule.captureScreen("ai_setup_downloading_phone_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_conversation_ko() = captureConversation("ai_conversation_phone_ko")

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_conversation_dark() = captureConversation("ai_conversation_phone_dark")

    private fun captureConversation(name: String) {
        // 답을 기다리는 점이 계속 튀므로 시계를 멈추고 정해진 만큼만 흘려 늘 같은 순간에 찍는다.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppFrame {
                val state = AiHubUiState(
                    facts = facts,
                    destination = AiDestination.OnDevice,
                    onDevice = OnDeviceState(OnDeviceAiStatus.Available),
                    messages = conversation,
                )
                AiHubScreen(uiState = state, promptText = aiPromptText(facts, ""), actions = AiHubActions())
            }
        }
        // 말풍선이 떠오르고 맨 아래로 따라가는 스크롤이 끝난 뒤에 찍는다.
        composeRule.mainClock.advanceTimeBy(SETTLE_MS)
        composeRule.captureScreen(name)
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_externalWithQuestion_ko() {
        composeRule.setContent {
            AppFrame {
                val state =
                    AiHubUiState(question = "장거리 운전 전에 확인할 게 있어?", facts = facts, destination = AiDestination.ChatGpt)
                AiHubScreen(uiState = state, promptText = aiPromptText(facts, state.question), actions = AiHubActions())
            }
        }
        composeRule.captureScreen("ai_external_phone_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_contextSheet_ko() {
        composeRule.setContent {
            AppFrame {
                val state = AiHubUiState(question = "차 상태 어때?", options = AiContextOptions(), facts = facts)
                AiHubScreen(uiState = state, promptText = aiPromptText(facts, state.question), actions = AiHubActions())
            }
        }
        composeRule.onNodeWithText("보낼 정보").performClick()
        composeRule.captureScreen("ai_context_preview_phone_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.TABLET)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_tablet() {
        composeRule.setContent {
            AppFrame {
                val state =
                    AiHubUiState(
                        facts = facts,
                        destination = AiDestination.OnDevice,
                        onDevice = OnDeviceState(OnDeviceAiStatus.Available),
                    )
                AiHubScreen(uiState = state, promptText = aiPromptText(facts, ""), actions = AiHubActions())
            }
        }
        composeRule.captureScreen("ai_tablet")
    }
}

private const val SETTLE_MS = 1_500L
