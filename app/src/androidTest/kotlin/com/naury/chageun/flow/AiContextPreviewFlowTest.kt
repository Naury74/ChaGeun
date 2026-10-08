package com.naury.chageun.flow

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.naury.chageun.MainActivity
import com.naury.chageun.feature.ai.R as AiR
import com.naury.chageun.feature.home.R as HomeR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 기획 문서 9.3 AI Context Preview. 외부 AI로 보내기 전에 보낼 정보와 보내지 않는 정보를 보여 주고,
 * 실제로 보낼 글에는 차량 정보가 들어가되 번호판은 빠져야 한다.
 */
@RunWith(AndroidJUnit4::class)
class AiContextPreviewFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { AppRobot(rule) }

    @Test
    fun preview_listsSharedFields_andPromptExcludesPlate() {
        val vehicle = robot.registerVehicle(plate = AppRobot.TEST_PLATE)

        robot.clickText(HomeR.string.home_ai_title)
        robot.waitForText(robot.text(AiR.string.ai_preview_title))

        listOf(
            AiR.string.ai_included,
            AiR.string.ai_included_vehicle,
            AiR.string.ai_included_mileage,
            AiR.string.ai_included_maintenance,
            AiR.string.ai_excluded,
            AiR.string.ai_excluded_plate,
            AiR.string.ai_excluded_owner,
            AiR.string.ai_excluded_location,
        ).forEach { robot.bringIntoView(hasText(robot.text(it))) }

        // 한 칸 화면은 전체 글을 접어 두고, 넓은 화면은 처음부터 펼쳐 둔다.
        val showText = hasText(robot.text(AiR.string.ai_show_text))
        if (robot.exists(showText)) robot.click(showText)

        val vehicleLine = robot.text(
            AiR.string.ai_prompt_vehicle,
            listOf(vehicle.title, vehicle.modelYear.toString(), vehicle.fuel).joinToString(" · "),
        )
        robot.waitFor(hasText(vehicleLine, substring = true))
        robot.bringIntoView(hasText(robot.text(AiR.string.ai_prompt_footer), substring = true))
        rule.onAllNodes(hasText(checkNotNull(vehicle.plate), substring = true)).assertCountEquals(0)
    }
}
