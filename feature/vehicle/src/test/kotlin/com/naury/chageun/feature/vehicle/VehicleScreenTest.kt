package com.naury.chageun.feature.vehicle

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.MileageSource
import com.naury.chageun.core.model.RegistrationMode
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleId
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h3000dp")
class VehicleScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val vehicle = Vehicle(
        id = VehicleId("v1"),
        maker = "KG Mobility",
        model = "Torres",
        modelYear = 2023,
        trim = null,
        fuelType = FuelType.Gasoline,
        firstRegistrationDate = null,
        plateMasked = "123가 **67",
        registrationMode = RegistrationMode.Manual,
        isPrimary = true,
    )
    private val log = listOf(
        MileageEntry("m2", LocalDate.of(2026, 10, 1), Kilometers(1_200), MileageSource.Correction),
        MileageEntry("m1", LocalDate.of(2026, 9, 1), Kilometers(42_891), MileageSource.User),
    )

    @Test
    fun showsMaskedPlate_mileageSources_andNeverClaimsNoRecall() {
        var updateRequested = false
        composeRule.setContent {
            ChageunTheme {
                VehicleScreen(VehicleUiState.Content(vehicle, log), isTwoPane = false, onUpdateMileage = {
                    updateRequested =
                        true
                })
            }
        }

        composeRule.onNodeWithText("123가 **67").assertIsDisplayed()
        composeRule.onNodeWithText("Odometer correction").assertIsDisplayed()
        composeRule.onNodeWithText("Check recalls (Korea Automobile Recall Center)").assertIsDisplayed()
        composeRule.onNodeWithText("No recall", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Update mileage").performClick()

        assertThat(updateRequested).isTrue()
    }
}
