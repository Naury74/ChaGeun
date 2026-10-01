package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.domain.reminder.InspectionReminderStage
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.VehicleId
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface InspectionRepository {
    fun observeSchedule(vehicleId: VehicleId): Flow<InspectionSchedule?>

    /** Stores a user-entered due date, or clears it when [date] is null. A new date restarts its reminders. */
    suspend fun setUserDueDate(vehicleId: VehicleId, date: LocalDate?)

    suspend fun notifiedStage(vehicleId: VehicleId): InspectionReminderStage?

    suspend fun markNotified(vehicleId: VehicleId, stage: InspectionReminderStage)
}
