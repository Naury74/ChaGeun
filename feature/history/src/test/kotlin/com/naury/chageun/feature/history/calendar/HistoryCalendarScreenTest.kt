package com.naury.chageun.feature.history.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.HistoryView
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.uitesting.assertAccessibleControls
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.feature.history.HistoryCalendarLinks
import com.naury.chageun.feature.history.HistoryScreen
import com.naury.chageun.feature.history.HistoryUiState
import com.naury.chageun.feature.history.HistoryViewToggle
import com.naury.chageun.feature.history.calendar.CalendarFixtures.TODAY
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
class HistoryCalendarScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val opened = mutableListOf<Any>()
    private val shownMonths = mutableListOf<YearMonth>()
    private val movedWeeks = mutableListOf<Long>()
    private val addedOn = mutableListOf<LocalDate>()
    private val searched = mutableListOf<String>()
    private val resultMoves = mutableListOf<String>()
    private var state by mutableStateOf(CalendarFixtures.state())

    /** 날짜 선택·보기·필터·검색은 실제 ViewModel처럼 상태에 반영한다. */
    private fun actions() = CalendarActions(
        onSelectDate = { state = state.copy(selectedDate = it) },
        onShowMonth = { shownMonths += it },
        onMoveWeek = { movedWeeks += it },
        onSetMode = { state = state.copy(mode = it) },
        onToggleKind = { kind ->
            val kinds = state.filter.kinds
            state = state.copy(filter = state.filter.copy(kinds = if (kind in kinds) kinds - kind else kinds + kind))
        },
        onSetItems = { state = state.copy(filter = state.filter.copy(items = it)) },
        onOpenSearch = { state = state.copy(search = state.search.copy(isOpen = true)) },
        onCloseSearch = { state = state.copy(search = CalendarSearch()) },
        onSearch = { searched += it },
        onPreviousResult = { resultMoves += "previous" },
        onNextResult = { resultMoves += "next" },
        onOpenResult = { record -> state = state.copy(selectedDate = record.date) },
        onClearAll = { state = state.copy(filter = CalendarFilter(), search = CalendarSearch()) },
        onOpenUpcoming = { opened += it },
        onOpenRecord = { opened += it },
        onOpenPhoto = { opened += it.id },
        onAddOnDate = { addedOn += it },
    )

    private fun show(
        initial: CalendarUiState = CalendarFixtures.state(),
        view: HistoryView = HistoryView.Calendar,
        isTwoPane: Boolean = false,
    ) {
        state = initial
        val actions = actions()
        composeRule.setContent {
            ChageunTheme {
                HistoryScreen(
                    uiState = HistoryUiState(isLoading = false),
                    isTwoPane = isTwoPane,
                    onKeywordChanged = {},
                    onFilterSelected = {},
                    onSelect = {},
                    onDelete = {},
                    onAdd = {},
                    onAddPhotos = {},
                    onDeleteAttachment = {},
                    onDismissAttachFailure = {},
                    view = view,
                    calendar = HistoryCalendarSlot(state, actions),
                )
            }
        }
    }

    private fun dayCell(description: String) = composeRule.onNode(hasContentDescription(description, substring = true))

    private fun day(description: String) = hasContentDescription(description, substring = true)

    private fun marks(count: Int) = SemanticsMatcher.expectValue(DayMarksShown, count)

    private val withThumbnail = SemanticsMatcher.expectValue(DayHasThumbnail, true)

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    }

    @Test
    fun toggle_switchesBetweenListAndCalendar() {
        var view by mutableStateOf(HistoryView.List)
        composeRule.setContent {
            ChageunTheme { HistoryViewToggle(view = view, onChange = { view = it }) }
        }

        composeRule.onNodeWithText("Calendar").performClick()
        assertThat(view).isEqualTo(HistoryView.Calendar)
        composeRule.onNodeWithText("Calendar").assertIsSelected()

        composeRule.onNodeWithText("List").performClick()
        assertThat(view).isEqualTo(HistoryView.List)
    }

    @Test
    fun listView_keepsTheTimeline_evenWithCalendarState() {
        show(view = HistoryView.List)

        composeRule.onNodeWithText("No records yet").assertIsDisplayed()
        composeRule.onAllNodesWithText("October 2026").assertCountEquals(0)
    }

    @Test
    fun showsMonthHeaderSummary_andTodaysAgenda() {
        show()

        composeRule.onNodeWithText("October 2026").assertIsDisplayed()
        composeRule.onNodeWithText("₩311,000 spent · 10 records · 4 photos").assertIsDisplayed()
        dayCell("October 12").assertIsSelected()
        scrollTo("Car wash")
        composeRule.onNodeWithText("Car wash").assertIsDisplayed()
        scrollTo("Next 30 days")
        composeRule.onNodeWithText("Next 30 days").assertIsDisplayed()
    }

    @Test
    fun dayCell_readsCountsWithTypeNames_andSelection() {
        show()

        dayCell(
            "Monday, October 12, today, 2 records (Fuel, Note), 2 photos, 1 upcoming (Cabin air filter), " +
                "1 overdue (Wipers)",
        ).assertIsSelected()
        dayCell("Thursday, October 8, 5 records (Engine oil replacement, Oil filter replacement").assertIsNotSelected()
        // 다른 달 날짜도 데이터가 함께 실린다.
        dayCell("Monday, September 28, 1 record (Fuel)").assertExists()
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h2000dp")
    fun dayCell_readsInKorean() {
        show()

        dayCell("10월 12일 월요일, 오늘, 기록 2개(주유, 기타), 사진 2장, 예정 1개(에어컨필터), 지남 1개(와이퍼)")
            .assertIsSelected()
        dayCell("10월 8일 목요일, 기록 5개(엔진오일 교체, 오일필터 교체").assertExists()
    }

    @Test
    fun monthCells_showTwoIcons_andPhotoThumbnails() {
        show()

        composeRule.onNode(day("October 8") and marks(2) and withThumbnail).assertExists()
        composeRule.onNode(day("October 12") and marks(2) and withThumbnail).assertExists()
        composeRule.onNode(day("October 24") and marks(1)).assertExists()
    }

    @Test
    fun weekButton_collapsesToTheSelectedWeek_withLargerCells() {
        show(CalendarFixtures.state(selected = LocalDate.of(2026, 10, 8)))

        composeRule.onNode(hasText("Week view") and hasClickAction()).performClick()

        assertThat(state.mode).isEqualTo(CalendarMode.Week)
        // 10월 4일(일)~10일(토) 한 주만 남고, 칸이 커져 아이콘을 4개까지 그린다.
        composeRule.onNode(day("October 8") and marks(4)).assertExists()
        dayCell("October 4").assertExists()
        dayCell("October 10").assertExists()
        composeRule.onAllNodes(day("October 12")).assertCountEquals(0)

        composeRule.onNode(hasText("Month view") and hasClickAction()).performClick()
        assertThat(state.mode).isEqualTo(CalendarMode.Month)
        dayCell("October 12").assertExists()
    }

    @Test
    fun weekArrowsAndSwipe_moveByWeek() {
        show(CalendarFixtures.state(mode = CalendarMode.Week))

        composeRule.onNodeWithContentDescription("Next week").performClick()
        composeRule.onNodeWithContentDescription("Previous week").performClick()
        dayCell("Wednesday, October 14").performTouchInput {
            swipe(center + Offset(SWIPE_PX, 0f), center - Offset(SWIPE_PX, 0f))
        }

        assertThat(movedWeeks).containsExactly(1L, -1L, 1L).inOrder()
        assertThat(shownMonths).isEmpty()
    }

    @Test
    fun scrollingTheAgendaUp_collapses_andPullingDownAtTop_expands() {
        show()

        composeRule.onNode(hasScrollToNodeAction()).performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        assertThat(state.mode).isEqualTo(CalendarMode.Week)

        composeRule.onNode(hasScrollToNodeAction()).performTouchInput { swipeDown() }
        composeRule.onNode(hasScrollToNodeAction()).performTouchInput { swipeDown() }
        composeRule.waitForIdle()
        assertThat(state.mode).isEqualTo(CalendarMode.Month)
    }

    @Test
    fun selectingADay_showsItsAgenda() {
        show()

        dayCell("Thursday, October 8").performClick()

        dayCell("Thursday, October 8").assertIsSelected()
        scrollTo("Tire check")
        composeRule.onNodeWithText("Tire check").assertIsDisplayed()
        composeRule.onAllNodesWithText("Next 30 days").assertCountEquals(0)
    }

    @Test
    fun kindChips_showOnlyThoseKinds_andTheSummaryFollows() {
        show()

        composeRule.onNode(hasText("Fuel") and hasClickAction()).performClick()

        dayCell("Monday, October 12, today, 1 record (Fuel)").assertIsSelected()
        dayCell("Thursday, October 8").assertExists()
        composeRule.onAllNodes(day("5 records")).assertCountEquals(0)
        composeRule.onNodeWithText("₩138,000 spent · 2 records · 0 photos").assertIsDisplayed()
        composeRule.onNodeWithText("1 filter").assertIsDisplayed()

        composeRule.onNode(hasText("Clear all") and hasClickAction()).performClick()
        composeRule.onAllNodesWithText("1 filter").assertCountEquals(0)
        dayCell("Thursday, October 8, 5 records").assertExists()
    }

    @Test
    fun itemSheet_limitsMaintenanceToChosenItems() {
        show()

        composeRule.onNode(hasText("Items") and hasClickAction()).performClick()
        composeRule.onNode(hasText("Engine oil") and isToggleable()).performClick()
        composeRule.onNode(hasText("Apply") and hasClickAction()).performClick()

        assertThat(state.filter.items).containsExactly(MaintenanceItem.EngineOil)
        composeRule.onNodeWithText("1 item").assertIsDisplayed()
        dayCell("Thursday, October 8, 1 record (Engine oil replacement)").assertExists()
        // 정비 항목만 골랐으므로 3일의 주유 기록은 숨는다.
        composeRule.onNode(hasContentDescription("Saturday, October 3")).assertExists()
    }

    @Test
    fun searchButton_opensTheField_andPassesTheKeyword() {
        show()

        composeRule.onNodeWithContentDescription("Search calendar").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("oil")

        assertThat(searched).containsExactly("oil")
        composeRule.onNodeWithContentDescription("Close search").performClick()
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
    }

    @Test
    fun searchResults_highlightDays_listAcrossMonths_andJump() {
        show(CalendarFixtures.state(search = CalendarFixtures.oilSearch))

        composeRule.onNodeWithText("“oil” · 3 results").assertIsDisplayed()
        composeRule.onNodeWithText("3 results").assertIsDisplayed()
        composeRule.onAllNodes(day("search result")).assertCountEquals(1)
        dayCell("Thursday, October 8, 5 records").assertExists()

        scrollTo("July 2026")
        composeRule.onNodeWithText("July 2026").assertIsDisplayed()
        composeRule.onNode(hasText("Oil filter") and hasClickAction()).performClick()
        dayCell("Thursday, October 8").assertIsSelected()

        composeRule.onNodeWithContentDescription("Previous result").performClick()
        composeRule.onNodeWithContentDescription("Next result").assertIsNotEnabled()
        assertThat(resultMoves).containsExactly("previous")
    }

    @Test
    fun agendaItems_openTheirDestinations() {
        show()

        scrollTo("Wipers")
        composeRule.onNodeWithText("Wipers").performClick()
        scrollTo("Car wash")
        composeRule.onNodeWithText("Car wash").performClick()
        composeRule.onNodeWithContentDescription("Photo from Oct 12, 2026").performClick()
        composeRule.onNodeWithContentDescription("Photo attached to Fuel · GS Caltex").performClick()
        scrollTo("Periodic inspection")
        composeRule.onNodeWithText("Periodic inspection").performClick()
        scrollTo("Update mileage")
        composeRule.onNodeWithText("Update mileage").performClick()

        assertThat(opened).containsExactly(
            CalendarFixtures.overdueWiper,
            RecordRef(TimelineEventType.Note, "wash"),
            "today",
            RecordRef(TimelineEventType.Fuel, "fuel-today"),
            CalendarFixtures.inspectionDue,
            CalendarFixtures.mileageUpdate,
        ).inOrder()
    }

    @Test
    fun links_routeEachUpcomingKind() {
        val routed = mutableListOf<String>()
        val links = HistoryCalendarLinks(
            onOpenManageItem = { routed += "manage:$it" },
            onOpenInspection = { routed += "inspection" },
            onUpdateMileage = { routed += "mileage" },
        )

        links.open(UpcomingEvent.MaintenanceDue(MaintenanceItem.Tire, TODAY, false, null))
        links.open(UpcomingEvent.InspectionDue(TODAY))
        links.open(UpcomingEvent.MileageUpdate(TODAY))

        assertThat(routed).containsExactly("manage:Tire", "inspection", "mileage").inOrder()
    }

    @Test
    fun emptyPastDay_offersAddingARecordOnThatDay() {
        show(CalendarFixtures.state(selected = LocalDate.of(2026, 10, 5)))

        scrollTo("Nothing recorded this day")
        composeRule.onNodeWithText("Add a record for this day").performClick()

        assertThat(addedOn).containsExactly(LocalDate.of(2026, 10, 5))
    }

    @Test
    fun emptyFutureDay_hasNoAddButton() {
        show(CalendarFixtures.state(selected = LocalDate.of(2026, 10, 15)))

        scrollTo("Nothing planned this day")
        composeRule.onAllNodesWithText("Add a record for this day").assertCountEquals(0)
    }

    @Test
    fun arrowsAndSwipe_changeTheMonth() {
        show()

        composeRule.onNodeWithContentDescription("Next month").performClick()
        composeRule.onNodeWithContentDescription("Previous month").performClick()
        // 날짜 칸 하나보다 길게 밀어야 달이 넘어간다.
        dayCell("Wednesday, October 14").performTouchInput {
            swipe(center + Offset(SWIPE_PX, 0f), center - Offset(SWIPE_PX, 0f))
        }
        dayCell("Wednesday, October 14").performTouchInput {
            swipe(center - Offset(SWIPE_PX, 0f), center + Offset(SWIPE_PX, 0f))
        }

        assertThat(shownMonths).containsExactly(
            YearMonth.of(2026, 11),
            YearMonth.of(2026, 9),
            YearMonth.of(2026, 11),
            YearMonth.of(2026, 9),
        ).inOrder()
    }

    @Test
    fun controls_haveLabelsAndLargeTouchTargets() {
        show()

        composeRule.assertAccessibleControls()
    }

    // 검색 결과까지 한 화면에 들어오게 높게 둔다. 화면 밖에 미리 그려 둔 행은 터치 영역이 0으로 잡힌다.
    @Test
    @Config(qualifiers = "w360dp-h3200dp")
    fun weekAndSearch_controls_haveLabelsAndLargeTouchTargets() {
        show(CalendarFixtures.state(mode = CalendarMode.Week, search = CalendarFixtures.oilSearch))

        composeRule.assertAccessibleControls()
    }

    @Test
    @Config(qualifiers = "w1280dp-h2000dp")
    fun twoPane_controls_haveLabelsAndLargeTouchTargets() {
        show(isTwoPane = true)

        composeRule.onNodeWithText("Car wash").assertIsDisplayed()
        composeRule.assertAccessibleControls()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun largeFont_keepsDayNumbersAndAgendaVisible() {
        show()

        composeRule.assertNoClippedText()
        scrollTo("Next 30 days")
        composeRule.assertNoClippedText()
    }

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun largeFont_weekAndSearch_keepTextVisible() {
        show(CalendarFixtures.state(mode = CalendarMode.Week, search = CalendarFixtures.oilSearch))

        composeRule.assertNoClippedText()
        scrollTo("July 2026")
        composeRule.assertNoClippedText()
    }

    private companion object {
        const val SWIPE_PX = 150f
    }
}
