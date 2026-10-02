package com.naury.chageun.core.model

import java.time.LocalDate

/** 사용자가 입력한 교체 완료 기록이다. [costWon]이 null이면 "입력 안 함"이고, 0은 실제 무상 정비다. */
data class ServiceEntry(
    val item: MaintenanceItem,
    val date: LocalDate,
    val mileage: Kilometers,
    val costWon: Long? = null,
    val shopName: String? = null,
    val memo: String? = null,
)
