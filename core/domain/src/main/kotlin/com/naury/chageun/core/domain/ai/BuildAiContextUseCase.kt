package com.naury.chageun.core.domain.ai

import com.naury.chageun.core.domain.history.HistoryRepository
import com.naury.chageun.core.domain.history.TimelineQuery
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.vehicle.InspectionEvaluator
import com.naury.chageun.core.domain.vehicle.InspectionRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class BuildAiContextUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val observeOverview: ObserveMaintenanceOverviewUseCase,
    private val historyRepository: HistoryRepository,
    private val inspectionRepository: InspectionRepository,
    private val clock: Clock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(options: Flow<AiContextOptions>): Flow<AiContextFacts> =
        vehicleRepository.observePrimaryVehicle()
            .filterNotNull()
            .flatMapLatest { vehicle ->
                val focused = options.map { it.focusRecord }.distinctUntilChanged().flatMapLatest { ref ->
                    ref?.let { historyRepository.observeRecord(vehicle.id, it) } ?: flowOf(null)
                }
                val extras = observeExtras(vehicle.id)
                combine(
                    observeOverview(vehicle.id),
                    historyRepository.observeTimeline(vehicle.id, TimelineQuery()),
                    options,
                    focused,
                    extras,
                ) { overview, timeline, opts, record, extra ->
                    // 정비 기록을 물으면 그 항목에 대한 질문으로 보고 상태와 이력을 그 항목으로 좁힌다.
                    val focusItem = opts.focusItem ?: (record as? RecordDetail.Maintenance)?.item
                    val relevant = overview.statuses.filter { status ->
                        if (focusItem != null) status.item == focusItem else status.state != MaintenanceState.Good
                    }
                    AiContextFacts(
                        asOf = LocalDate.now(clock),
                        maker = vehicle.maker,
                        model = vehicle.model,
                        modelYear = vehicle.modelYear,
                        fuelType = vehicle.fuelType,
                        mileage = overview.currentMileage,
                        health = overview.health.level,
                        maintenance = if (opts.includeMaintenance) {
                            relevant.filter { it.state != MaintenanceState.Unknown }
                        } else {
                            emptyList()
                        },
                        missingInfo = relevant.filter { it.state == MaintenanceState.Unknown }.map { it.item },
                        recentRecords = if (opts.includeRecords) {
                            timeline
                                .filter { focusItem == null || it.maintenanceItem == focusItem }
                                .filter { it.ref != opts.focusRecord }
                                .take(MAX_SHARED_RECORDS)
                                .map { it.toSharedRecord(includeCost = opts.includeCosts) }
                        } else {
                            emptyList()
                        },
                        focusRecord = record?.toSharedRecord(includeCost = opts.includeCosts),
                        inspection = extra.inspection,
                        costTotals = extra.costTotals.takeIf { opts.includeRecords && opts.includeCosts },
                    )
                }
            }

    /** 메모·정비소·주유소 이름은 넣지 않는다. 자유 텍스트이거나 다니는 장소를 드러내기 때문이다. */
    private fun RecordDetail.toSharedRecord(includeCost: Boolean): SharedRecord = when (this) {
        is RecordDetail.Maintenance -> SharedRecord(
            type = ref.type,
            date = entry.date,
            maintenanceItem = item,
            title = null,
            mileage = entry.mileage,
            costWon = entry.costWon.takeIf { includeCost },
        )
        is RecordDetail.Fuel -> SharedRecord(
            type = ref.type,
            date = entry.date,
            maintenanceItem = null,
            title = null,
            mileage = entry.mileage,
            costWon = entry.amounts.totalPriceWon.takeIf { includeCost },
            fuelVolumeMl = entry.amounts.volumeMl,
            fuelUnitPriceWon = entry.amounts.unitPriceWon.takeIf { includeCost },
            isFullTank = entry.isFullTank,
        )
        is RecordDetail.Check -> SharedRecord(
            type = ref.type,
            date = entry.date,
            maintenanceItem = null,
            title = entry.title,
            mileage = entry.mileage,
            costWon = entry.costWon.takeIf { includeCost },
        )
        is RecordDetail.Mileage -> SharedRecord(
            type = ref.type,
            date = date,
            maintenanceItem = null,
            title = null,
            mileage = mileage,
            costWon = null,
        )
    }

    private fun TimelineItem.toSharedRecord(includeCost: Boolean) = SharedRecord(
        type = ref.type,
        date = date,
        maintenanceItem = maintenanceItem,
        title = title.takeIf { ref.type != TimelineEventType.Fuel && maintenanceItem == null },
        mileage = mileage,
        costWon = costWon.takeIf { includeCost },
    )

    private fun observeExtras(vehicleId: VehicleId): Flow<Extras> {
        val year = LocalDate.now(clock).year
        return combine(
            inspectionRepository.observeSchedule(vehicleId),
            historyRepository.observeMonthlyCosts(vehicleId, TimelineQuery(types = MAINTENANCE_TYPES)),
            historyRepository.observeMonthlyCosts(vehicleId, TimelineQuery(types = setOf(TimelineEventType.Fuel))),
        ) { schedule, maintenanceCosts, fuelCosts ->
            Extras(
                inspection = schedule?.let { InspectionEvaluator.evaluate(it, LocalDate.now(clock)) },
                costTotals = CostTotals(year, maintenanceCosts.sumForYear(year), fuelCosts.sumForYear(year)),
            )
        }
    }

    private class Extras(val inspection: InspectionStatus?, val costTotals: CostTotals)

    private fun Map<YearMonth?, Long>.sumForYear(year: Int): Long =
        entries.sumOf { (month, won) -> if (month?.year == year) won else 0L }

    private companion object {
        const val MAX_SHARED_RECORDS = 5

        /** "정비비"에 넣는 기록. 주유는 따로 센다. */
        val MAINTENANCE_TYPES = setOf(
            TimelineEventType.Maintenance,
            TimelineEventType.Inspection,
            TimelineEventType.Repair,
            TimelineEventType.Note,
        )
    }
}
