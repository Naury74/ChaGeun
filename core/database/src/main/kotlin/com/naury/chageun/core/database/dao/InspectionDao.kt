package com.naury.chageun.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.naury.chageun.core.database.entity.CheckRecordEntity
import com.naury.chageun.core.database.entity.InspectionScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {
    @Query("SELECT * FROM inspection_schedule WHERE vehicle_id = :vehicleId")
    fun observe(vehicleId: String): Flow<InspectionScheduleEntity?>

    @Query("SELECT * FROM inspection_schedule WHERE vehicle_id = :vehicleId")
    suspend fun find(vehicleId: String): InspectionScheduleEntity?

    @Upsert
    suspend fun upsert(schedule: InspectionScheduleEntity)

    @Query("DELETE FROM inspection_schedule WHERE vehicle_id = :vehicleId")
    suspend fun delete(vehicleId: String)

    @Query("UPDATE inspection_schedule SET notified_stage = :stage WHERE vehicle_id = :vehicleId")
    suspend fun updateNotifiedStage(vehicleId: String, stage: String)

    @Query(
        """
        SELECT * FROM check_record
        WHERE vehicle_id = :vehicleId AND periodic_result IS NOT NULL
        ORDER BY check_date DESC, created_at DESC
        """,
    )
    fun observePeriodicChecks(vehicleId: String): Flow<List<CheckRecordEntity>>
}
