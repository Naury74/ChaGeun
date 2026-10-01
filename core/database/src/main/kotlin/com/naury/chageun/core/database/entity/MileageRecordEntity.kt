package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "mileage_record",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vehicle_id", "recorded_on")],
)
data class MileageRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "mileage_km") val mileageKm: Long,
    @ColumnInfo(name = "recorded_on") val recordedOn: LocalDate,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "related_record_id") val relatedRecordId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
