package com.naury.chageun.core.domain.maintenance

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.Confidence
import com.naury.chageun.core.model.DueEstimate
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.MissingInput
import com.naury.chageun.core.model.ServiceRecord
import java.time.LocalDate
import org.junit.Test

class RuleBasedMaintenanceEngineTest {

    private val engine = RuleBasedMaintenanceEngine(DrivingPaceEstimator())
    private val today = LocalDate.of(2026, 10, 1)
    private val engineOil = MaintenanceRule(MaintenanceItem.EngineOil, intervalKm = 10_000, intervalMonths = 12)

    private fun evaluate(
        rule: MaintenanceRule = engineOil,
        lastService: ServiceRecord? = ServiceRecord(today.minusMonths(3), Kilometers(40_000)),
        currentKm: Long? = 45_000,
        history: List<MileageReading> = listOfNotNull(currentKm?.let { MileageReading(today, Kilometers(it)) }),
    ) = engine.evaluate(MaintenanceEvaluationInput(rule, lastService, history, today))

    @Test
    fun estimatesDueDate_fromLifetimePace_whenOnlyOneReadingExists() {
        // 2024-01-01부터 1,004일에 45,000 km → 하루 약 44.8 km, 남은 5,000 km는 112일
        val status = engine.evaluate(
            MaintenanceEvaluationInput(
                engineOil,
                ServiceRecord(today.minusMonths(3), Kilometers(40_000)),
                listOf(MileageReading(today, Kilometers(45_000))),
                today,
                modelYear = 2024,
            ),
        )

        assertThat(status.estimatedDue).isEqualTo(DueEstimate(today.plusDays(112), Confidence.Low))
    }

    @Test
    fun returnsGood_whenBothDimensionsAreFarFromDue() {
        val status = evaluate()

        assertThat(status.state).isEqualTo(MaintenanceState.Good)
        assertThat(status.remainingKm).isEqualTo(5_000)
        assertThat(status.distanceDue).isEqualTo(Kilometers(50_000))
        assertThat(status.dateDue).isEqualTo(today.plusMonths(9))
    }

    @Test
    fun returnsOverdue_whenMileageReachesLimitExactly() {
        assertThat(evaluate(currentKm = 50_000).state).isEqualTo(MaintenanceState.Overdue)
    }

    @Test
    fun returnsOverdue_whenMileageExceedsLimit() {
        val status = evaluate(currentKm = 50_300)

        assertThat(status.state).isEqualTo(MaintenanceState.Overdue)
        assertThat(status.remainingKm).isEqualTo(-300)
    }

    @Test
    fun returnsDue_atDueSoonKmBoundary() {
        assertThat(evaluate(currentKm = 49_500).state).isEqualTo(MaintenanceState.Due)
        assertThat(evaluate(currentKm = 49_499).state).isEqualTo(MaintenanceState.Upcoming)
    }

    @Test
    fun returnsUpcoming_atUpcomingKmBoundary() {
        assertThat(evaluate(currentKm = 48_000).state).isEqualTo(MaintenanceState.Upcoming)
        assertThat(evaluate(currentKm = 47_999).state).isEqualTo(MaintenanceState.Good)
    }

    @Test
    fun returnsOverdue_onDateDueDay_evenWhenDistanceIsFine() {
        val status = evaluate(lastService = ServiceRecord(today.minusMonths(12), Kilometers(44_000)))

        assertThat(status.remainingDays).isEqualTo(0)
        assertThat(status.state).isEqualTo(MaintenanceState.Overdue)
    }

    @Test
    fun appliesEarliestThreshold_whenDateIsDueSoonButDistanceIsGood() {
        val status = evaluate(lastService = ServiceRecord(today.minusMonths(12).plusDays(10), Kilometers(44_000)))

        assertThat(status.state).isEqualTo(MaintenanceState.Due)
    }

    @Test
    fun returnsUnknown_withoutServiceHistory() {
        val status = evaluate(lastService = null)

        assertThat(status.state).isEqualTo(MaintenanceState.Unknown)
        assertThat(status.missingInputs).contains(MissingInput.LastService)
    }

    @Test
    fun returnsUnknown_whenCurrentMileageMissingAndDateLooksGood() {
        val status = evaluate(currentKm = null)

        assertThat(status.state).isEqualTo(MaintenanceState.Unknown)
        assertThat(status.missingInputs).containsExactly(MissingInput.CurrentMileage)
    }

    @Test
    fun keepsOverdue_whenDateIsOverdueEvenWithoutMileage() {
        val status = evaluate(
            lastService = ServiceRecord(today.minusMonths(14), mileage = null),
            currentKm = null,
        )

        assertThat(status.state).isEqualTo(MaintenanceState.Overdue)
        assertThat(status.missingInputs).contains(MissingInput.LastServiceMileage)
    }

    @Test
    fun returnsUnknown_whenServiceDateMissingAndDistanceLooksGood() {
        val status = evaluate(lastService = ServiceRecord(date = null, mileage = Kilometers(40_000)))

        assertThat(status.state).isEqualTo(MaintenanceState.Unknown)
        assertThat(status.remainingKm).isEqualTo(5_000)
        assertThat(status.dateDue).isNull()
        assertThat(status.missingInputs).containsExactly(MissingInput.LastServiceDate)
    }

    @Test
    fun keepsOverdue_whenServiceDateMissingButDistanceExceeded() {
        val status = evaluate(lastService = ServiceRecord(date = null, mileage = Kilometers(30_000)))

        assertThat(status.state).isEqualTo(MaintenanceState.Overdue)
    }

    @Test
    fun evaluatesDistanceOnlyRules_withoutServiceDate() {
        val rule = MaintenanceRule(MaintenanceItem.Tire, intervalKm = 50_000, intervalMonths = null)

        val status = evaluate(rule = rule, lastService = ServiceRecord(date = null, mileage = Kilometers(20_000)))

        assertThat(status.state).isEqualTo(MaintenanceState.Good)
        assertThat(status.missingInputs).isEmpty()
    }

    @Test
    fun evaluatesDateOnlyRules_withoutMileage() {
        val wiper = MaintenanceRule(MaintenanceItem.Wiper, intervalKm = null, intervalMonths = 12)

        val status = evaluate(rule = wiper, lastService = ServiceRecord(today.minusMonths(2), null), currentKm = null)

        assertThat(status.state).isEqualTo(MaintenanceState.Good)
        assertThat(status.remainingKm).isNull()
    }

    @Test
    fun ignoresFutureMileageReadings() {
        val history = listOf(
            MileageReading(today, Kilometers(45_000)),
            MileageReading(today.plusDays(3), Kilometers(52_000)),
        )

        assertThat(evaluate(history = history).state).isEqualTo(MaintenanceState.Good)
    }

    @Test
    fun estimatesDueDate_fromDrivingPace() {
        val history = listOf(
            MileageReading(today.minusDays(100), Kilometers(41_000)),
            MileageReading(today, Kilometers(45_000)),
        )

        val estimate = evaluate(history = history).estimatedDue

        assertThat(estimate?.date).isEqualTo(today.plusDays(125))
        assertThat(estimate?.confidence).isEqualTo(Confidence.High)
    }

    @Test
    fun usesScheduledDate_whenItComesBeforeDistancePrediction() {
        val history = listOf(
            MileageReading(today.minusDays(100), Kilometers(44_900)),
            MileageReading(today, Kilometers(45_000)),
        )

        assertThat(evaluate(history = history).estimatedDue?.date).isEqualTo(today.plusMonths(9))
    }
}
