package com.naury.chageun.core.domain.maintenance

import com.naury.chageun.core.model.Confidence
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class DrivingPace(val kmPerDay: Double, val confidence: Confidence)

class DrivingPaceEstimator @Inject constructor() {

    fun estimate(history: List<MileageReading>, today: LocalDate): DrivingPace? {
        val readings = normalize(history, today)
        if (readings.size < 2) return null

        var totalKm = 0L
        var totalDays = 0L
        var excludedSegments = 0
        readings.zipWithNext { previous, next ->
            val km = next.mileage distanceFrom previous.mileage
            val days = ChronoUnit.DAYS.between(previous.date, next.date)
            if (km < 0 || km / days > MAX_PLAUSIBLE_KM_PER_DAY) {
                excludedSegments++
            } else {
                totalKm += km
                totalDays += days
            }
        }
        return if (totalDays == 0L || totalKm == 0L) {
            null
        } else {
            DrivingPace(totalKm.toDouble() / totalDays, confidenceFor(totalDays, excludedSegments))
        }
    }

    /**
     * 최근 기록으로 속도를 구할 수 없을 때 연식 1월 1일부터 지금까지의 평균으로 대신한다.
     * 오래 탄 차일수록 최근 습관과 다를 수 있어 신뢰도는 항상 낮음이다. 반년이 안 된 차는 평균을 내지 않는다.
     */
    fun lifetimeEstimate(currentMileage: Kilometers?, modelYear: Int?, today: LocalDate): DrivingPace? {
        currentMileage ?: return null
        modelYear ?: return null
        val days = ChronoUnit.DAYS.between(LocalDate.of(modelYear, 1, 1), today)
        if (days < MIN_LIFETIME_DAYS || currentMileage.value <= 0) return null
        return DrivingPace(currentMileage.value.toDouble() / days, Confidence.Low)
    }

    private fun normalize(history: List<MileageReading>, today: LocalDate): List<MileageReading> {
        val windowStart = today.minusDays(WINDOW_DAYS)
        return history
            .filter { it.date in windowStart..today }
            .groupBy { it.date }
            .map { (_, sameDay) -> sameDay.maxBy { it.mileage } }
            .sortedBy { it.date }
    }

    private fun confidenceFor(observedDays: Long, excludedSegments: Int) = when {
        excludedSegments > 0 -> Confidence.Low
        observedDays < MEDIUM_CONFIDENCE_MIN_DAYS -> Confidence.Low
        observedDays < HIGH_CONFIDENCE_MIN_DAYS -> Confidence.Medium
        else -> Confidence.High
    }

    private companion object {
        const val WINDOW_DAYS = 180L
        const val MAX_PLAUSIBLE_KM_PER_DAY = 1_000
        const val MEDIUM_CONFIDENCE_MIN_DAYS = 30L
        const val HIGH_CONFIDENCE_MIN_DAYS = 90L
        const val MIN_LIFETIME_DAYS = 180L
    }
}
