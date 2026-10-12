package com.naury.chageun.flow

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.naury.chageun.MainActivity
import com.naury.chageun.R
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.feature.history.R as HistoryR
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 기록 탭 달력. 저장한 교체 기록이 그날 칸에 보이고, 눌러 상세를 열고, 검색으로 다시 찾아 그날로 간다. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HistoryCalendarFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { AppRobot(rule) }

    @Test
    fun savedReplacement_appearsOnItsDay_opensDetail_andIsFoundBySearch() {
        val vehicle = robot.registerVehicle()
        val item = MaintenanceItem.EngineOil
        robot.openManageItem(item)
        robot.startServiceRecord(item, vehicle.mileage)
        robot.saveServiceRecord()
        robot.finishSavedRecord(item)

        robot.openTab(R.string.nav_history)
        robot.clickText(HistoryR.string.history_view_calendar)

        // 오늘 칸이 교체한 항목 이름을 읽어 준다. 교체 기록 시트의 날짜는 오늘이 기본값이다.
        val itemName = robot.itemName(item)
        val todayCell = hasContentDescription(
            robot.text(HistoryR.string.calendar_a11y_replacement, itemName),
            substring = true,
        )
        robot.click(todayCell)
        robot.waitFor(todayCell and isSelected())

        robot.click(hasText(itemName) and hasClickAction())
        // 상세 맨 위 제목으로 열린 것을 확인한다. 수정 버튼은 사진·소리 칸 아래라 짧은 화면에서는 그려지지 않는다.
        robot.waitFor(hasText(itemName) and isHeading())
        // 한 칸·두 칸 배치 모두 시스템 뒤로 가기로 상세를 닫는다.
        Espresso.pressBack()

        // 짧은 화면에서 그날 목록의 행을 누르려고 스크롤하면 달력이 주 보기로 접힌다. 달 이동은 월 보기에서 한다.
        val weekView = hasContentDescription(robot.text(HistoryR.string.calendar_previous_week))
        if (robot.exists(weekView)) {
            robot.clickText(HistoryR.string.calendar_month_view)
            robot.waitUntilGone(weekView)
        }

        // 다른 달로 옮긴 뒤 검색 결과를 눌러 그날로 돌아온다.
        robot.click(hasContentDescription(robot.text(HistoryR.string.calendar_previous_month)) and hasClickAction())
        // 앞 달 그리드 끝에 이번 달 첫 주가 보일 수 있으므로 칸이 사라지는 대신 선택이 풀리는 것을 기다린다.
        robot.waitUntilGone(todayCell and isSelected())
        robot.click(hasContentDescription(robot.text(HistoryR.string.calendar_search)) and hasClickAction())
        robot.inputText(hasSetTextAction(), itemName)
        // 검색칸도 같은 글자를 담고 눌리므로 결과 행만 고른다.
        robot.click(hasText(itemName) and hasClickAction() and !hasSetTextAction())
        robot.waitFor(todayCell and isSelected())
    }
}
