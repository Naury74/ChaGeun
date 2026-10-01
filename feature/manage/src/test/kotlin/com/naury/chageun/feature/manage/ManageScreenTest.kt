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
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.ServiceHistoryEntry
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
                ManageScreen(stateFor(selected), isTwoPane = false, onFilterSelected = {}, onItemSelected = {
                    selected =
                        it
                })
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
    fun twoPane_showsPlaceholderUntilSelection() {
        composeRule.setContent {
            ChageunTheme {
                ManageScreen(stateFor(null), isTwoPane = true, onFilterSelected = {}, onItemSelected = {})
            }
        }

        composeRule.onNodeWithText("All 1").assertIsDisplayed()
        composeRule.onNodeWithText("Select an item to see why it has this status").assertIsDisplayed()
    }
}
