package com.naury.chageun.core.model

import java.time.LocalDate

data class ServiceHistoryEntry(
    val id: String,
    val date: LocalDate?,
    val mileage: Kilometers?,
    val costWon: Long?,
    val shopName: String?,
)
