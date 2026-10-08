package com.naury.chageun.flow

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.naury.chageun.MainActivity
import com.naury.chageun.feature.home.R as HomeR
import com.naury.chageun.feature.onboarding.R as OnboardingR
import com.naury.chageun.feature.settings.R as SettingsR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 기획 문서 9.3 데이터 삭제. 설정에서 모든 데이터를 지우면 차량이 사라지고 온보딩부터 다시 시작한다. */
@RunWith(AndroidJUnit4::class)
class DataDeletionFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { AppRobot(rule) }

    @Test
    fun deleteAllData_returnsToOnboarding() {
        val vehicle = robot.registerVehicle()

        robot.click(hasContentDescription(robot.text(HomeR.string.home_open_settings)) and hasClickAction())
        robot.clickText(SettingsR.string.settings_delete_all)
        robot.waitForText(robot.text(SettingsR.string.settings_delete_title))
        robot.clickText(SettingsR.string.settings_delete_confirm)

        robot.waitForText(robot.text(OnboardingR.string.onboarding_start), AppRobot.REGISTER_TIMEOUT_MILLIS)
        rule.onAllNodes(hasText(vehicle.title)).assertCountEquals(0)
    }

    @Test
    fun cancelDeletion_keepsVehicle() {
        val vehicle = robot.registerVehicle()

        robot.click(hasContentDescription(robot.text(HomeR.string.home_open_settings)) and hasClickAction())
        robot.clickText(SettingsR.string.settings_delete_all)
        robot.waitForText(robot.text(SettingsR.string.settings_delete_title))
        robot.clickText(SettingsR.string.settings_cancel)
        robot.waitUntilGone(hasText(robot.text(SettingsR.string.settings_delete_title)))

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        robot.bringIntoView(hasText(vehicle.title))
    }
}
