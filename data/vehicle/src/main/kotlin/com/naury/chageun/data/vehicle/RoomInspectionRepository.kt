package com.naury.chageun.data.vehicle

import com.naury.chageun.core.database.dao.InspectionDao
import com.naury.chageun.core.database.entity.InspectionScheduleEntity
import com.naury.chageun.core.domain.reminder.InspectionReminderStage
import com.naury.chageun.core.domain.vehicle.InspectionRepository
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomInspectionRepository @Inject constructor(private val dao: InspectionDao, private val clock: Clock) :
    InspectionRepository {

    override fun observeSchedule(vehicleId: VehicleId): Flow<InspectionSchedule?> =
        dao.observe(vehicleId.value).map { row ->
            row?.let { InspectionSchedule(it.nextDueDate, InspectionSource.valueOf(it.source)) }
        }

    override suspend fun setUserDueDate(vehicleId: VehicleId, date: LocalDate?) {
        if (date == null) {
            dao.delete(vehicleId.value)
            return
        }
        val existing = dao.find(vehicleId.value)
        val unchanged = existing?.nextDueDate == date && existing.source == InspectionSource.User.name
        dao.upsert(
            InspectionScheduleEntity(
                vehicleId = vehicleId.value,
                nextDueDate = date,
                source = InspectionSource.User.name,
                notifiedStage = existing?.notifiedStage.takeIf { unchanged },
                updatedAt = clock.instant(),
            ),
        )
    }

    override suspend fun notifiedStage(vehicleId: VehicleId): InspectionReminderStage? =
        dao.find(vehicleId.value)?.notifiedStage
            ?.let { name -> InspectionReminderStage.entries.firstOrNull { it.name == name } }

    override suspend fun markNotified(vehicleId: VehicleId, stage: InspectionReminderStage) =
        dao.updateNotifiedStage(vehicleId.value, stage.name)
}
