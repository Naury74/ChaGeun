package com.naury.chageun.core.database.entity

import androidx.room.ColumnInfo
import java.time.Instant
import java.time.LocalDate

data class TimelineRow(
    @ColumnInfo(name = "event_type") val eventType: String,
    val id: String,
    @ColumnInfo(name = "occurred_on") val occurredOn: LocalDate?,
    val title: String?,
    @ColumnInfo(name = "item_type") val itemType: String?,
    @ColumnInfo(name = "mileage_km") val mileageKm: Long?,
    @ColumnInfo(name = "cost_won") val costWon: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "source_type") val sourceType: String,
)
