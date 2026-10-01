package com.naury.chageun.feature.manage.record

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h2000dp")
class RecordServiceContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var confirmed = false
    private var dismissed = false
    private val actions = RecordServiceActions(
        onDateSelected = {},
        onMileageChanged = {},
        onCostChanged = {},
        onShopNameChanged = {},
        onMemoChanged = {},
        onSave = {},
        onConfirmLowerMileage = { confirmed = true },
        onEditLowerMileage = {},
        onDismiss = { dismissed = true },
    )

    private fun show(state: RecordServiceUiState) = composeRule.setContent {
        ChageunTheme { RecordServiceContent(state, actions) }
    }

    private val base =
        RecordServiceUiState(item = MaintenanceItem.EngineOil, date = LocalDate.of(2026, 10, 1), mileage = "42891")

    @Test
    fun showsLowerMileageWarning_andConfirms() {
        show(base.copy(mileage = "39000", lowerMileageWarning = Kilometers(40_260)))

        composeRule.onNodeWithText("Lower than the previous record").assertIsDisplayed()
        composeRule.onNodeWithText("Save as is").performClick()

        assertThat(confirmed).isTrue()
    }

    @Test
    fun showsNextThreshold_afterSaving() {
        show(base.copy(savedResult = SavedResult(Kilometers(52_891), LocalDate.of(2027, 10, 1))))

        composeRule.onNodeWithText("Engine oil replacement saved").assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()

        assertThat(dismissed).isTrue()
    }
}
