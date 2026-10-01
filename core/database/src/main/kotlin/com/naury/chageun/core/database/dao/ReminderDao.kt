package com.naury.chageun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.naury.chageun.core.database.entity.ReminderStateEntity

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_state WHERE vehicle_id = :vehicleId")
    suspend fun findAll(vehicleId: String): List<ReminderStateEntity>

    @Query("DELETE FROM reminder_state WHERE vehicle_id = :vehicleId")
    suspend fun deleteAll(vehicleId: String)

    @Insert
    suspend fun insertAll(states: List<ReminderStateEntity>)

    @Transaction
    suspend fun replaceAll(vehicleId: String, states: List<ReminderStateEntity>) {
        deleteAll(vehicleId)
        insertAll(states)
    }
}
