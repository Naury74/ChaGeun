package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.reminder.InspectionReminderStage
import com.naury.chageun.core.domain.vehicle.InspectionRepository
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.VehicleId
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeInspectionRepository(schedule: InspectionSchedule? = null) : InspectionRepository {
    val schedule = MutableStateFlow(schedule)
    var notified: InspectionReminderStage? = null

    override fun observeSchedule(vehicleId: VehicleId): Flow<InspectionSchedule?> = schedule

    override suspend fun setUserDueDate(vehicleId: VehicleId, date: LocalDate?) {
        schedule.value = date?.let { InspectionSchedule(it, InspectionSource.User) }
        notified = null
    }

    override suspend fun notifiedStage(vehicleId: VehicleId) = notified

    override suspend fun markNotified(vehicleId: VehicleId, stage: InspectionReminderStage) {
        notified = stage
    }
}
