package com.naury.chageun.feature.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.ServiceHistoryEntry
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2000dp")
class HistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val ref = RecordRef(TimelineEventType.Fuel, "fuel")
    private val item =
        TimelineItem(
            ref,
            LocalDate.of(2026, 8, 3),
            "S-Oil",
            null,
            Kilometers(37_120),
            70_000,
            RecordSource.User,
            Instant.EPOCH,
        )
    private val detail = RecordDetail.Fuel(
        ref,
        FuelEntry(
            LocalDate.of(2026, 8, 3),
            Kilometers(37_120),
            FuelAmounts(70_000, 41_176, 1_700, FuelField.Volume),
            true,
            "S-Oil",
        ),
    )

    private fun show(state: HistoryUiState, onAdd: () -> Unit = {}, onDelete: (RecordRef) -> Unit = {}) =
        composeRule.setContent {
            ChageunTheme {
                HistoryScreen(
                    uiState = state,
                    isTwoPane = false,
                    onKeywordChanged = {},
                    onFilterSelected = {},
                    onSelect = {},
                    onDelete = onDelete,
                    onAdd = onAdd,
                    onAttach = { _, _ -> },
                    onDeleteAttachment = {},
                    onDismissAttachFailure = {},
                )
            }
        }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun twoPane_showsListOnly_untilSelection_thenDetail() {
        var selected by mutableStateOf<RecordRef?>(null)
        composeRule.setContent {
            ChageunTheme {
                HistoryScreen(
                    uiState = HistoryUiState(
                        isLoading = false,
                        sections = listOf(TimelineSection(YearMonth.of(2026, 8), listOf(item))),
                        selected = selected,
                        detail = detail.takeIf { selected != null },
                    ),
                    isTwoPane = true,
                    onKeywordChanged = {},
                    onFilterSelected = {},
                    onSelect = { selected = it },
                    onDelete = {},
                    onAdd = {},
                    onAttach = { _, _ -> },
                    onDeleteAttachment = {},
                    onDismissAttachFailure = {},
                )
            }
        }

        composeRule.onNodeWithText("Select a record to see its details").assertDoesNotExist()
        composeRule.onNodeWithText("Fuel · S-Oil").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Unit price").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertThat(selected).isNull()
    }

    /** 폴더블을 접고 펴 한 칸·두 칸이 바뀌어도 상세 안의 삭제 확인 창이 그대로 남는다. */
    @Test
    @Config(qualifiers = "w1280dp-h2000dp")
    fun deleteConfirmation_survivesPaneLayoutChange() {
        var twoPane by mutableStateOf(false)
        composeRule.setContent {
            ChageunTheme {
                HistoryScreen(
                    uiState = HistoryUiState(
                        isLoading = false,
                        sections = listOf(TimelineSection(YearMonth.of(2026, 8), listOf(item))),
                        selected = ref,
                        detail = detail,
                    ),
                    isTwoPane = twoPane,
                    onKeywordChanged = {},
                    onFilterSelected = {},
                    onSelect = {},
                    onDelete = {},
                    onAdd = {},
                    onAttach = { _, _ -> },
                    onDeleteAttachment = {},
                    onDismissAttachFailure = {},
                )
            }
        }
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Delete this record?").assertIsDisplayed()

        twoPane = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Delete this record?").assertIsDisplayed()
    }

    @Test
    fun emptyState_offersFirstRecord() {
        var added = false
        show(HistoryUiState(isLoading = false), onAdd = { added = true })

        composeRule.onNodeWithText("No records yet").assertIsDisplayed()
        composeRule.onNodeWithText("Add your first record").performClick()

        assertThat(added).isTrue()
    }

    @Test
    fun showsTimelineRow_withCostAndMileage() {
        show(HistoryUiState(isLoading = false, sections = listOf(TimelineSection(YearMonth.of(2026, 8), listOf(item)))))

        composeRule.onNodeWithText("Fuel · S-Oil").assertIsDisplayed()
        composeRule.onNodeWithText("₩70,000").assertIsDisplayed()
    }

    @Test
    fun detail_marksComputedValue_andConfirmsDelete() {
        var deleted: RecordRef? = null
        show(HistoryUiState(isLoading = false, selected = ref, detail = detail), onDelete = { deleted = it })

        composeRule.onNodeWithText("41.18 L (calculated)").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Delete this record?").assertIsDisplayed()
        composeRule.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()

        assertThat(deleted).isEqualTo(ref)
    }

    @Test
    fun detail_marksEstimatedMileageAndSource() {
        val maintenanceRef = RecordRef(TimelineEventType.Maintenance, "oil")
        val estimated = RecordDetail.Maintenance(
            maintenanceRef,
            MaintenanceItem.EngineOil,
            ServiceHistoryEntry(
                "oil",
                LocalDate.of(2026, 4, 6),
                Kilometers(39_007),
                null,
                null,
                isMileageEstimated = true,
            ),
            memo = null,
        )
        show(HistoryUiState(isLoading = false, selected = maintenanceRef, detail = estimated))

        composeRule.onNodeWithText("About 39,007 km (estimated from your average driving)").assertIsDisplayed()
        composeRule.onNodeWithText("Rough entry (mileage estimated)").assertIsDisplayed()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun largeFont_keepsTimelineAndDetailTextVisible() {
        var state by mutableStateOf(
            HistoryUiState(isLoading = false, sections = listOf(TimelineSection(YearMonth.of(2026, 8), listOf(item)))),
        )
        composeRule.setContent {
            ChageunTheme {
                HistoryScreen(
                    uiState = state,
                    isTwoPane = false,
                    onKeywordChanged = {},
                    onFilterSelected = {},
                    onSelect = {},
                    onDelete = {},
                    onAdd = {},
                    onAttach = { _, _ -> },
                    onDeleteAttachment = {},
                    onDismissAttachFailure = {},
                )
            }
        }
        composeRule.assertNoClippedText()

        state = HistoryUiState(isLoading = false, selected = ref, detail = detail)
        composeRule.waitForIdle()

        composeRule.assertNoClippedText()
    }

    /** Screenshot은 정비·주유·검사가 섞인 두 달치 기록으로 실제 사용 화면에 가깝게 그린다. */
    private val screenshotSections = listOf(
        TimelineSection(
            YearMonth.of(2026, 9),
            listOf(
                TimelineItem(
                    RecordRef(TimelineEventType.Maintenance, "oil"),
                    LocalDate.of(2026, 9, 21),
                    null,
                    MaintenanceItem.EngineOil,
                    Kilometers(41_800),
                    95_000,
                    RecordSource.User,
                    Instant.EPOCH,
                ),
                TimelineItem(
                    RecordRef(TimelineEventType.Fuel, "fuel-2"),
                    LocalDate.of(2026, 9, 12),
                    "GS Caltex",
                    null,
                    Kilometers(41_350),
                    68_000,
                    RecordSource.User,
                    Instant.EPOCH,
                ),
                TimelineItem(
                    RecordRef(TimelineEventType.Inspection, "inspection"),
                    LocalDate.of(2026, 9, 2),
                    null,
                    null,
                    Kilometers(40_900),
                    null,
                    RecordSource.User,
                    Instant.EPOCH,
                ),
            ),
        ),
        TimelineSection(YearMonth.of(2026, 8), listOf(item)),
    )

    private fun screenshot(name: String, state: HistoryUiState, isTwoPane: Boolean) {
        composeRule.setContent {
            AppFrame {
                HistoryScreen(
                    uiState = state,
                    isTwoPane = isTwoPane,
                    onKeywordChanged = {},
                    onFilterSelected = {},
                    onSelect = {},
                    onDelete = {},
                    onAdd = {},
                    onAttach = { _, _ -> },
                    onDeleteAttachment = {},
                    onDismissAttachFailure = {},
                )
            }
        }
        composeRule.captureScreen(name)
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_phoneTimeline() = screenshot(
        "history_phone",
        HistoryUiState(isLoading = false, sections = screenshotSections),
        isTwoPane = false,
    )

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_phoneEmpty() =
        screenshot("history_phone_empty", HistoryUiState(isLoading = false), isTwoPane = false)

    @Test
    @Config(qualifiers = ScreenshotDevices.TABLET)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_tabletList() = screenshot(
        "history_tablet_list",
        HistoryUiState(isLoading = false, sections = screenshotSections),
        isTwoPane = true,
    )

    @Test
    @Config(qualifiers = ScreenshotDevices.TABLET)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_tabletDetail() = screenshot(
        "history_tablet",
        HistoryUiState(
            isLoading = false,
            sections = screenshotSections,
            selected = ref,
            detail = detail,
        ),
        isTwoPane = true,
    )
}
