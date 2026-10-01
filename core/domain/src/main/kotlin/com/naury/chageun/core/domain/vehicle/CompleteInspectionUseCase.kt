package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.domain.history.AddHistoryRecordUseCase
import com.naury.chageun.core.domain.history.HistoryEntryError
import com.naury.chageun.core.domain.reminder.ReminderNotifier
import com.naury.chageun.core.model.CheckEntry
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.VehicleId
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.absoluteValue

class CompleteInspectionUseCase @Inject constructor(
    private val addHistoryRecord: AddHistoryRecordUseCase,
    private val inspectionRepository: InspectionRepository,
    private val notifier: ReminderNotifier,
) {
    /**
     * Records the inspection in the timeline and stores [nextDueDate] (null clears it).
     * The date is only changed when the record is valid, so a rejected entry leaves the schedule as it was.
     */
    suspend operator fun invoke(
        vehicleId: VehicleId,
        completedOn: LocalDate,
        title: String,
        mileage: Kilometers?,
        nextDueDate: LocalDate?,
    ): Set<HistoryEntryError> {
        val errors = addHistoryRecord.addCheck(
            vehicleId,
            CheckEntry(kind = CheckKind.Inspection, date = completedOn, title = title, mileage = mileage),
        )
        if (errors.isNotEmpty()) return errors
        inspectionRepository.setUserDueDate(vehicleId, nextDueDate)
        notifier.cancelInspection()
        return errors
    }

    companion object {
        /** Private passenger cars are inspected every two years after the first one. */
        private const val INTERVAL_YEARS = 2L

        /** An inspection taken within this many days of the due date keeps the original cycle. */
        private const val ON_TIME_WINDOW_DAYS = 31L

        /**
         * A starting point the user confirms against their notice: on-time inspections extend the previous
         * due date, late or early ones count from the inspection day.
         */
        fun suggestNextDueDate(completedOn: LocalDate, previousDueDate: LocalDate?): LocalDate {
            val onTime = previousDueDate != null &&
                ChronoUnit.DAYS.between(previousDueDate, completedOn).absoluteValue <= ON_TIME_WINDOW_DAYS
            val base = if (onTime) previousDueDate else completedOn
            return base.plusYears(INTERVAL_YEARS)
        }
    }
}
