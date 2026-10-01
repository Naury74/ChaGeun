package com.naury.chageun.core.model

import java.time.LocalDate

/** A completed replacement entered by the user. A null [costWon] means "not entered"; 0 is a real free service. */
data class ServiceEntry(
    val item: MaintenanceItem,
    val date: LocalDate,
    val mileage: Kilometers,
    val costWon: Long? = null,
    val shopName: String? = null,
    val memo: String? = null,
)
