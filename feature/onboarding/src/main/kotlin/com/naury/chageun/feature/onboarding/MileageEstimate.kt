package com.naury.chageun.feature.onboarding

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/**
 * 계기판을 바로 볼 수 없는 사람을 위한 대략값. 연식부터 지금까지 해마다 평균만큼 달렸다고 보고
 * 천 km 단위로 반올림한다. 나중에 주행거리를 갱신하면 실제 값으로 바뀐다.
 */
internal object MileageEstimate {

    fun fromModelYear(modelYear: Int?, today: LocalDate): Long? {
        modelYear ?: return null
        // 연식은 출고 전해 하반기부터 쓰이므로 연식의 1월 1일을 출고 시점으로 본다.
        val years = (today.year - modelYear) + (today.dayOfYear - 1) / DAYS_PER_YEAR
        if (years <= 0) return null
        return (years * KM_PER_YEAR / ROUNDING).roundToLong() * ROUNDING.toLong()
    }

    /**
     * [serviceDate]에 교체했을 때의 주행거리를 이 차의 평균 주행량으로 거꾸로 계산한다.
     * 연식이 너무 최근이라 평균을 낼 수 없으면 국내 평균을 쓰고, 0보다 작아지면 0으로 둔다.
     */
    fun mileageAt(serviceDate: LocalDate, currentMileage: Long, modelYear: Int?, today: LocalDate): Long {
        val daysAgo = ChronoUnit.DAYS.between(serviceDate, today).coerceAtLeast(0)
        val ownedDays = modelYear?.let { ChronoUnit.DAYS.between(LocalDate.of(it, 1, 1), today) } ?: 0
        val kmPerDay = if (ownedDays >= MIN_DAYS_FOR_OWN_PACE) {
            currentMileage.toDouble() / ownedDays
        } else {
            KM_PER_YEAR / DAYS_PER_YEAR
        }
        return (currentMileage - (daysAgo * kmPerDay).roundToLong()).coerceAtLeast(0)
    }

    // 국내 승용차 연평균 주행거리(약 1만 2천 km)를 쓴다.
    private const val KM_PER_YEAR = 12_000.0
    private const val DAYS_PER_YEAR = 365.0
    private const val ROUNDING = 1_000.0

    // 반년도 안 된 차는 초기 주행이 평균을 크게 흔들어 국내 평균을 쓴다.
    private const val MIN_DAYS_FOR_OWN_PACE = 180
}
