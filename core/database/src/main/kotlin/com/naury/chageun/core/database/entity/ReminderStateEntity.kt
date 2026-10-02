package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import java.time.Instant

/** 항목별로 이미 알린 가장 긴급한 단계. 매일 평가해도 같은 알림이 반복되지 않게 한다. */
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
