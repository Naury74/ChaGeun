package com.naury.chageun.feature.manage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.maintenance.DefaultMaintenanceRules
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MissingInput
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.ServiceHistoryEntry
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2000dp")
class ManageScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val oil = MaintenanceStatus(
        item = MaintenanceItem.EngineOil,
        state = MaintenanceState.Due,
        remainingKm = 420,
        remainingDays = 120,
        distanceDue = Kilometers(50_260),
        dateDue = LocalDate.of(2027, 3, 10),
        estimatedDue = null,
        missingInputs = emptySet(),
        ruleSource = RuleSource.Generic,
    )
    private val rule = MaintenanceRule(MaintenanceItem.EngineOil, intervalKm = 10_000, intervalMonths = 12)
    private val history = listOf(ServiceHistoryEntry("r1", LocalDate.of(2026, 3, 10), Kilometers(40_260), 95_000, null))

    private fun stateFor(selected: MaintenanceItem?) = ManageUiState(
        isLoading = false,
        items = listOf(oil),
        counts = mapOf(ManageFilter.All to 1, ManageFilter.NeedsAttention to 1, ManageFilter.Upcoming to 0),
        rules = mapOf(MaintenanceItem.EngineOil to rule),
        selectedItem = selected,
        detail = selected?.let { ManageDetail(oil, rule, history, null) },
    )

    @Test
    fun compact_opensDetailAndReturnsToList() {
        var selected by mutableStateOf<MaintenanceItem?>(null)
        composeRule.setContent {
            ChageunTheme {
                ManageScreen(
                    uiState = stateFor(selected),
                    isTwoPane = false,
                    onFilterSelected = {},
                    onItemSelected = { selected = it },
                    onRecordService = {},
                    onEditRule = {},
                )
            }
        }

        composeRule.onNodeWithText("Engine oil").performClick()
        composeRule.onNodeWithText("10,000 km or 12 months, whichever comes first").assertIsDisplayed()
        composeRule.onNodeWithText("General guideline, not a manufacturer value").assertIsDisplayed()
        composeRule.onNodeWithText("₩95,000").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("All 1").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun twoPane_listUsesFullWidth_untilSelection_thenDetailSlidesIn() {
        var selected by mutableStateOf<MaintenanceItem?>(null)
        composeRule.setContent {
            ChageunTheme {
                ManageScreen(stateFor(selected), isTwoPane = true, onFilterSelected = {
                }, onItemSelected = { selected = it }, onRecordService = {}, onEditRule = {})
            }
        }

        // 선택 전에는 빈 상세 칸 없이 목록만 보인다.
        composeRule.onNodeWithText("All 1").assertIsDisplayed()
        composeRule.onNodeWithText("Select an item to see why it has this status").assertDoesNotExist()

        composeRule.onNodeWithText("Engine oil").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Edit interval").assertIsDisplayed()

        // 상세의 뒤로 가기는 상세를 닫고 목록을 다시 넓힌다.
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Edit interval").assertDoesNotExist()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun largeFont_keepsListAndDetailTextVisible() {
        var selected by mutableStateOf<MaintenanceItem?>(null)
        composeRule.setContent {
            ChageunTheme {
                ManageScreen(
                    uiState = stateFor(selected),
                    isTwoPane = false,
                    onFilterSelected = {},
                    onItemSelected = { selected = it },
                    onRecordService = {},
                    onEditRule = {},
                )
            }
        }
        composeRule.assertNoClippedText()

        selected = MaintenanceItem.EngineOil
        composeRule.waitForIdle()

        composeRule.assertNoClippedText()
    }

    /** Screenshot은 실제 사용 화면처럼 여러 상태의 항목을 함께 보여 준다. */
    private fun screenshotState(selected: MaintenanceItem?): ManageUiState {
        val tire = oil.copy(item = MaintenanceItem.Tire, state = MaintenanceState.Overdue, remainingKm = -1_200)
        val airFilter = oil.copy(item = MaintenanceItem.AirFilter, state = MaintenanceState.Good, remainingKm = 12_400)
        val battery = oil.copy(
            item = MaintenanceItem.Battery,
            state = MaintenanceState.Unknown,
            remainingKm = null,
            remainingDays = null,
            missingInputs = setOf(MissingInput.LastService),
        )
        return stateFor(selected).copy(
            items = listOf(tire, oil, airFilter, battery),
            counts = mapOf(ManageFilter.All to 4, ManageFilter.NeedsAttention to 2, ManageFilter.Upcoming to 0),
            rules = listOf(tire, airFilter).associate {
                it.item to checkNotNull(DefaultMaintenanceRules.genericFor(it.item, FuelType.Gasoline))
            } + (MaintenanceItem.EngineOil to rule),
            disabledItems = listOf(MaintenanceItem.Wiper),
        )
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_phoneList() {
        composeRule.setContent {
            AppFrame {
                ManageScreen(screenshotState(null), isTwoPane = false, onFilterSelected = {
                }, onItemSelected = {}, onRecordService = {}, onEditRule = {})
            }
        }
        composeRule.captureScreen("care_phone")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_phoneListDark() {
        composeRule.setContent {
            AppFrame {
                ManageScreen(screenshotState(null), isTwoPane = false, onFilterSelected = {
                }, onItemSelected = {}, onRecordService = {}, onEditRule = {})
            }
        }
        composeRule.captureScreen("care_phone_dark")
    }

    /** 펼친 폴더블의 목록 칸처럼 좁은 너비에서는 배지가 이름 아래로 내려간다. */
    @Test
    @Config(qualifiers = "w300dp-h800dp-hdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_narrowList() {
        composeRule.setContent {
            AppFrame {
                ManageScreen(screenshotState(null), isTwoPane = false, onFilterSelected = {
                }, onItemSelected = {}, onRecordService = {}, onEditRule = {})
            }
        }
        composeRule.captureScreen("care_narrow")
    }

    /** 넓은 창에서 아무것도 고르지 않으면 카드가 여러 열로 놓인다. */
    @Test
    @Config(qualifiers = ScreenshotDevices.TABLET)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_tabletGrid() {
        composeRule.setContent {
            AppFrame {
                ManageScreen(screenshotState(null), isTwoPane = true, onFilterSelected = {
                }, onItemSelected = {}, onRecordService = {}, onEditRule = {})
            }
        }
        composeRule.captureScreen("care_tablet_grid")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.TABLET)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_tabletDetail() {
        composeRule.setContent {
            AppFrame {
                ManageScreen(
                    screenshotState(MaintenanceItem.EngineOil),
                    isTwoPane = true,
                    onFilterSelected = {},
                    onItemSelected = {},
                    onRecordService = {},
                    onEditRule = {},
                )
            }
        }
        composeRule.captureScreen("care_tablet")
    }
}
