package com.naury.chageun.core.model

import java.time.LocalDate

data class ServiceHistoryEntry(
    val id: String,
    val date: LocalDate?,
    val mileage: Kilometers?,
    val costWon: Long?,
    val shopName: String?,
    /** 주행거리를 평균 주행량으로 추정한 기록이다. 화면에 '추정'으로 표시한다. */
    val isMileageEstimated: Boolean = false,
)
