package com.naury.chageun.feature.history

import com.naury.chageun.core.domain.history.TimelineQuery
import java.time.LocalDate

enum class HistoryPeriod { All, LastMonth, Last3Months, Last6Months, ThisYear, Custom }

/** 기획서 10.3 고급 필터. 공식 데이터/사용자 입력 필터는 공식 데이터 연동(ADR-004) 뒤에 추가한다. */
data class AdvancedFilter(
    val period: HistoryPeriod = HistoryPeriod.All,
    val customFrom: LocalDate? = null,
    val customTo: LocalDate? = null,
    val minCostWon: Long? = null,
    val maxCostWon: Long? = null,
    val withAttachmentsOnly: Boolean = false,
) {
    /** 필터 버튼에 보여 줄 적용 중인 조건 수. */
    val activeCount: Int
        get() = listOf(
            period != HistoryPeriod.All,
            minCostWon != null || maxCostWon != null,
            withAttachmentsOnly,
        ).count { it }

    /** [today]를 기준으로 기간 프리셋을 날짜로 바꿔 [query]에 넣는다. */
    fun applyTo(query: TimelineQuery, today: LocalDate): TimelineQuery {
        val (from, to) = when (period) {
            HistoryPeriod.All -> null to null
            HistoryPeriod.LastMonth -> today.minusMonths(1) to today
            HistoryPeriod.Last3Months -> today.minusMonths(QUARTER_MONTHS) to today
            HistoryPeriod.Last6Months -> today.minusMonths(HALF_YEAR_MONTHS) to today
            HistoryPeriod.ThisYear -> today.withDayOfYear(1) to today
            HistoryPeriod.Custom -> customFrom to customTo
        }
        return query.copy(
            dateFrom = from,
            dateTo = to,
            minCostWon = minCostWon,
            maxCostWon = maxCostWon,
            withAttachmentsOnly = withAttachmentsOnly,
        )
    }

    /** SavedStateHandle에 문자열 하나로 보관한다. */
    fun encode(): String = listOf(
        period.name,
        customFrom?.toEpochDay(),
        customTo?.toEpochDay(),
        minCostWon,
        maxCostWon,
        withAttachmentsOnly,
    ).joinToString(SEPARATOR) { it?.toString().orEmpty() }

    companion object {
        private const val SEPARATOR = "|"
        private const val FIELD_COUNT = 6
        private const val QUARTER_MONTHS = 3L
        private const val HALF_YEAR_MONTHS = 6L

        fun decode(value: String?): AdvancedFilter {
            val parts = value?.split(SEPARATOR)?.takeIf { it.size == FIELD_COUNT } ?: return AdvancedFilter()
            return AdvancedFilter(
                period = HistoryPeriod.entries.firstOrNull { it.name == parts[0] } ?: HistoryPeriod.All,
                customFrom = parts[1].toLongOrNull()?.let(LocalDate::ofEpochDay),
                customTo = parts[2].toLongOrNull()?.let(LocalDate::ofEpochDay),
                minCostWon = parts[3].toLongOrNull(),
                maxCostWon = parts[4].toLongOrNull(),
                withAttachmentsOnly = parts[5].toBoolean(),
            )
        }
    }
}
