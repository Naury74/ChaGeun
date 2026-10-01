package com.naury.chageun.core.model

import java.time.Instant
import java.time.LocalDate

enum class FuelField { Total, Volume, UnitPrice }

/** Volume is kept in millilitres so amounts never go through floating point. */
data class FuelAmounts(
    val totalPriceWon: Long,
    val volumeMl: Long,
    val unitPriceWon: Long,
    val computedField: FuelField?,
)

data class FuelEntry(
    val date: LocalDate,
    val mileage: Kilometers,
    val amounts: FuelAmounts,
    val isFullTank: Boolean,
    val stationName: String? = null,
    val memo: String? = null,
)

enum class CheckKind { Inspection, Repair, Note }

data class CheckEntry(
    val kind: CheckKind,
    val date: LocalDate,
    val title: String,
    val mileage: Kilometers? = null,
    val costWon: Long? = null,
    val memo: String? = null,
)

enum class TimelineEventType { Maintenance, Fuel, Inspection, Repair, Note }

enum class RecordSource { User, Official }

data class RecordRef(val type: TimelineEventType, val id: String)

data class TimelineItem(
    val ref: RecordRef,
    val date: LocalDate?,
    /** Free text for user records; null for maintenance, which is labelled by [maintenanceItem]. */
    val title: String?,
    val maintenanceItem: MaintenanceItem?,
    val mileage: Kilometers?,
    val costWon: Long?,
    val source: RecordSource,
    val createdAt: Instant,
)

sealed interface RecordDetail {
    val ref: RecordRef

    data class Maintenance(
        override val ref: RecordRef,
        val item: MaintenanceItem,
        val entry: ServiceHistoryEntry,
        val memo: String?,
    ) : RecordDetail

    data class Fuel(override val ref: RecordRef, val entry: FuelEntry) : RecordDetail

    data class Check(override val ref: RecordRef, val entry: CheckEntry) : RecordDetail
}
