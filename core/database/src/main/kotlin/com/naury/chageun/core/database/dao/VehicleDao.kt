package com.naury.chageun.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.naury.chageun.core.database.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicle WHERE is_primary = 1 LIMIT 1")
    fun observePrimary(): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicle WHERE id = :id")
    fun observe(id: String): Flow<VehicleEntity?>

    @Upsert
    suspend fun upsert(vehicle: VehicleEntity)

    @Query("UPDATE vehicle SET is_primary = 0 WHERE is_primary = 1")
    suspend fun clearPrimary()

    @Query("DELETE FROM vehicle WHERE id = :id")
    suspend fun delete(id: String)
}
