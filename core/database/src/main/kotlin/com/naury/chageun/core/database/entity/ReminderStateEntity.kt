package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import java.time.Instant

/** The most urgent stage already notified for an item, so a daily evaluation never repeats a notification. */
@Entity(
    tableName = "reminder_state",
    primaryKeys = ["vehicle_id", "item_type"],
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReminderStateEntity(
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    @ColumnInfo(name = "item_type") val itemType: String,
    @ColumnInfo(name = "notified_state") val notifiedState: String,
    @ColumnInfo(name = "notified_at") val notifiedAt: Instant,
)
