package com.naury.chageun.core.model

import java.time.LocalDate

/** Declared from most to least urgent; [ordinal] is used for sorting. */
enum class MaintenanceState { Overdue, Due, Upcoming, Unknown, Good }

enum class MissingInput { LastService, LastServiceDate, LastServiceMileage, CurrentMileage }

enum class Confidence { High, Medium, Low }

/** A past service; either value may be unknown when the user only remembers one of them. */
data class ServiceRecord(val date: LocalDate?, val mileage: Kilometers?)

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
