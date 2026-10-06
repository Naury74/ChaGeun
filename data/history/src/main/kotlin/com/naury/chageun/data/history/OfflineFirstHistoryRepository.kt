package com.naury.chageun.data.history

import androidx.room.withTransaction
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.CheckRecordEntity
import com.naury.chageun.core.database.entity.FuelRecordEntity
import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MileageRecordEntity
import com.naury.chageun.core.database.entity.RecordSourceTypes
import com.naury.chageun.core.database.entity.TimelineRow
import com.naury.chageun.core.domain.history.HistoryRepository
import com.naury.chageun.core.domain.history.TimelineQuery
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.FuelAmounts
import com.naury.chageun.core.model.FuelEntry
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.ServiceHistoryEntry
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class OfflineFirstHistoryRepository @Inject constructor(
    private val database: ChageunDatabase,
    private val clock: Clock,
) : HistoryRepository {

    private val historyDao get() = database.historyDao()

    override fun observeTimeline(vehicleId: VehicleId, query: TimelineQuery): Flow<List<TimelineItem>> =
        historyDao.observeTimeline(
            vehicleId = vehicleId.value,
            eventTypes = query.types.map { it.name },
            keyword = query.keyword.trim(),
            matchingItemTypes = query.matchingItems.map { it.name },
        ).map { rows -> rows.mapNotNull { it.asTimelineItem() } }

    override fun observeRecord(vehicleId: VehicleId, ref: RecordRef): Flow<RecordDetail?> = when (ref.type) {
        TimelineEventType.Maintenance -> historyDao.observeMaintenance(vehicleId.value, ref.id).map {
            it?.asDetail(ref)
        }
        TimelineEventType.Fuel -> historyDao.observeFuel(vehicleId.value, ref.id).map { it?.asDetail(ref) }
        TimelineEventType.Inspection, TimelineEventType.Repair, TimelineEventType.Note ->
            historyDao.observeCheck(vehicleId.value, ref.id).map { it?.asDetail(ref) }
    }

    override suspend fun addFuel(vehicleId: VehicleId, entry: FuelEntry, advancesOdometer: Boolean) {
        val now = clock.instant()
        val record = FuelRecordEntity(
            id = UUID.randomUUID().toString(),
            vehicleId = vehicleId.value,
            fuelDate = entry.date,
            mileageKm = entry.mileage.value,
            totalPriceWon = entry.amounts.totalPriceWon,
            volumeMl = entry.amounts.volumeMl,
            unitPriceWon = entry.amounts.unitPriceWon,
            computedField = entry.amounts.computedField?.name,
            isFullTank = entry.isFullTank,
            stationName = entry.stationName.normalized(),
            memo = entry.memo.normalized(),
            createdAt = now,
            updatedAt = now,
        )
        database.withTransaction {
            historyDao.insertFuel(record)
            if (advancesOdometer) insertOdometer(vehicleId, entry.mileage, entry.date, SOURCE_FUEL, record.id)
        }
    }

    override suspend fun addCheck(vehicleId: VehicleId, entry: CheckEntry, advancesOdometer: Boolean) {
        val now = clock.instant()
        val record = CheckRecordEntity(
            id = UUID.randomUUID().toString(),
            vehicleId = vehicleId.value,
            kind = entry.kind.name,
            checkDate = entry.date,
            title = entry.title,
            mileageKm = entry.mileage?.value,
            costWon = entry.costWon,
            memo = entry.memo.normalized(),
            createdAt = now,
            updatedAt = now,
        )
        database.withTransaction {
            historyDao.insertCheck(record)
            val mileage = entry.mileage
            if (advancesOdometer &&
                mileage != null
            ) {
                insertOdometer(vehicleId, mileage, entry.date, SOURCE_CHECK, record.id)
            }
        }
    }

    override suspend fun delete(vehicleId: VehicleId, ref: RecordRef) {
        database.withTransaction {
            when (ref.type) {
                TimelineEventType.Maintenance -> historyDao.deleteMaintenance(vehicleId.value, ref.id)
                TimelineEventType.Fuel -> historyDao.deleteFuel(vehicleId.value, ref.id)
                TimelineEventType.Inspection, TimelineEventType.Repair, TimelineEventType.Note ->
                    historyDao.deleteCheck(vehicleId.value, ref.id)
            }
            historyDao.deleteMileageCreatedBy(vehicleId.value, ref.id)
        }
    }

    private suspend fun insertOdometer(
        vehicleId: VehicleId,
        mileage: Kilometers,
        date: LocalDate,
        source: String,
        recordId: String,
    ) {
        database.mileageRecordDao().insert(
            MileageRecordEntity(
                id = UUID.randomUUID().toString(),
                vehicleId = vehicleId.value,
                mileageKm = mileage.value,
                recordedOn = date,
                sourceType = source,
                relatedRecordId = recordId,
                createdAt = clock.instant(),
            ),
        )
    }

    private fun String?.normalized() = this?.trim()?.ifEmpty { null }

    private companion object {
        const val SOURCE_FUEL = "FUEL"
        const val SOURCE_CHECK = "CHECK"
    }
}

/** 더 새로운 앱 버전이 쓴 경우처럼 알 수 없는 유형이나 항목의 행은 건너뛴다. */
private fun TimelineRow.asTimelineItem(): TimelineItem? {
    val type = TimelineEventType.entries.firstOrNull { it.name == eventType } ?: return null
    val item = itemType?.let { name -> MaintenanceItem.entries.firstOrNull { it.name == name } ?: return null }
    return TimelineItem(
        ref = RecordRef(type, id),
        date = occurredOn,
        title = title,
        maintenanceItem = item,
        mileage = mileageKm?.let(::Kilometers),
        costWon = costWon,
        source = RecordSource.User,
        createdAt = createdAt,
    )
}

private fun MaintenanceRecordEntity.asDetail(ref: RecordRef): RecordDetail? {
    val item = MaintenanceItem.entries.firstOrNull { it.name == itemType } ?: return null
    return RecordDetail.Maintenance(
        ref = ref,
        item = item,
        entry = ServiceHistoryEntry(
            id,
            serviceDate,
            mileageKm?.let(::Kilometers),
            costWon,
            shopName,
            isMileageEstimated = sourceType == RecordSourceTypes.ESTIMATED,
        ),
        memo = memo,
    )
}

private fun FuelRecordEntity.asDetail(ref: RecordRef) = RecordDetail.Fuel(
    ref = ref,
    entry = FuelEntry(
        date = fuelDate,
        mileage = Kilometers(mileageKm),
        amounts = FuelAmounts(
            totalPriceWon = totalPriceWon,
            volumeMl = volumeMl,
            unitPriceWon = unitPriceWon,
            computedField = computedField?.let { name -> FuelField.entries.firstOrNull { it.name == name } },
        ),
        isFullTank = isFullTank,
        stationName = stationName,
        memo = memo,
    ),
)

private fun CheckRecordEntity.asDetail(ref: RecordRef): RecordDetail? {
    val checkKind = CheckKind.entries.firstOrNull { it.name == kind } ?: return null
    return RecordDetail.Check(
        ref = ref,
        entry = CheckEntry(checkKind, checkDate, title, mileageKm?.let(::Kilometers), costWon, memo),
    )
}
