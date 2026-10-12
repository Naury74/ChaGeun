package com.naury.chageun.core.domain.calendar

import com.naury.chageun.core.domain.album.AlbumRepository
import com.naury.chageun.core.domain.history.HistoryRepository
import com.naury.chageun.core.domain.history.TimelineQuery
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.CalendarDay
import com.naury.chageun.core.model.CalendarMonth
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 한 달 달력에 기록(주행거리 포함)·앨범 사진·예정 일정을 날짜별로 모은다. 그리드에 함께 보이는 앞뒤 달 날짜까지
 * 읽어, 흐리게 보이는 칸에도 점이 찍힌다. 달을 옮기면 그 달 범위만 다시 구독한다.
 */
class ObserveCalendarMonthUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val albumRepository: AlbumRepository,
    private val observeUpcomingEvents: ObserveUpcomingEventsUseCase,
    private val clock: Clock,
) {
    operator fun invoke(vehicleId: VehicleId, month: YearMonth, firstDayOfWeek: DayOfWeek): Flow<CalendarMonth> {
        val (gridStart, gridEnd) = gridRange(month, firstDayOfWeek)
        val gridQuery = TimelineQuery(types = TimelineEventType.entries.toSet(), dateFrom = gridStart, dateTo = gridEnd)
        val monthQuery = gridQuery.copy(dateFrom = month.atDay(1), dateTo = month.atEndOfMonth())
        return combine(
            historyRepository.observeTimeline(vehicleId, gridQuery),
            albumRepository.observeBetween(vehicleId, gridStart, gridEnd),
            observeUpcomingEvents(vehicleId, gridStart, gridEnd),
            historyRepository.observeMonthlyCosts(vehicleId, monthQuery),
        ) { records, photos, upcoming, totals ->
            CalendarMonth(
                month = month,
                firstDayOfWeek = firstDayOfWeek,
                gridStart = gridStart,
                gridEnd = gridEnd,
                today = LocalDate.now(clock),
                days = groupByDate(records, photos, upcoming),
                monthTotalWon = totals[month] ?: 0,
                recordCount = records.count { it.date?.let(YearMonth::from) == month },
                photoCount = photos.count { YearMonth.from(it.takenOn) == month },
            )
        }
    }

    private fun groupByDate(
        records: List<TimelineItem>,
        photos: List<AlbumPhoto>,
        upcoming: List<UpcomingEvent>,
    ): Map<LocalDate, CalendarDay> {
        val recordsByDate = records.mapNotNull { record -> record.date?.let { it to record } }
            .groupBy({ it.first }, { it.second })
        val photosByDate = photos.groupBy { it.takenOn }
        val upcomingByDate = upcoming.groupBy { it.date }
        return (recordsByDate.keys + photosByDate.keys + upcomingByDate.keys).associateWith { date ->
            CalendarDay(
                date = date,
                records = recordsByDate[date].orEmpty(),
                photos = photosByDate[date].orEmpty(),
                upcoming = upcomingByDate[date].orEmpty(),
            )
        }
    }

    companion object {
        /** 달의 첫 주 시작일부터 마지막 주 끝날까지. 주 시작 요일은 기기 언어를 따른다(한국어는 일요일). */
        fun gridRange(month: YearMonth, firstDayOfWeek: DayOfWeek): Pair<LocalDate, LocalDate> {
            val start = month.atDay(1).with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            val end = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(firstDayOfWeek.minus(1)))
            return start to end
        }
    }
}
