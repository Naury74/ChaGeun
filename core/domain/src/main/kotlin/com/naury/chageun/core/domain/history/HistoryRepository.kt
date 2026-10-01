package com.naury.chageun.core.domain.history

import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.flow.Flow

data class TimelineQuery(
    val types: Set<TimelineEventType> = TimelineEventType.entries.toSet(),
    val keyword: String = "",
    /** Maintenance items whose localized name matches [keyword]; resolved by the UI that knows the locale. */
    val matchingItems: Set<MaintenanceItem> = emptySet(),
)

interface HistoryRepository {
    /** Newest first; undated maintenance entries follow dated ones. */
    fun observeTimeline(vehicleId: VehicleId, query: TimelineQuery): Flow<List<TimelineItem>>

    fun observeRecord(vehicleId: VehicleId, ref: RecordRef): Flow<RecordDetail?>

    suspend fun addFuel(vehicleId: VehicleId, entry: FuelEntry, advancesOdometer: Boolean)

    suspend fun addCheck(vehicleId: VehicleId, entry: CheckEntry, advancesOdometer: Boolean)

    /** Also removes the odometer reading that was created together with the record. */
    suspend fun delete(vehicleId: VehicleId, ref: RecordRef)
}
