package com.naury.chageun.feature.vehicle

import com.naury.chageun.core.model.MileageEntry
import com.naury.chageun.core.model.MileageSource
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 주행거리 그래프에 그릴 점과 요약. 날짜가 같은 기록은 마지막 값 하나로 합친다. */
internal data class MileageTrend(
    val points: List<Point>,
    /** 첫 기록부터 마지막 기록까지 한 달에 평균 몇 km를 탔는지. 기간이 너무 짧으면 null이다. */
    val monthlyAverageKm: Long?,
) {
    data class Point(val date: LocalDate, val km: Long)

    val first: Point get() = points.first()
    val last: Point get() = points.last()
    val minKm: Long get() = points.minOf { it.km }
    val maxKm: Long get() = points.maxOf { it.km }

    /** 기간 동안 늘어난 거리. 줄었으면 null로 두어 "+-" 같은 표시를 하지 않는다. */
    val drivenKm: Long? get() = (last.km - first.km).takeIf { it > 0 }

    /** [date]가 첫 기록에서 마지막 기록 사이 어디쯤인지 0~1로. 가로축을 시간에 비례하게 그린다. */
    fun fractionOf(date: LocalDate): Float {
        val span = ChronoUnit.DAYS.between(first.date, last.date)
        return if (span == 0L) 0f else ChronoUnit.DAYS.between(first.date, date).toFloat() / span
    }

    companion object {
        /**
         * 날짜가 다른 기록이 두 개보다 적으면 추이를 그릴 수 없어 null이다. [log]는 순서와 상관없다.
         * 계기판 교정이 있으면 그 전 값은 잘못 입력한 기준이므로 마지막 교정부터만 그린다.
         */
        fun from(log: List<MileageEntry>): MileageTrend? {
            val sorted = log.sortedBy { it.date }
            val sinceCorrection = sorted.lastOrNull { it.source == MileageSource.Correction }
                ?.let { correction -> sorted.filter { it.date >= correction.date } }
                ?: sorted
            val points = sinceCorrection
                .groupBy { it.date }
                .map { (date, entries) -> Point(date, entries.last().mileage.value) }
            if (points.size < MIN_POINTS) return null
            val days = ChronoUnit.DAYS.between(points.first().date, points.last().date)
            val driven = points.last().km - points.first().km
            // 2주보다 짧으면 하루 이틀의 운행이 한 달로 부풀려 보이고, 줄었으면 평균이 의미 없어 내지 않는다.
            val monthly = if (days >= MIN_DAYS_FOR_AVERAGE && driven > 0) {
                (driven * DAYS_PER_MONTH / days).toLong()
            } else {
                null
            }
            return MileageTrend(points, monthly)
        }

        private const val MIN_POINTS = 2
        private const val MIN_DAYS_FOR_AVERAGE = 14
        private const val DAYS_PER_MONTH = 30.4
    }
}
