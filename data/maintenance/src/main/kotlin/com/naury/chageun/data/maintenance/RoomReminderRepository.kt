package com.naury.chageun.data.maintenance

import com.naury.chageun.core.database.dao.ReminderDao
import com.naury.chageun.core.database.entity.ReminderStateEntity
import com.naury.chageun.core.domain.reminder.ReminderRepository
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import javax.inject.Inject

internal class RoomReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val clock: Clock,
) : ReminderRepository {

    override suspend fun notifiedStates(vehicleId: VehicleId): Map<MaintenanceItem, MaintenanceState> =
        reminderDao.findAll(vehicleId.value).mapNotNull { row ->
            val item = row.itemType.toMaintenanceItemOrNull() ?: return@mapNotNull null
            val state = MaintenanceState.entries.firstOrNull { it.name == row.notifiedState } ?: return@mapNotNull null
            item to state
        }.toMap()

    override suspend fun replaceNotifiedStates(vehicleId: VehicleId, states: Map<MaintenanceItem, MaintenanceState>) {
        val now = clock.instant()
        reminderDao.replaceAll(
            vehicleId.value,
            states.map { (item, state) -> ReminderStateEntity(vehicleId.value, item.name, state.name, now) },
        )
    }
}
