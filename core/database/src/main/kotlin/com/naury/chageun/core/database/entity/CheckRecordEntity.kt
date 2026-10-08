package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "check_record",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicle_id", "check_date")],
)
data class CheckRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    val kind: String,
    @ColumnInfo(name = "check_date") val checkDate: LocalDate,
    val title: String,
    @ColumnInfo(name = "mileage_km") val mileageKm: Long?,
    @ColumnInfo(name = "cost_won") val costWon: Long?,
    val memo: String?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    /** 정기검사 결과. 일반 점검이면 null이다. */
    @ColumnInfo(name = "periodic_result") val periodicResult: String? = null,
)
