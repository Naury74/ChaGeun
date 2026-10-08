package com.naury.chageun.feature.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MileageSource
import com.naury.chageun.core.model.PeriodicInspectionResult
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.RecordTimestamps
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

    private fun show(
        state: HistoryUiState,
        onAdd: () -> Unit = {},
        onDelete: (RecordRef) -> Unit = {},
        onLoadMore: () -> Unit = {},
    ) = composeRule.setContent {
        ChageunTheme {
            HistoryScreen(
                uiState = state,
                isTwoPane = false,
                onKeywordChanged = {},
                onFilterSelected = {},
                onSelect = {},
                onDelete = onDelete,
                onAdd = onAdd,
                onAddPhotos = {},
                onDeleteAttachment = {},
                onDismissAttachFailure = {},
                onLoadMore = onLoadMore,
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
                    onAddPhotos = {},
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

    @Test
    @Config(qualifiers = "w1400dp-h900dp")
    fun threePane_movesFiltersAside_andKeepsTimelinePositionWhileDetailOpens() {
        val items = (1..30).map { day ->
            item.copy(ref = RecordRef(TimelineEventType.Fuel, "fuel-$day"), title = "Station $day")
        }
        var selected by mutableStateOf<RecordRef?>(null)
        var openedAdvanced = false
        composeRule.setContent {
            ChageunTheme {
                HistoryScreen(
                    uiState = HistoryUiState(
                        isLoading = false,
                        sections = listOf(TimelineSection(YearMonth.of(2026, 8), items)),
                        selected = selected,
                        detail = detail.takeIf { selected != null },
                    ),
                    isTwoPane = true,
                    isThreePane = true,
                    onKeywordChanged = {},
                    onFilterSelected = {},
                    onSelect = { selected = it },
                    onDelete = {},
                    onAdd = {},
                    onAddPhotos = {},
                    onDeleteAttachment = {},
                    onDismissAttachFailure = {},
                    onOpenAdvancedFilter = { openedAdvanced = true },
                )
            }
        }

        // 검색과 필터는 왼쪽 Pane에 한 번만 있고, 상세 칸은 선택 전부터 안내 문구를 보여 준다.
        composeRule.onAllNodes(hasText("Search records")).assertCountEquals(1)
        composeRule.onNodeWithText("Record type").assertIsDisplayed()
        composeRule.onNodeWithText("No extra conditions").performClick()
        assertThat(openedAdvanced).isTrue()
        composeRule.onNodeWithText("Select a record to see its details").assertIsDisplayed()

        composeRule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasText("Fuel · Station 25"))
        composeRule.onNodeWithText("Fuel · Station 25").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Unit price").assertIsDisplayed()
        composeRule.onNodeWithText("Select a record to see its details").assertDoesNotExist()
        composeRule.onNodeWithText("Fuel · Station 25").assertIsDisplayed()
        composeRule.onNodeWithText("Fuel · Station 1").assertDoesNotExist()
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
                    onAddPhotos = {},
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
    fun reachingTheEnd_asksForTheNextPage_onlyWhenMoreRemain() {
        var requests = 0
        val section = TimelineSection(YearMonth.of(2026, 8), listOf(item), totalWon = 70_000)
        var state by mutableStateOf(HistoryUiState(isLoading = false, sections = listOf(section), hasMore = true))
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
                    onAddPhotos = {},
                    onDeleteAttachment = {},
                    onDismissAttachFailure = {},
                    onLoadMore = { requests++ },
                )
            }
        }
        composeRule.waitForIdle()
        val whileMoreRemain = requests
        val july = item.copy(ref = RecordRef(TimelineEventType.Fuel, "fuel-july"), date = LocalDate.of(2026, 7, 3))
        state =
            state.copy(
                hasMore = false,
                sections = listOf(section, TimelineSection(YearMonth.of(2026, 7), listOf(july))),
            )
        composeRule.waitForIdle()

        // 기록이 적어 처음부터 끝이 보이므로 바로 요청하고, 더 없으면 목록이 바뀌어도 다시 요청하지 않는다.
        // 같은 화면에서 여러 번 요청해도 ViewModel은 불러온 수가 한도에 닿았을 때만 한 페이지를 늘린다.
        assertThat(whileMoreRemain).isAtLeast(1)
        assertThat(requests).isEqualTo(whileMoreRemain)
    }

    @Test
    fun detail_offersAskingAiAboutThatRecord() {
        var asked: RecordRef? = null
        composeRule.setContent {
            ChageunTheme {
                HistoryScreen(
                    uiState = HistoryUiState(isLoading = false, selected = ref, detail = detail),
                    isTwoPane = false,
                    onKeywordChanged = {},
                    onFilterSelected = {},
                    onSelect = {},
                    onDelete = {},
                    onAdd = {},
                    onAddPhotos = {},
                    onDeleteAttachment = {},
                    onDismissAttachFailure = {},
                    onAskAi = { asked = it },
                )
            }
        }

        composeRule.onNodeWithText("Ask AI about this record").performScrollTo().performClick()

        assertThat(asked).isEqualTo(ref)
    }

    @Test
    fun mileageDetail_showsSourceAndOnlyDelete() {
        val mileageRef = RecordRef(TimelineEventType.Mileage, "m1")
        val mileage = RecordDetail.Mileage(
            mileageRef,
            LocalDate.of(2026, 9, 1),
            Kilometers(1_200),
            MileageSource.Correction,
        )
        var deleted: RecordRef? = null
        show(HistoryUiState(isLoading = false, selected = mileageRef, detail = mileage), onDelete = { deleted = it })

        composeRule.onNodeWithText("Odometer correction").assertExists()
        composeRule.onNodeWithText("Edit").assertDoesNotExist()
        composeRule.onNodeWithText("Ask AI about this record").assertDoesNotExist()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onAllNodes(hasText("Delete") and hasAnyAncestor(isDialog())).onFirst().performClick()

        assertThat(deleted).isEqualTo(mileageRef)
    }

    @Test
    fun monthHeader_showsTheWholeMonthsTotal() {
        val section = TimelineSection(YearMonth.of(2026, 8), listOf(item), totalWon = 250_000)
        show(HistoryUiState(isLoading = false, sections = listOf(section)))

        composeRule.onNodeWithText("₩250,000 spent").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun closingDetailOnPhone_returnsToWhereTheListWas() {
        val items = (1..30).map { day ->
            item.copy(ref = RecordRef(TimelineEventType.Fuel, "fuel-$day"), title = "Station $day")
        }
        val section = TimelineSection(YearMonth.of(2026, 8), items)
        var state by mutableStateOf(HistoryUiState(isLoading = false, sections = listOf(section)))
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
                    onAddPhotos = {},
                    onDeleteAttachment = {},
                    onDismissAttachFailure = {},
                )
            }
        }
        composeRule.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasText("Fuel · Station 25"))

        state = state.copy(selected = ref, detail = detail)
        composeRule.waitForIdle()
        state = state.copy(selected = null, detail = null)
        composeRule.waitForIdle()

        // 휴대폰에서는 상세를 여는 동안 목록이 사라지지만, 돌아오면 보던 자리에서 이어진다.
        composeRule.onNodeWithText("Fuel · Station 25").assertIsDisplayed()
        composeRule.onNodeWithText("Fuel · Station 1").assertDoesNotExist()
    }

    @Test
    fun showsTimelineRow_withCostAndMileage() {
        show(HistoryUiState(isLoading = false, sections = listOf(TimelineSection(YearMonth.of(2026, 8), listOf(item)))))

        composeRule.onNodeWithText("Fuel · S-Oil").assertIsDisplayed()
        composeRule.onNodeWithText("₩70,000").assertIsDisplayed()
    }

    @Test
    fun detail_showsWhenItWasRecordedAndEdited() {
        val times = RecordTimestamps(Instant.parse("2026-08-03T05:00:00Z"), Instant.parse("2026-08-05T05:00:00Z"))
        show(HistoryUiState(isLoading = false, selected = ref, detail = detail.copy(timestamps = times)))

        composeRule.onNodeWithText("Recorded", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("edited", substring = true).assertIsDisplayed()
    }

    @Test
    fun detail_showsPeriodicInspectionResult_butNotForGeneralCheck() {
        val checkRef = RecordRef(TimelineEventType.Inspection, "c1")
        val periodic = CheckEntry(
            kind = CheckKind.Inspection,
            date = LocalDate.of(2026, 9, 1),
            title = "Periodic inspection",
            periodicResult = PeriodicInspectionResult.Failed,
        )
        var state by mutableStateOf(
            HistoryUiState(isLoading = false, selected = checkRef, detail = RecordDetail.Check(checkRef, periodic)),
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
                    onAddPhotos = {},
                    onDeleteAttachment = {},
                    onDismissAttachFailure = {},
                )
            }
        }

        composeRule.onNodeWithText("Inspection result").assertIsDisplayed()
        composeRule.onNodeWithText("Failed", substring = true).assertIsDisplayed()

        val general = periodic.copy(title = "Free check", periodicResult = null)
        state = state.copy(detail = RecordDetail.Check(checkRef, general))
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Inspection result").assertDoesNotExist()
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
                    onAddPhotos = {},
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
            totalWon = 163_000,
        ),
        TimelineSection(YearMonth.of(2026, 8), listOf(item), totalWon = 70_000),
    )

    private fun screenshot(name: String, state: HistoryUiState, isTwoPane: Boolean, isThreePane: Boolean = false) {
        composeRule.setContent {
            AppFrame {
                HistoryScreen(
                    uiState = state,
                    isTwoPane = isTwoPane,
                    isThreePane = isThreePane,
                    onKeywordChanged = {},
                    onFilterSelected = {},
                    onSelect = {},
                    onDelete = {},
                    onAdd = {},
                    onAddPhotos = {},
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
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_phoneTimelineDark() = screenshot(
        "history_phone_dark",
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

    @Test
    @Config(qualifiers = ScreenshotDevices.LARGE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_largeThreePaneKorean() = screenshot(
        "history_large_ko",
        HistoryUiState(
            isLoading = false,
            filter = HistoryFilter.All,
            advanced = AdvancedFilter(period = HistoryPeriod.Last6Months, withAttachmentsOnly = true),
            sections = screenshotSections,
            selected = ref,
            detail = detail,
        ),
        isTwoPane = true,
        isThreePane = true,
    )

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_detailWithAskAiKorean() {
        composeRule.setContent {
            AppFrame {
                HistoryScreen(
                    uiState = HistoryUiState(isLoading = false, selected = ref, detail = detail),
                    isTwoPane = false,
                    onKeywordChanged = {},
                    onFilterSelected = {},
                    onSelect = {},
                    onDelete = {},
                    onAdd = {},
                    onAddPhotos = {},
                    onDeleteAttachment = {},
                    onDismissAttachFailure = {},
                    onAskAi = {},
                )
            }
        }
        composeRule.captureScreen("history_detail_ask_ai_ko")
    }
}
