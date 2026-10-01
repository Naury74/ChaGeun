package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.history.HistoryRepository
import com.naury.chageun.core.domain.history.TimelineQuery
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.VehicleId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeHistoryRepository : HistoryRepository {
    val timeline = MutableStateFlow<List<TimelineItem>>(emptyList())
    val details = MutableStateFlow<Map<RecordRef, RecordDetail>>(emptyMap())
    val addedFuel = mutableListOf<Pair<FuelEntry, Boolean>>()
    val addedChecks = mutableListOf<Pair<CheckEntry, Boolean>>()
    val deleted = mutableListOf<RecordRef>()
    var lastQuery: TimelineQuery? = null

    override fun observeTimeline(vehicleId: VehicleId, query: TimelineQuery): Flow<List<TimelineItem>> {
        lastQuery = query
        return timeline.map { items -> items.filter { it.ref.type in query.types } }
    }

    override fun observeRecord(vehicleId: VehicleId, ref: RecordRef): Flow<RecordDetail?> = details.map { it[ref] }

    override suspend fun addFuel(vehicleId: VehicleId, entry: FuelEntry, advancesOdometer: Boolean) {
        addedFuel += entry to advancesOdometer
    }

    override suspend fun addCheck(vehicleId: VehicleId, entry: CheckEntry, advancesOdometer: Boolean) {
        addedChecks += entry to advancesOdometer
    }

    override suspend fun delete(vehicleId: VehicleId, ref: RecordRef) {
        deleted += ref
        timeline.value = timeline.value.filterNot { it.ref == ref }
    }
}
