package com.naury.chageun.data.maintenance

import com.naury.chageun.core.database.dao.ReminderDao
import com.naury.chageun.core.database.entity.ReminderStateEntity
import com.naury.chageun.core.domain.reminder.MaintenanceReminderStage
import com.naury.chageun.core.domain.reminder.ReminderRepository
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import javax.inject.Inject

internal class RoomReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val clock: Clock,
) : ReminderRepository {

    override suspend fun notifiedStages(vehicleId: VehicleId): Map<MaintenanceItem, MaintenanceReminderStage> =
        reminderDao.findAll(vehicleId.value).mapNotNull { row ->
            val item = row.itemType.toMaintenanceItemOrNull() ?: return@mapNotNull null
            val stage = row.notifiedState.toStageOrNull() ?: return@mapNotNull null
            item to stage
        }.toMap()

    override suspend fun replaceNotifiedStages(
        vehicleId: VehicleId,
        stages: Map<MaintenanceItem, MaintenanceReminderStage>,
    ) {
        val now = clock.instant()
        reminderDao.replaceAll(
            vehicleId.value,
            stages.map { (item, stage) -> ReminderStateEntity(vehicleId.value, item.name, stage.name, now) },
        )
    }

    /**
     * 예전에는 화면 상태(Upcoming·Due·Overdue)를 저장했다. 이미 알린 단계를 다시 알리지 않도록 가장 가까운 단계로 읽는다.
     * 다가옴은 첫 단계, 교체 시기와 지남은 도래 단계로 본다.
     */
    private fun String.toStageOrNull(): MaintenanceReminderStage? =
        MaintenanceReminderStage.entries.firstOrNull { it.name == this } ?: when (this) {
            LEGACY_UPCOMING -> MaintenanceReminderStage.Early
            LEGACY_DUE, LEGACY_OVERDUE -> MaintenanceReminderStage.Due
            else -> null
        }

    private companion object {
        const val LEGACY_UPCOMING = "Upcoming"
        const val LEGACY_DUE = "Due"
        const val LEGACY_OVERDUE = "Overdue"
    }
}
