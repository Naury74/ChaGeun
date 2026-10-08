package com.naury.chageun.flow

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.naury.chageun.MainActivity
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.feature.manage.R as ManageR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 기획 문서 9.3 주행거리 충돌. 교체 기록의 주행거리가 현재 값이나 이전 기록과 어긋나면 저장 전에 묻는다. */
@RunWith(AndroidJUnit4::class)
class MileageConflictFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { AppRobot(rule) }

    @Test
    fun higherThanCurrentMileage_asksToUpdateOdometer() {
        val vehicle = robot.registerVehicle()
        val item = MaintenanceItem.EngineOil
        val higher = vehicle.mileage + 2_000
        robot.openManageItem(item)

        robot.startServiceRecord(item, higher)
        robot.saveServiceRecord()
        robot.waitForText(robot.text(ManageR.string.record_odometer_title))
        robot.bringIntoView(
            hasText(
                robot.text(ManageR.string.record_odometer_body, robot.number(vehicle.mileage), robot.number(higher)),
            ),
        )
        robot.clickText(ManageR.string.record_odometer_update)
        robot.finishSavedRecord(item)

        // 현재 주행거리가 올라갔으므로 같은 값으로 다시 저장할 때는 묻지 않고 바로 저장된다.
        robot.startServiceRecord(item, higher)
        robot.saveServiceRecord()
        robot.finishSavedRecord(item)
        rule.onAllNodes(hasText(robot.text(ManageR.string.record_odometer_title))).assertCountEquals(0)
    }

    @Test
    fun lowerThanPreviousRecord_warnsBeforeSaving() {
        val vehicle = robot.registerVehicle()
        val item = MaintenanceItem.EngineOil
        val lower = vehicle.mileage - 5_000
        robot.openManageItem(item)
        robot.startServiceRecord(item, vehicle.mileage)
        robot.saveServiceRecord()
        robot.finishSavedRecord(item)

        robot.startServiceRecord(item, lower)
        robot.saveServiceRecord()
        val warning = hasText(robot.text(ManageR.string.record_lower_mileage_title))
        robot.waitFor(warning)
        robot.bringIntoView(
            hasText(
                robot.text(
                    ManageR.string.record_lower_mileage_body,
                    robot.itemName(item),
                    robot.number(vehicle.mileage),
                ),
            ),
        )

        // 고치기를 누르면 안내가 닫히고 저장 버튼이 다시 보인다.
        robot.clickText(ManageR.string.record_lower_mileage_edit)
        robot.waitUntilGone(warning)
        robot.saveServiceRecord()

        // 계기판을 바꾼 경우처럼 낮은 값을 그대로 저장할 수도 있다.
        robot.waitFor(warning)
        robot.clickText(ManageR.string.record_lower_mileage_save)
        robot.finishSavedRecord(item)
    }
}
