package com.naury.chageun.feature.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.uitesting.assertNoClippedText
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
}
