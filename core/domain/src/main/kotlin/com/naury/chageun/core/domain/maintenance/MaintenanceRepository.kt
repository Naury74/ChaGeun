package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.ServiceHistoryEntry
import com.naury.chageun.core.model.ServiceRecord
import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.flow.Flow

data class MaintenanceInputs(
    /** 다시 켤 수 있도록 비활성 규칙도 포함한다. 평가에서는 건너뛴다. */
    val rules: List<MaintenanceRule>,
    val lastServices: Map<MaintenanceItem, ServiceRecord>,
    val mileageHistory: List<MileageReading>,
    /** 주행 기록이 적을 때 연식부터의 평균 주행 속도를 구하는 데 쓴다. */
    val modelYear: Int? = null,
)

interface MaintenanceRepository {
    /** 차량의 규칙, 정비 기록, 주행거리 기록이 바뀔 때마다 다시 emit한다. */
    fun observeInputs(vehicleId: VehicleId): Flow<MaintenanceInputs>

    /** 최신순. 날짜 없는 기록은 날짜 있는 기록 뒤에 온다. */
    fun observeServiceHistory(vehicleId: VehicleId, item: MaintenanceItem): Flow<List<ServiceHistoryEntry>>

    suspend fun findRule(vehicleId: VehicleId, item: MaintenanceItem): MaintenanceRule?

    suspend fun saveRule(vehicleId: VehicleId, rule: MaintenanceRule)

    suspend fun findLatestService(vehicleId: VehicleId, item: MaintenanceItem): ServiceRecord?

    suspend fun findCurrentMileage(vehicleId: VehicleId): MileageReading?

    /** [isCorrection]은 계기판 교체 후처럼 이전 값보다 낮은 주행거리 기록을 표시한다. */
    suspend fun addMileageReading(vehicleId: VehicleId, reading: MileageReading, isCorrection: Boolean)

    /**
     * [entry]와, [advancesOdometer]가 true이면 이 정비에서 나온 주행거리 기록까지
     * 하나의 Transaction으로 저장한다.
     */
    /** 새 기록의 ID를 돌려준다. 사진을 바로 붙일 때 쓴다. */
    suspend fun recordService(vehicleId: VehicleId, entry: ServiceEntry, advancesOdometer: Boolean): String
}
