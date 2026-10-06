package com.naury.chageun.feature.vehicle

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.graphics.applyCanvas
import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.vehicle.CompleteInspectionUseCase
import com.naury.chageun.core.domain.vehicle.InspectionEvaluator
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.MileageSource
import com.naury.chageun.core.model.RegistrationMode
import com.naury.chageun.core.model.Vehicle
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.ui.photo.VehiclePhotoConfirmSheet
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import java.io.File
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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

    @Test
    @Config(fontScale = 2f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun largeFont_keepsEveryTextVisible() {
        composeRule.setContent {
            ChageunTheme {
                VehicleScreen(
                    VehicleUiState.Content(vehicle, log, dueIn(-12)),
                    isTwoPane = false,
                    onUpdateMileage = {},
                )
            }
        }

        composeRule.assertNoClippedText()
    }

    @Test
    fun inspection_showsDateAndDaysLeft_andCanBeRemoved() {
        var selected: LocalDate? = LocalDate.MIN
        composeRule.setContent {
            ChageunTheme {
                VehicleScreen(
                    VehicleUiState.Content(vehicle, log, dueIn(14)),
                    isTwoPane = false,
                    onUpdateMileage = {},
                    onInspectionDateSelected = { selected = it },
                )
            }
        }

        composeRule.onNodeWithText("14 days left").assertIsDisplayed()
        composeRule.onNodeWithText("Date entered by you").assertIsDisplayed()
        composeRule.onNodeWithText("Remove").performClick()

        assertThat(selected).isNull()
    }

    @Test
    fun inspection_withoutDate_offersEntry() {
        composeRule.setContent {
            ChageunTheme {
                VehicleScreen(VehicleUiState.Content(vehicle, log), isTwoPane = false, onUpdateMileage = {})
            }
        }

        composeRule.onNodeWithText("Enter inspection date").assertIsDisplayed()
        composeRule.onNodeWithText("days left", substring = true).assertDoesNotExist()
    }

    @Test
    // legacy graphics 모드에서는 Dialog 안의 텍스트 필드가 idle 상태가 되지 않는다.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun completeInspection_suggestsNextDate_andReportsIt() {
        val status = dueIn(14)
        var completion: InspectionCompletion? = null
        composeRule.setContent {
            ChageunTheme {
                VehicleScreen(
                    VehicleUiState.Content(vehicle, log, status),
                    isTwoPane = false,
                    onUpdateMileage = {},
                    onInspectionCompleted = { completion = it },
                )
            }
        }

        composeRule.onNodeWithText("Mark as inspected").performClick()
        composeRule.onNodeWithText("Inspection done").assertIsDisplayed()
        composeRule.onNode(hasText("Save") and hasAnyAncestor(isDialog())).performClick()

        val result = checkNotNull(completion)
        assertThat(result.mileage).isEqualTo(Kilometers(1_200))
        assertThat(result.nextDueDate)
            .isEqualTo(CompleteInspectionUseCase.suggestNextDueDate(result.completedOn, status.schedule?.nextDueDate))
    }

    private fun dueIn(days: Long): InspectionStatus = InspectionEvaluator.evaluate(
        InspectionSchedule(LocalDate.of(2026, 10, 1).plusDays(days), InspectionSource.User),
        LocalDate.of(2026, 10, 1),
    )

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_phone() {
        composeRule.setContent {
            AppFrame {
                VehicleScreen(VehicleUiState.Content(vehicle, log, dueIn(14)), isTwoPane = false, onUpdateMileage = {})
            }
        }
        composeRule.captureScreen("vehicle_phone")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_phoneDark() {
        composeRule.setContent {
            AppFrame {
                VehicleScreen(VehicleUiState.Content(vehicle, log, dueIn(14)), isTwoPane = false, onUpdateMileage = {})
            }
        }
        composeRule.captureScreen("vehicle_phone_dark")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.TABLET)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_tablet() {
        composeRule.setContent {
            AppFrame {
                VehicleScreen(VehicleUiState.Content(vehicle, log, dueIn(14)), isTwoPane = true, onUpdateMileage = {})
            }
        }
        composeRule.captureScreen("vehicle_tablet")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_phoneWithPhoto() {
        val photo = File.createTempFile("vehicle", ".jpg").apply { deleteOnExit() }
        // 실제 사진 대신 하늘·도로 두 색으로 나눈 가로 이미지를 쓴다. 세로가 긴 사진이어도 잘리지 않는지 보기 위해 4:3 비율이다.
        val bitmap = Bitmap.createBitmap(PHOTO_WIDTH, PHOTO_HEIGHT, Bitmap.Config.ARGB_8888).applyCanvas {
            drawColor(Color.rgb(176, 205, 230))
            drawRect(
                0f,
                PHOTO_HEIGHT * 0.6f,
                PHOTO_WIDTH.toFloat(),
                PHOTO_HEIGHT.toFloat(),
                Paint().apply {
                    color =
                        Color.DKGRAY
                },
            )
        }
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }

        composeRule.setContent {
            AppFrame {
                VehicleScreen(
                    VehicleUiState.Content(vehicle, log, dueIn(14), photoPath = photo.absolutePath),
                    isTwoPane = false,
                    onUpdateMileage = {},
                )
            }
        }
        // 사진은 IO 스레드에서 디코딩되므로 화면에 붙을 때까지 기다린다.
        composeRule.waitUntil(PHOTO_LOAD_TIMEOUT_MS) {
            composeRule.onAllNodesWithContentDescription("Photo of your car").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.captureScreen("vehicle_phone_photo")
    }

    private companion object {
        const val PHOTO_WIDTH = 800
        const val PHOTO_HEIGHT = 600
        const val JPEG_QUALITY = 90
        const val PHOTO_LOAD_TIMEOUT_MS = 5_000L
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun screenshot_photoConfirmKorean() {
        val photo = File.createTempFile("pick", ".jpg").apply { deleteOnExit() }
        val bitmap = Bitmap.createBitmap(PHOTO_WIDTH, PHOTO_HEIGHT, Bitmap.Config.ARGB_8888).applyCanvas {
            drawColor(Color.rgb(176, 205, 230))
        }
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }

        composeRule.setContent {
            AppFrame {
                VehiclePhotoConfirmSheet(
                    sourceUri = photo.toUri().toString(),
                    initialRemoveBackground = true,
                    isExpanded = true,
                    onApply = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.waitUntil(PHOTO_LOAD_TIMEOUT_MS) {
            composeRule.onAllNodesWithContentDescription("내 차 사진").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.captureScreen("vehicle_photo_confirm_ko")
    }
}
