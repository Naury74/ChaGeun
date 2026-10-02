package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.domain.vehicle.InspectionEvaluator
import com.naury.chageun.core.domain.vehicle.InspectionRepository
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.domain.vehicle.VehicleHealthInput
import com.naury.chageun.core.model.MaintenanceOverview
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveMaintenanceOverviewUseCase @Inject constructor(
    private val repository: MaintenanceRepository,
    private val inspectionRepository: InspectionRepository,
    private val engine: MaintenanceEngine,
    private val healthAggregator: VehicleHealthAggregator,
    private val clock: Clock,
) {
    operator fun invoke(vehicleId: VehicleId): Flow<MaintenanceOverview> = combine(
        repository.observeInputs(vehicleId),
        inspectionRepository.observeSchedule(vehicleId),
    ) { inputs, inspectionSchedule ->
        val today = LocalDate.now(clock)
        val inspection = InspectionEvaluator.evaluate(inspectionSchedule, today)
        val statuses = inputs.rules
            .filter { it.isEnabled }
            .map { rule ->
                engine.evaluate(
                    MaintenanceEvaluationInput(
                        rule = rule,
                        lastService = inputs.lastServices[rule.item],
                        mileageHistory = inputs.mileageHistory,
                        today = today,
                    ),
                )
            }
            .sortedWith(URGENCY_ORDER)

        MaintenanceOverview(
            statuses = statuses,
            rules = inputs.rules.associateBy { it.item },
            disabledItems = inputs.rules.filterNot { it.isEnabled }.map { it.item }.sorted(),
            // TODO(CHGN-33): 공식 데이터가 연결되면 리콜 상태를 넘긴다.
            health = healthAggregator.aggregate(
                VehicleHealthInput(statuses, hasActiveSafetyRecall = false, inspection = inspection.state),
            ),
            currentMileage = inputs.mileageHistory.currentAsOf(today),
            inspection = inspection,
        )
    }

    private companion object {
        val URGENCY_ORDER: Comparator<MaintenanceStatus> = compareBy<MaintenanceStatus> { it.state.ordinal }
            .thenByDescending { it.item.isSafetyCritical }
            .thenBy(nullsLast()) { it.estimatedDue?.date }
            .thenBy { it.item.ordinal }
    }
}
