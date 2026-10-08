package com.naury.chageun.flow

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.naury.chageun.MainActivity
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.feature.manage.R as ManageR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 기획 문서 9.3 정비 완료 저장. 관리 탭 상세에서 교체 기록을 저장하면 그 항목의 이력에 남아야 한다. */
@RunWith(AndroidJUnit4::class)
class ServiceRecordFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val robot by lazy { AppRobot(rule) }

    @Test
    fun recordReplacement_appearsInItemHistory() {
        val vehicle = robot.registerVehicle()
        val item = MaintenanceItem.EngineOil
        robot.openManageItem(item)
        robot.bringIntoView(hasText(robot.text(ManageR.string.manage_history_empty)))

        robot.startServiceRecord(item, vehicle.mileage)
        robot.saveServiceRecord()
        robot.finishSavedRecord(item)

        // 지난 교체와 교체 이력에 저장한 주행거리가 나온다.
        val savedMileage = robot.text(ManageR.string.manage_km, robot.number(vehicle.mileage))
        robot.waitFor(hasText(savedMileage, substring = true))
        robot.bringIntoView(hasText(savedMileage, substring = true))
        robot.waitUntilGone(hasText(robot.text(ManageR.string.manage_history_empty)))
    }
}
