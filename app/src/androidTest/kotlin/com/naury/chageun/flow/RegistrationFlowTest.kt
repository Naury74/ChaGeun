package com.naury.chageun.flow

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.naury.chageun.MainActivity
import com.naury.chageun.core.ui.R as UiR
import com.naury.chageun.feature.onboarding.R as OnboardingR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 기획 문서 9.3 차량 등록. 처음 설치한 상태에서 온보딩을 마치면 홈에 등록한 차량이 보여야 한다.
 * 번호판 조회 API가 아직 없어 번호판을 넣는 경우와 건너뛰는 경우 모두 직접 입력으로 등록한다.
 */
@RunWith(AndroidJUnit4::class)
class RegistrationFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { AppRobot(rule) }

    @Test
    fun registerWithPlate_showsVehicleOnHome() {
        val vehicle = robot.registerVehicle(plate = AppRobot.TEST_PLATE)

        robot.bringIntoView(hasText(vehicle.title))
    }

    @Test
    fun registerWithoutPlate_showsVehicleOnHome() {
        val vehicle = robot.registerVehicle(plate = null)

        robot.bringIntoView(hasText(vehicle.title))
    }

    @Test
    fun invalidPlate_staysOnPlateStepWithError() {
        robot.clickText(OnboardingR.string.onboarding_start)
        val plateField =
            hasSetTextAction() and hasContentDescription(robot.text(OnboardingR.string.onboarding_plate_field))
        robot.inputText(plateField, "1234")
        robot.clickText(OnboardingR.string.onboarding_next)

        robot.waitForText(robot.text(OnboardingR.string.onboarding_error_plate_invalid))
        // 차량 정보 단계로 넘어가지 않았다.
        rule.onAllNodes(hasText(robot.text(UiR.string.maker_hyundai))).assertCountEquals(0)
    }
}
