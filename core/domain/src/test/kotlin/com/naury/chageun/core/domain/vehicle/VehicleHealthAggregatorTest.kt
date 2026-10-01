package com.naury.chageun.core.domain.vehicle

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.InspectionState
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.VehicleHealthLevel
import org.junit.Test

class VehicleHealthAggregatorTest {

    private val aggregator = VehicleHealthAggregator()

    private fun status(item: MaintenanceItem, state: MaintenanceState) = MaintenanceStatus(
        item = item,
        state = state,
        remainingKm = null,
        remainingDays = null,
        distanceDue = null,
        dateDue = null,
        estimatedDue = null,
        missingInputs = emptySet(),
        ruleSource = RuleSource.Generic,
    )

    private fun aggregate(
        vararg items: MaintenanceStatus,
        recall: Boolean = false,
        inspection: InspectionState = InspectionState.Ok,
    ) = aggregator.aggregate(VehicleHealthInput(items.toList(), recall, inspection))

    @Test
    fun returnsGood_whenEverythingIsFine() {
        val health = aggregate(status(MaintenanceItem.EngineOil, MaintenanceState.Good))

        assertThat(health.level).isEqualTo(VehicleHealthLevel.Good)
        assertThat(health.reasons).isEmpty()
    }

    @Test
    fun prioritizesSafetyRecall_overEverything() {
        val health = aggregate(
            status(MaintenanceItem.EngineOil, MaintenanceState.Overdue),
            status(MaintenanceItem.Tire, MaintenanceState.Overdue),
            recall = true,
            inspection = InspectionState.Overdue,
        )

        assertThat(health.level).isEqualTo(VehicleHealthLevel.NeedsAttention)
        assertThat(health.reasons).containsExactly(
            HealthReason.ActiveSafetyRecall,
            HealthReason.InspectionOverdue,
            HealthReason.SafetyItemOverdue(MaintenanceItem.Tire),
            HealthReason.ItemOverdue(MaintenanceItem.EngineOil),
        ).inOrder()
    }

    @Test
    fun returnsUpcoming_forDueSoonItems() {
        val health = aggregate(
            status(MaintenanceItem.EngineOil, MaintenanceState.Due),
            status(MaintenanceItem.Tire, MaintenanceState.Good),
        )

        assertThat(health.level).isEqualTo(VehicleHealthLevel.Upcoming)
    }

    @Test
    fun returnsInsufficientData_whenSafetyItemUnknown() {
        val health = aggregate(
            status(MaintenanceItem.EngineOil, MaintenanceState.Good),
            status(MaintenanceItem.BrakePad, MaintenanceState.Unknown),
        )

        assertThat(health.level).isEqualTo(VehicleHealthLevel.InsufficientData)
    }

    @Test
    fun staysGood_whenOnlyNonSafetyItemUnknownAndOthersEvaluated() {
        val health = aggregate(
            status(MaintenanceItem.EngineOil, MaintenanceState.Good),
            status(MaintenanceItem.Wiper, MaintenanceState.Unknown),
        )

        assertThat(health.level).isEqualTo(VehicleHealthLevel.Good)
        assertThat(health.reasons).containsExactly(HealthReason.ItemUnknown(MaintenanceItem.Wiper))
    }

    @Test
    fun neverReturnsGood_withoutAnyEvaluatedItem() {
        assertThat(aggregate().level).isEqualTo(VehicleHealthLevel.InsufficientData)
        assertThat(aggregate(status(MaintenanceItem.Wiper, MaintenanceState.Unknown)).level)
            .isEqualTo(VehicleHealthLevel.InsufficientData)
    }

    @Test
    fun overdueOutranksUnknownSafetyItem() {
        val health = aggregate(
            status(MaintenanceItem.EngineOil, MaintenanceState.Overdue),
            status(MaintenanceItem.Tire, MaintenanceState.Unknown),
        )

        assertThat(health.level).isEqualTo(VehicleHealthLevel.NeedsAttention)
    }
}
