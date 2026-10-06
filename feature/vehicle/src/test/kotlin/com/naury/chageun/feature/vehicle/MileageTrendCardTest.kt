package com.naury.chageun.feature.vehicle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.MileageSource
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.captureScreen
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 주행거리 추이 그래프. 기록 간격이 고르지 않아도 가로축이 실제 기간을 따르는지 눈으로 확인한다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MileageTrendCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val trend = MileageTrend.from(
        listOf(
            LocalDate.of(2025, 10, 6) to 30_200L,
            LocalDate.of(2025, 12, 20) to 32_900L,
            LocalDate.of(2026, 1, 15) to 33_400L,
            LocalDate.of(2026, 4, 6) to 36_800L,
            LocalDate.of(2026, 8, 3) to 40_100L,
            LocalDate.of(2026, 10, 2) to 42_800L,
        ).map { (date, km) -> MileageEntry("$date", date, Kilometers(km), MileageSource.User) },
    )!!

    private fun show() = composeRule.setContent {
        AppFrame { Column { MileageTrendCard(trend, Modifier.padding(16.dp)) } }
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun korean() {
        show()
        composeRule.captureScreen("vehicle_mileage_trend_ko")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    fun dark() {
        show()
        composeRule.captureScreen("vehicle_mileage_trend_dark")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE)
    fun screenReader_readsOneSummarySentence() {
        show()

        composeRule.onNodeWithContentDescription(
            "From Oct 6, 2025 (30,200 km) to Oct 2, 2026 (42,800 km). About 1,061 km a month",
        ).assertExists()
    }
}
