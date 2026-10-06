package com.naury.chageun.core.domain.history

import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.flow.Flow

data class TimelineQuery(
    val types: Set<TimelineEventType> = TimelineEventType.entries.toSet(),
    val keyword: String = "",
    /** 현지화된 이름이 [keyword]와 일치하는 정비 항목. locale을 아는 UI 쪽에서 판단한다. */
    val matchingItems: Set<MaintenanceItem> = emptySet(),
)

interface HistoryRepository {
    /** 최신순. 날짜 없는 정비 기록은 날짜 있는 기록 뒤에 온다. */
    fun observeTimeline(vehicleId: VehicleId, query: TimelineQuery): Flow<List<TimelineItem>>

    fun observeRecord(vehicleId: VehicleId, ref: RecordRef): Flow<RecordDetail?>

    suspend fun addFuel(vehicleId: VehicleId, entry: FuelEntry, advancesOdometer: Boolean)

    suspend fun addCheck(vehicleId: VehicleId, entry: CheckEntry, advancesOdometer: Boolean)

    /**
     * 기록을 고친다. 첨부와 생성 시각은 그대로 두고, 이 기록이 만든 주행거리 기록은 지운 뒤
     * 새 값이 지금 주행거리보다 크면 다시 만든다. 기록이 없으면 아무것도 하지 않는다.
     */
    suspend fun updateFuel(vehicleId: VehicleId, id: String, entry: FuelEntry)

    suspend fun updateCheck(vehicleId: VehicleId, id: String, entry: CheckEntry)

    /** 사용자가 값을 확인했으므로 추정 주행거리 표시는 지운다. 정비 항목은 바꾸지 않는다. */
    suspend fun updateMaintenance(vehicleId: VehicleId, id: String, entry: ServiceEntry)

    /** 기록과 함께 생성된 주행거리 기록도 지운다. */
    suspend fun delete(vehicleId: VehicleId, ref: RecordRef)
}
