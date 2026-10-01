package com.naury.chageun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.naury.chageun.core.database.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query(
        """
        SELECT * FROM attachment
        WHERE vehicle_id = :vehicleId AND owner_type = :ownerType AND owner_id = :ownerId
        ORDER BY created_at ASC
        """,
    )
    fun observe(vehicleId: String, ownerType: String, ownerId: String): Flow<List<AttachmentEntity>>

    @Query(
        "SELECT * FROM attachment WHERE vehicle_id = :vehicleId AND owner_type = :ownerType AND owner_id = :ownerId",
    )
    suspend fun findFor(vehicleId: String, ownerType: String, ownerId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachment WHERE vehicle_id = :vehicleId AND id = :id")
    suspend fun find(vehicleId: String, id: String): AttachmentEntity?

    @Query(
        """
        SELECT COUNT(*) FROM attachment
        WHERE vehicle_id = :vehicleId AND owner_type = :ownerType AND owner_id = :ownerId
        """,
    )
    suspend fun count(vehicleId: String, ownerType: String, ownerId: String): Int

    @Insert
    suspend fun insert(attachment: AttachmentEntity)

    @Query("DELETE FROM attachment WHERE vehicle_id = :vehicleId AND id = :id")
    suspend fun delete(vehicleId: String, id: String)

    @Query(
        "DELETE FROM attachment WHERE vehicle_id = :vehicleId AND owner_type = :ownerType AND owner_id = :ownerId",
    )
    suspend fun deleteFor(vehicleId: String, ownerType: String, ownerId: String)
}
