package com.naury.chageun.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * 앞으로 할 일. [date]는 달력에 놓이는 날짜다. 기한이 지난 일은 지난 날짜에 흩어 두지 않고
 * 오늘에 모으며 [isOverdue]로 표시한다.
 */
sealed interface UpcomingEvent {
    val date: LocalDate
    val isOverdue: Boolean

    /**
     * 정비 항목의 교체 시점. 교체 기준일(dateDue)이면 확정이고, 주행 속도로 내다본 날이면 [isEstimated]다.
     * 확정 일정의 [confidence]는 null이다.
     */
    data class MaintenanceDue(
        val item: MaintenanceItem,
        override val date: LocalDate,
        val isEstimated: Boolean,
        val confidence: Confidence?,
        override val isOverdue: Boolean = false,
    ) : UpcomingEvent

    data class InspectionDue(override val date: LocalDate, override val isOverdue: Boolean = false) : UpcomingEvent

    /** 마지막 주행거리 기록 뒤 갱신을 권하는 날. */
    data class MileageUpdate(override val date: LocalDate, override val isOverdue: Boolean = false) : UpcomingEvent
}

/**
 * 달력 하루 칸. 기록·사진은 지난 일, [upcoming]은 앞으로 할 일이다. 사진은 앨범 사진·동영상([photos])과
 * 그날 기록에 붙인 사진([recordPhotos], 영수증 등)을 따로 들고, [photoCount]는 동영상까지 합쳐 센다.
 * 화면 읽기 문구처럼 종류를 나눠 말할 때는 [videoCount]·[soundCount]를 쓴다.
 */
data class CalendarDay(
    val date: LocalDate,
    val records: List<TimelineItem> = emptyList(),
    val photos: List<AlbumPhoto> = emptyList(),
    val upcoming: List<UpcomingEvent> = emptyList(),
    val recordPhotos: List<RecordPhoto> = emptyList(),
) {
    val photoCount: Int get() = photos.size + recordPhotos.size

    /** 앨범 동영상 수. [photoCount]에 들어 있다. */
    val videoCount: Int get() = photos.count { it.isVideo }

    /** 그날 기록에 붙인 녹음한 소리 수. */
    val soundCount: Int get() = records.sumOf { it.soundCount }

    val isEmpty: Boolean get() = records.isEmpty() && photoCount == 0 && upcoming.isEmpty()
}

/**
 * 한 달 달력. [gridStart]~[gridEnd]는 그리드에 보이는 앞뒤 달 날짜까지 포함하며, [days]에는 그 범위에서
 * 내용이 있는 날만 들어 있다. 요약 값([monthTotalWon], [recordCount], [photoCount])은 [month] 안만 센다.
 * [photoCount]는 앨범 사진과 기록에 붙인 사진을 합친 수다.
 */
data class CalendarMonth(
    val month: YearMonth,
    val firstDayOfWeek: DayOfWeek,
    val gridStart: LocalDate,
    val gridEnd: LocalDate,
    val today: LocalDate,
    val days: Map<LocalDate, CalendarDay> = emptyMap(),
    val monthTotalWon: Long = 0,
    val recordCount: Int = 0,
    val photoCount: Int = 0,
) {
    fun day(date: LocalDate): CalendarDay = days[date] ?: CalendarDay(date)

    /** 그리드의 주 단위 날짜. 각 주는 7일이다. */
    val weeks: List<List<LocalDate>>
        get() = generateSequence(gridStart) { it.plusDays(1) }
            .takeWhile { !it.isAfter(gridEnd) }
            .chunked(DAYS_PER_WEEK)
            .toList()

    private companion object {
        const val DAYS_PER_WEEK = 7
    }
}

/** 기록 탭에서 고른 보기. 다음에 열 때도 같은 보기로 시작한다. */
enum class HistoryView { List, Calendar }
