package com.naury.chageun.core.model

import java.time.Instant
import java.time.LocalDate

enum class FuelField { Total, Volume, UnitPrice }

/** 주유량은 밀리리터 단위로 보관해 부동소수점 계산을 거치지 않게 한다. */
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
    /** 사용자 기록의 자유 입력 제목이다. 정비 기록은 [maintenanceItem]으로 이름을 붙이므로 null이다. */
    val title: String?,
    val maintenanceItem: MaintenanceItem?,
    val mileage: Kilometers?,
    val costWon: Long?,
    val source: RecordSource,
    val createdAt: Instant,
    /** 주행거리를 평균 주행량으로 추정한 정비 기록이다. */
    val isMileageEstimated: Boolean = false,
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
