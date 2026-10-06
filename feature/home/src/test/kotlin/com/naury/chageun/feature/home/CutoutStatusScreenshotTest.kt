package com.naury.chageun.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.CutoutFailure
import com.naury.chageun.core.model.CutoutStatus
import com.naury.chageun.core.ui.CutoutStatusPanel
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 내 차 사진 배경 지우기 안내의 상태별 모습. 모델을 처음 받을 때 이유와 진행률을 보여 주는지 확인한다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CutoutStatusScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showAllStates() = composeRule.setContent {
        AppFrame {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    CutoutStatus.DownloadingModel(0.42f),
                    CutoutStatus.DownloadingModel(null),
                    CutoutStatus.Processing,
                    CutoutStatus.Failed(CutoutFailure.ModelUnavailable),
                    CutoutStatus.Failed(CutoutFailure.NoSubject),
                    CutoutStatus.Failed(CutoutFailure.Error),
                    CutoutStatus.Idle,
                ).forEach { CutoutStatusPanel(it, canRemoveBackground = true, onRemoveBackground = {}) }
            }
        }
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    fun allStates() {
        showAllStates()
        composeRule.captureScreen("cutout_status_phone")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun allStatesKorean() {
        showAllStates()
        composeRule.captureScreen("cutout_status_phone_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    fun allStatesDark() {
        showAllStates()
        composeRule.captureScreen("cutout_status_phone_dark")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    fun retry_callsRemoveBackground_butNoSubjectOffersNoRetry() {
        var retries = 0
        composeRule.setContent {
            AppFrame {
                Column {
                    CutoutStatusPanel(CutoutStatus.Failed(CutoutFailure.ModelUnavailable), true, { retries++ })
                    CutoutStatusPanel(CutoutStatus.Failed(CutoutFailure.NoSubject), true, { retries++ })
                }
            }
        }

        composeRule.onNodeWithText("Try again").performClick()

        assertThat(retries).isEqualTo(1)
        // 차를 찾지 못한 경우에는 다시 시도 버튼이 하나뿐이어야 한다(위 다운로드 실패 패널의 것).
        assertThat(composeRule.onAllNodes(hasText("Try again")).fetchSemanticsNodes())
            .hasSize(1)
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    fun idle_withCutoutAlready_showsNothing() {
        composeRule.setContent {
            AppFrame { CutoutStatusPanel(CutoutStatus.Idle, canRemoveBackground = false, onRemoveBackground = {}) }
        }

        composeRule.onNodeWithText("Remove background").assertDoesNotExist()
    }
}
