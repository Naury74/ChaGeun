package com.naury.chageun.flow

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.naury.chageun.MainActivity
import com.naury.chageun.core.domain.history.HistoryRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.feature.history.R as HistoryR
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 기록 상세의 소리 칸. 정비·점검 기록에는 녹음과 소리 파일 추가 버튼이 보이고, 주유 기록에는 칸이 없다.
 * 녹음 버튼은 FrameKit 녹음 화면과 마이크 권한 창을 열므로 누르지 않는다(CI 기기에는 마이크가 없다).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RecordSoundFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var vehicleRepository: VehicleRepository

    @Inject lateinit var historyRepository: HistoryRepository

    private val robot by lazy { AppRobot(rule) }

    @Before
    fun inject() = hiltRule.inject()

    @Test
    fun maintenanceRecord_showsSoundButtons() {
        val vehicle = robot.registerVehicle()
        val item = MaintenanceItem.EngineOil
        robot.recordReplacement(item, vehicle.mileage)

        robot.openHistoryRecord(hasText(robot.itemName(item)), title = robot.itemName(item))
        robot.scrollTo(hasText(robot.text(HistoryR.string.sound_title)))
        listOf(HistoryR.string.sound_record, HistoryR.string.sound_add_file).forEach {
            robot.scrollTo(hasText(robot.text(it)) and hasClickAction()).assertIsEnabled()
        }
    }

    @Test
    fun fuelRecord_hasNoSoundSection() {
        val vehicle = robot.registerVehicle()
        addFuelRecord(vehicle.mileage)

        // 주유 기록은 목록에서 "주유 · 주유소 이름"으로 보인다.
        robot.openHistoryRecord(
            hasText(STATION, substring = true),
            title = robot.text(TimelineEventType.Fuel.labelRes),
        )
        // 소리 칸은 맨 아래 수정·삭제 버튼 바로 위에 들어가므로, 버튼까지 내려도 보이지 않으면 칸이 없는 것이다.
        robot.scrollTo(hasText(robot.text(HistoryR.string.history_delete)) and hasClickAction())
        listOf(HistoryR.string.sound_title, HistoryR.string.sound_record, HistoryR.string.sound_add_file).forEach {
            rule.onAllNodes(hasText(robot.text(it))).assertCountEquals(0)
        }
    }

    /** 주유 입력 화면 대신 저장소로 오늘 주유 기록을 넣는다. 주행거리는 지금 값 그대로라 계기판을 바꾸지 않는다. */
    private fun addFuelRecord(mileage: Long) = runBlocking {
        val vehicleId = vehicleRepository.observePrimaryVehicle().filterNotNull().first().id
        val entry = FuelEntry(
            date = LocalDate.now(),
            mileage = Kilometers(mileage),
            amounts = FuelAmounts(
                totalPriceWon = FUEL_TOTAL_WON,
                volumeMl = FUEL_VOLUME_ML,
                unitPriceWon = FUEL_UNIT_PRICE_WON,
                computedField = null,
            ),
            isFullTank = true,
            stationName = STATION,
        )
        historyRepository.addFuel(vehicleId, entry, advancesOdometer = false)
    }

    private companion object {
        const val STATION = "차근주유소"
        const val FUEL_TOTAL_WON = 60_000L
        const val FUEL_VOLUME_ML = 36_000L
        const val FUEL_UNIT_PRICE_WON = 1_667L
    }
}
