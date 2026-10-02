package com.naury.chageun.core.domain.vehicle

import com.naury.chageun.core.domain.reminder.InspectionReminderStage
import com.naury.chageun.core.model.InspectionSchedule
import com.naury.chageun.core.model.VehicleId
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface InspectionRepository {
    fun observeSchedule(vehicleId: VehicleId): Flow<InspectionSchedule?>

    /** 사용자가 입력한 만료일을 저장하고, [date]가 null이면 지운다. 날짜가 바뀌면 알림을 처음부터 다시 시작한다. */
    suspend fun setUserDueDate(vehicleId: VehicleId, date: LocalDate?)

    suspend fun notifiedStage(vehicleId: VehicleId): InspectionReminderStage?

    suspend fun markNotified(vehicleId: VehicleId, stage: InspectionReminderStage)
}
