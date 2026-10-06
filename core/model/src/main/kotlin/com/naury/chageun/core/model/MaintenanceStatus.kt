package com.naury.chageun.core.model

import java.time.LocalDate

/** 가장 급한 것부터 순서대로 선언한다. 정렬에 [ordinal]을 쓴다. */
enum class MaintenanceState { Overdue, Due, Upcoming, Unknown, Good }

enum class MissingInput { LastService, LastServiceDate, LastServiceMileage, CurrentMileage }

enum class Confidence { High, Medium, Low }

/**
 * 지난 정비 기록이다. 사용자가 둘 중 하나만 기억하는 경우 어느 쪽이든 비어 있을 수 있다.
 * [isMileageEstimated]는 주행거리를 사용자가 넣지 않고 평균 주행량으로 추정했다는 뜻이다.
 */
data class ServiceRecord(val date: LocalDate?, val mileage: Kilometers?, val isMileageEstimated: Boolean = false)

data class MileageReading(val date: LocalDate, val mileage: Kilometers)

data class DueEstimate(val date: LocalDate, val confidence: Confidence)

data class MaintenanceStatus(
    val item: MaintenanceItem,
    val state: MaintenanceState,
    val remainingKm: Long?,
    val remainingDays: Long?,
    val distanceDue: Kilometers?,
    val dateDue: LocalDate?,
    val estimatedDue: DueEstimate?,
    val missingInputs: Set<MissingInput>,
    val ruleSource: RuleSource,
)
