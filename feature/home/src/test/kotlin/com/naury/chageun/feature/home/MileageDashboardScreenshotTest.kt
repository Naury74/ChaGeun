package com.naury.chageun.feature.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 계기판 사진 읽기에서 글자 인식 모델을 처음 내려받을 때와 받지 못했을 때의 안내. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MileageDashboardScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(dashboard: DashboardReadState, onRetry: () -> Unit = {}) = composeRule.setContent {
        AppFrame {
            MileageUpdateContent(
                uiState = MileageUpdateUiState(previous = Kilometers(42_891), dashboard = dashboard),
                onMileageChanged = {},
                onSave = {},
                onConfirmCorrection = {},
                onDismiss = {},
                onRetryDashboard = onRetry,
            )
        }
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun downloadingModel() {
        show(DashboardReadState.DownloadingModel(0.42f))
        composeRule.captureScreen("mileage_dashboard_downloading_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun modelUnavailable() {
        show(DashboardReadState.ModelUnavailable)
        composeRule.captureScreen("mileage_dashboard_model_failed_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    fun modelUnavailable_offersRetry_andAnotherPhoto() {
        var retries = 0
        show(DashboardReadState.ModelUnavailable) { retries++ }

        composeRule.onNodeWithText("Try again").performClick()

        assertThat(retries).isEqualTo(1)
        composeRule.onNodeWithText("Read from a dashboard photo", substring = true).assertExists()
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    fun downloadingModel_hidesPhotoButton() {
        show(DashboardReadState.DownloadingModel(null))

        composeRule.onNodeWithText("Downloading text recognition", substring = true).assertExists()
        composeRule.onNodeWithText("Read from a dashboard photo", substring = true).assertDoesNotExist()
    }
}
