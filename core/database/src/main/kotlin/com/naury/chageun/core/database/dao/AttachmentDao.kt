package com.naury.chageun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.naury.chageun.core.database.entity.AttachmentEntity
import com.naury.chageun.core.database.entity.DatedAttachmentRow
import java.time.LocalDate
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

    /**
     * 기록 날짜가 [from]~[to](양 끝 포함)인 기록의 첨부. 첨부는 소유자 FK가 없는 다형 구조라 기록 테이블들을
     * 모아 owner_type·owner_id로 잇는다. 날짜 없는 정비 기록의 첨부는 빠진다. 기록 날짜, 첨부한 순서로 정렬한다.
     */
    @Query(
        """
        SELECT a.*, r.occurred_on FROM attachment a
        INNER JOIN (
            SELECT 'Maintenance' AS owner_type, id, service_date AS occurred_on
            FROM maintenance_record WHERE vehicle_id = :vehicleId
            UNION ALL
            SELECT 'Fuel', id, fuel_date FROM fuel_record WHERE vehicle_id = :vehicleId
            UNION ALL
            SELECT kind, id, check_date FROM check_record WHERE vehicle_id = :vehicleId
        ) AS r ON a.owner_type = r.owner_type AND a.owner_id = r.id
        WHERE a.vehicle_id = :vehicleId AND r.occurred_on BETWEEN :from AND :to
        ORDER BY r.occurred_on ASC, a.created_at ASC
        """,
    )
    fun observeBetween(vehicleId: String, from: LocalDate, to: LocalDate): Flow<List<DatedAttachmentRow>>

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

    /** 편집한 사진으로 파일만 바꿀 때 쓴다. id·소유자·만든 시각은 그대로 둔다. */
    @Update
    suspend fun update(attachment: AttachmentEntity)

    @Query("DELETE FROM attachment WHERE vehicle_id = :vehicleId AND id = :id")
    suspend fun delete(vehicleId: String, id: String)

    @Query(
        "DELETE FROM attachment WHERE vehicle_id = :vehicleId AND owner_type = :ownerType AND owner_id = :ownerId",
    )
    suspend fun deleteFor(vehicleId: String, ownerType: String, ownerId: String)
}
