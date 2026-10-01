package com.naury.chageun.core.domain.ai

import com.naury.chageun.core.domain.history.HistoryRepository
import com.naury.chageun.core.domain.history.TimelineQuery
import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

class BuildAiContextUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val observeOverview: ObserveMaintenanceOverviewUseCase,
    private val historyRepository: HistoryRepository,
    private val clock: Clock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(options: Flow<AiContextOptions>): Flow<AiContextFacts> =
        vehicleRepository.observePrimaryVehicle()
            .filterNotNull()
            .flatMapLatest { vehicle ->
                combine(
                    observeOverview(vehicle.id),
                    historyRepository.observeTimeline(vehicle.id, TimelineQuery()),
                    options,
                ) { overview, timeline, opts ->
                    val relevant = overview.statuses.filter { status ->
                        if (opts.focusItem !=
                            null
                        ) {
                            status.item == opts.focusItem
                        } else {
                            status.state != MaintenanceState.Good
                        }
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
                            relevant.filter {
                                it.state != MaintenanceState.Unknown
                            }
                        } else {
                            emptyList()
                        },
                        missingInfo = relevant.filter { it.state == MaintenanceState.Unknown }.map { it.item },
                        recentRecords = if (opts.includeRecords) {
                            timeline
                                .filter { opts.focusItem == null || it.maintenanceItem == opts.focusItem }
                                .take(MAX_SHARED_RECORDS)
                                .map { it.toSharedRecord(includeCost = opts.includeCosts) }
                        } else {
                            emptyList()
                        },
                    )
                }
            }

    private fun TimelineItem.toSharedRecord(includeCost: Boolean) = SharedRecord(
        type = ref.type,
        date = date,
        maintenanceItem = maintenanceItem,
        title = title.takeIf { ref.type != TimelineEventType.Fuel && maintenanceItem == null },
        mileage = mileage,
        costWon = costWon.takeIf { includeCost },
    )

    private companion object {
        const val MAX_SHARED_RECORDS = 5
    }
}
