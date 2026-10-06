package com.naury.chageun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.naury.chageun.core.database.entity.AlbumPhotoEntity
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    /** 찍은 날짜 최신순. 같은 날은 나중에 넣은 사진이 앞에 온다. */
    @Query("SELECT * FROM album_photo WHERE vehicle_id = :vehicleId ORDER BY taken_on DESC, created_at DESC")
    fun observe(vehicleId: String): Flow<List<AlbumPhotoEntity>>

    @Query("SELECT * FROM album_photo WHERE vehicle_id = :vehicleId AND id = :id")
    suspend fun find(vehicleId: String, id: String): AlbumPhotoEntity?

    @Insert
    suspend fun insert(photo: AlbumPhotoEntity)

    @Query(
        """
        UPDATE album_photo SET taken_on = :takenOn, comment = :comment, updated_at = :updatedAt
        WHERE vehicle_id = :vehicleId AND id = :id
        """,
    )
    suspend fun updateDetails(vehicleId: String, id: String, takenOn: LocalDate, comment: String?, updatedAt: Instant)

    @Query("DELETE FROM album_photo WHERE vehicle_id = :vehicleId AND id = :id")
    suspend fun delete(vehicleId: String, id: String)
}
