package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.history.HistoryRepository
import com.naury.chageun.core.domain.history.TimelineQuery
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.ServiceEntry
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.VehicleId
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeHistoryRepository : HistoryRepository {
    val timeline = MutableStateFlow<List<TimelineItem>>(emptyList())
    val details = MutableStateFlow<Map<RecordRef, RecordDetail>>(emptyMap())
    val addedFuel = mutableListOf<Pair<FuelEntry, Boolean>>()
    val addedChecks = mutableListOf<Pair<CheckEntry, Boolean>>()
    val deleted = mutableListOf<RecordRef>()
    val updated = mutableListOf<Pair<String, Any>>()
    var lastQuery: TimelineQuery? = null

    var lastLimit: Int? = null

    override fun observeTimeline(vehicleId: VehicleId, query: TimelineQuery, limit: Int?): Flow<List<TimelineItem>> {
        lastQuery = query
        lastLimit = limit
        return timeline.map { items ->
            items.filter { it.ref.type in query.types }.let { if (limit != null) it.take(limit) else it }
        }
    }

    override fun observeMonthlyCosts(vehicleId: VehicleId, query: TimelineQuery): Flow<Map<YearMonth?, Long>> =
        timeline.map { items ->
            items.filter { it.ref.type in query.types && it.costWon != null }
                .groupBy { item -> item.date?.let(YearMonth::from) }
                .mapValues { (_, monthItems) -> monthItems.sumOf { it.costWon ?: 0L } }
        }

    override fun observeRecord(vehicleId: VehicleId, ref: RecordRef): Flow<RecordDetail?> = details.map { it[ref] }

    override suspend fun addFuel(vehicleId: VehicleId, entry: FuelEntry, advancesOdometer: Boolean) {
        addedFuel += entry to advancesOdometer
    }

    override suspend fun addCheck(vehicleId: VehicleId, entry: CheckEntry, advancesOdometer: Boolean) {
        addedChecks += entry to advancesOdometer
    }

    override suspend fun updateFuel(vehicleId: VehicleId, id: String, entry: FuelEntry) {
        updated += id to entry
    }

    override suspend fun updateCheck(vehicleId: VehicleId, id: String, entry: CheckEntry) {
        updated += id to entry
    }

    override suspend fun updateMaintenance(vehicleId: VehicleId, id: String, entry: ServiceEntry) {
        updated += id to entry
    }

    override suspend fun delete(vehicleId: VehicleId, ref: RecordRef) {
        deleted += ref
        timeline.value = timeline.value.filterNot { it.ref == ref }
    }
}
