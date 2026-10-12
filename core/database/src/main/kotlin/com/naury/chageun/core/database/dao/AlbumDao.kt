package com.naury.chageun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.naury.chageun.core.database.entity.AlbumPhotoEntity
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    /** 찍은 날짜 최신순. 같은 날은 나중에 넣은 사진이 앞에 온다. */
    @Query("SELECT * FROM album_photo WHERE vehicle_id = :vehicleId ORDER BY taken_on DESC, created_at DESC")
    fun observe(vehicleId: String): Flow<List<AlbumPhotoEntity>>

    /** 찍은 날짜가 [from]~[to](양 끝 포함)인 사진. (vehicle_id, taken_on) 인덱스로 범위를 읽는다. */
    @Query(
        """
        SELECT * FROM album_photo WHERE vehicle_id = :vehicleId AND taken_on BETWEEN :from AND :to
        ORDER BY taken_on ASC, created_at ASC
        """,
    )
    fun observeBetween(vehicleId: String, from: LocalDate, to: LocalDate): Flow<List<AlbumPhotoEntity>>

    @Query("SELECT * FROM album_photo WHERE vehicle_id = :vehicleId AND id = :id")
    suspend fun find(vehicleId: String, id: String): AlbumPhotoEntity?

    @Insert
    suspend fun insert(photo: AlbumPhotoEntity)

    /** 편집한 사진·동영상으로 파일만 바꿀 때 쓴다. id·날짜·메모·만든 시각은 그대로 넘긴다. */
    @Update
    suspend fun update(photo: AlbumPhotoEntity)

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
