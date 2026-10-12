package com.naury.chageun.feature.history.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
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
    private val addedOn = mutableListOf<LocalDate>()

    /** 날짜 선택과 레이어는 실제 ViewModel처럼 상태에 반영한다. */
    private fun show(
        initial: CalendarUiState = CalendarFixtures.state(),
        view: HistoryView = HistoryView.Calendar,
        isTwoPane: Boolean = false,
    ) {
        var state by mutableStateOf(initial)
        val actions = CalendarActions(
            onSelectDate = { state = state.copy(selectedDate = it) },
            onShowMonth = { shownMonths += it },
            onToggleLayer = { layer ->
                state = state.copy(layers = if (layer in state.layers) state.layers - layer else state.layers + layer)
            },
            onOpenUpcoming = { opened += it },
            onOpenRecord = { opened += it },
            onOpenPhoto = { opened += it.id },
            onAddOnDate = { addedOn += it },
        )
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
        composeRule.onNodeWithText("₩311,000 spent · 10 records · 2 photos").assertIsDisplayed()
        dayCell("October 12").assertIsSelected()
        scrollTo("Car wash")
        composeRule.onNodeWithText("Car wash").assertIsDisplayed()
        scrollTo("Next 30 days")
        composeRule.onNodeWithText("Next 30 days").assertIsDisplayed()
    }

    @Test
    fun dayCell_describesItsContent_andSelection() {
        show()

        dayCell("Monday, October 12, today, 2 records, 1 photo, 1 upcoming, 1 overdue").assertIsSelected()
        dayCell("Thursday, October 8, 5 records").assertIsNotSelected()
        // 다른 달 날짜도 데이터가 함께 실린다.
        dayCell("Monday, September 28, 1 record").assertExists()
    }

    @Test
    @Config(qualifiers = "ko-w360dp-h2000dp")
    fun dayCell_readsInKorean() {
        show()

        dayCell("10월 12일 월요일, 오늘, 기록 2개, 사진 1장, 예정 1개, 지남 1개").assertIsSelected()
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
    fun layerChips_hideThatLayerFromDays() {
        show()

        composeRule.onNode(hasText("Records") and hasClickAction()).performClick()
        composeRule.onNode(hasText("Photos") and hasClickAction()).performClick()

        dayCell("Monday, October 12, today, 1 upcoming, 1 overdue").assertIsSelected()
        dayCell("Thursday, October 8").assertExists()
        composeRule.onAllNodes(hasContentDescription("5 records", substring = true)).assertCountEquals(0)
    }

    @Test
    fun agendaItems_openTheirDestinations() {
        show()

        scrollTo("Wipers")
        composeRule.onNodeWithText("Wipers").performClick()
        scrollTo("Car wash")
        composeRule.onNodeWithText("Car wash").performClick()
        composeRule.onNodeWithContentDescription("Photo from Oct 12, 2026").performClick()
        scrollTo("Periodic inspection")
        composeRule.onNodeWithText("Periodic inspection").performClick()
        scrollTo("Update mileage")
        composeRule.onNodeWithText("Update mileage").performClick()

        assertThat(opened).containsExactly(
            CalendarFixtures.overdueWiper,
            RecordRef(TimelineEventType.Note, "wash"),
            "today",
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

    private companion object {
        const val SWIPE_PX = 150f
    }
}
