package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.domain.vehicle.InspectionState
import com.naury.chageun.core.domain.vehicle.VehicleHealthAggregator
import com.naury.chageun.core.domain.vehicle.VehicleHealthInput
import com.naury.chageun.core.model.MaintenanceOverview
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveMaintenanceOverviewUseCase @Inject constructor(
    private val repository: MaintenanceRepository,
    private val engine: MaintenanceEngine,
    private val healthAggregator: VehicleHealthAggregator,
    private val clock: Clock,
) {
    operator fun invoke(vehicleId: VehicleId): Flow<MaintenanceOverview> =
        repository.observeInputs(vehicleId).map { inputs ->
            val today = LocalDate.now(clock)
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
                // TODO(CHGN-33): Feed inspection and recall state once official data is connected.
                health = healthAggregator.aggregate(
                    VehicleHealthInput(statuses, hasActiveSafetyRecall = false, inspection = InspectionState.Unknown),
                ),
                currentMileage = inputs.mileageHistory.currentAsOf(today),
            )
        }

    private companion object {
        val URGENCY_ORDER: Comparator<MaintenanceStatus> = compareBy<MaintenanceStatus> { it.state.ordinal }
            .thenByDescending { it.item.isSafetyCritical }
            .thenBy(nullsLast()) { it.estimatedDue?.date }
            .thenBy { it.item.ordinal }
    }
}
