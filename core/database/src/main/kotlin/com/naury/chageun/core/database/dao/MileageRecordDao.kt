package com.naury.chageun.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.naury.chageun.core.database.entity.MileageRecordEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface MileageRecordDao {
    @Query(
        """
        SELECT * FROM mileage_record
        WHERE vehicle_id = :vehicleId AND recorded_on >= :since
        ORDER BY recorded_on ASC, created_at ASC
        """,
    )
    fun observeSince(vehicleId: String, since: LocalDate): Flow<List<MileageRecordEntity>>

    @Query("SELECT * FROM mileage_record WHERE vehicle_id = :vehicleId ORDER BY recorded_on ASC, created_at ASC")
    fun observeAll(vehicleId: String): Flow<List<MileageRecordEntity>>

    @Query(
        """
        SELECT * FROM mileage_record
        WHERE vehicle_id = :vehicleId
        ORDER BY recorded_on DESC, created_at DESC
        LIMIT 1
        """,
    )
    fun observeLatest(vehicleId: String): Flow<MileageRecordEntity?>

    @Insert
    suspend fun insert(record: MileageRecordEntity)
}
