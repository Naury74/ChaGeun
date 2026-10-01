package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.model.Confidence
import com.naury.chageun.core.model.DueEstimate
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MissingInput
import com.naury.chageun.core.model.ServiceRecord
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.ceil

class RuleBasedMaintenanceEngine @Inject constructor(private val paceEstimator: DrivingPaceEstimator) :
    MaintenanceEngine {

    override fun evaluate(input: MaintenanceEvaluationInput): MaintenanceStatus {
        val rule = input.rule
        val missing = mutableSetOf<MissingInput>()
        if (input.lastService == null) missing += MissingInput.LastService

        val distance = evaluateDistance(rule, input.lastService, input.currentMileage(), missing)
        val date = evaluateDate(rule, input.lastService, input.today, missing)

        val mostUrgent = listOfNotNull(distance?.state, date?.state).minOrNull()
        val state = when {
            mostUrgent == null -> MaintenanceState.Unknown
            // A configured dimension we could not evaluate may already be overdue, so it must not read as GOOD.
            mostUrgent == MaintenanceState.Good && missing.isNotEmpty() -> MaintenanceState.Unknown
            else -> mostUrgent
        }

        return MaintenanceStatus(
            item = rule.item,
            state = state,
            remainingKm = distance?.remainingKm,
            remainingDays = date?.remainingDays,
            distanceDue = distance?.due,
            dateDue = date?.due,
            estimatedDue = estimateDueDate(distance?.remainingKm, date?.due, input),
            missingInputs = missing,
            ruleSource = rule.source,
        )
    }

    private fun evaluateDistance(
        rule: MaintenanceRule,
        lastService: ServiceRecord?,
        currentMileage: Kilometers?,
        missing: MutableSet<MissingInput>,
    ): DistanceEvaluation? {
        val interval = rule.intervalKm ?: return null
        if (lastService == null) return null
        if (lastService.mileage == null) missing += MissingInput.LastServiceMileage
        if (currentMileage == null) missing += MissingInput.CurrentMileage
        val lastServiceMileage = lastService.mileage ?: return null
        currentMileage ?: return null

        val due = lastServiceMileage + Kilometers(interval)
        val remaining = due distanceFrom currentMileage
        val thresholds = rule.thresholds
        return DistanceEvaluation(due, remaining, stateFor(remaining, thresholds.dueSoonKm, thresholds.upcomingKm))
    }

    private fun evaluateDate(
        rule: MaintenanceRule,
        lastService: ServiceRecord?,
        today: LocalDate,
        missing: MutableSet<MissingInput>,
    ): DateEvaluation? {
        val months = rule.intervalMonths ?: return null
        if (lastService == null) return null
        if (lastService.date == null) missing += MissingInput.LastServiceDate
        val due = lastService.date?.plusMonths(months) ?: return null
        val remaining = ChronoUnit.DAYS.between(today, due)
        val thresholds = rule.thresholds
        return DateEvaluation(due, remaining, stateFor(remaining, thresholds.dueSoonDays, thresholds.upcomingDays))
    }

    private fun estimateDueDate(
        remainingKm: Long?,
        dateDue: LocalDate?,
        input: MaintenanceEvaluationInput,
    ): DueEstimate? {
        val pace = paceEstimator.estimate(input.mileageHistory, input.today)
        val predicted = if (remainingKm != null && pace != null) {
            val days = if (remainingKm <= 0) 0L else ceil(remainingKm / pace.kmPerDay).toLong()
            DueEstimate(input.today.plusDays(days), pace.confidence)
        } else {
            null
        }
        val scheduled = dateDue?.let { DueEstimate(it, Confidence.High) }
        return listOfNotNull(predicted, scheduled).minByOrNull { it.date }
    }

    private fun stateFor(remaining: Long, dueSoon: Long, upcoming: Long): MaintenanceState = when {
        remaining <= 0 -> MaintenanceState.Overdue
        remaining <= dueSoon -> MaintenanceState.Due
        remaining <= upcoming -> MaintenanceState.Upcoming
        else -> MaintenanceState.Good
    }

    private fun MaintenanceEvaluationInput.currentMileage(): Kilometers? = mileageHistory.currentAsOf(today)?.mileage

    private data class DistanceEvaluation(val due: Kilometers, val remainingKm: Long, val state: MaintenanceState)

    private data class DateEvaluation(val due: LocalDate, val remainingDays: Long, val state: MaintenanceState)
}
