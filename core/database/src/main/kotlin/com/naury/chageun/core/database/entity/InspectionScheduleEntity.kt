package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/** Next periodic inspection per vehicle. [notifiedStage] is reset whenever the due date changes. */
@Entity(
    tableName = "inspection_schedule",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class InspectionScheduleEntity(
    @PrimaryKey @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "next_due_date") val nextDueDate: LocalDate,
    val source: String,
    @ColumnInfo(name = "notified_stage") val notifiedStage: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
)
