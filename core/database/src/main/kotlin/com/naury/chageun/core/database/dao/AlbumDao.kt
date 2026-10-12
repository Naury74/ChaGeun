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
    /** 찍은 날짜 최신순. 같은 날은 나중에 넣은 사진이 앞에 온다. 지우고 되돌리기를 기다리는 사진은 빠진다. */
    @Query(
        """
        SELECT * FROM album_photo WHERE vehicle_id = :vehicleId AND deleted_at IS NULL
        ORDER BY taken_on DESC, created_at DESC
        """,
    )
    fun observe(vehicleId: String): Flow<List<AlbumPhotoEntity>>

    /** 찍은 날짜가 [from]~[to](양 끝 포함)인 사진. (vehicle_id, taken_on) 인덱스로 범위를 읽는다. */
    @Query(
        """
        SELECT * FROM album_photo
        WHERE vehicle_id = :vehicleId AND taken_on BETWEEN :from AND :to AND deleted_at IS NULL
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

    /**
     * 지운 시각을 적어 목록에서 뺀다. 행과 파일은 [deleteRows]로 나중에 지운다. [deletedAt]이 null이면 되돌린다.
     */
    @Query("UPDATE album_photo SET deleted_at = :deletedAt WHERE vehicle_id = :vehicleId AND id IN (:ids)")
    suspend fun setDeletedAt(vehicleId: String, ids: List<String>, deletedAt: Instant?)

    /** 지운 표시가 붙어 되돌리기를 기다리는 행. 되돌린 사진은 빠진다. 많아야 한 번에 지운 수만큼이라 모두 읽는다. */
    @Query("SELECT * FROM album_photo WHERE deleted_at IS NOT NULL")
    suspend fun deletedRows(): List<AlbumPhotoEntity>

    @Query("DELETE FROM album_photo WHERE id IN (:ids) AND deleted_at IS NOT NULL")
    suspend fun deleteRows(ids: List<String>)
}
