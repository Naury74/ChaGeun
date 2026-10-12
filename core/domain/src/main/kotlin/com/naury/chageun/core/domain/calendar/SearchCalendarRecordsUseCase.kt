package com.naury.chageun.core.domain.calendar

import com.naury.chageun.core.domain.history.HistoryRepository
import com.naury.chageun.core.domain.history.TimelineQuery
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.VehicleId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * 달력 검색. 달과 상관없이 모든 기록에서 제목·메모·정비소·주유소와 현지화된 항목 이름([matchingItems])으로 찾는다.
 * 달력에 놓을 수 없는 날짜 없는 기록은 빼고, 최신순으로 [MAX_RESULTS]개까지만 읽는다.
 */
class SearchCalendarRecordsUseCase @Inject constructor(private val historyRepository: HistoryRepository) {
    operator fun invoke(
        vehicleId: VehicleId,
        keyword: String,
        matchingItems: Set<MaintenanceItem>,
    ): Flow<List<TimelineItem>> {
        if (keyword.isBlank()) return flowOf(emptyList())
        val query = TimelineQuery(
            types = TimelineEventType.entries.toSet(),
            keyword = keyword.trim(),
            matchingItems = matchingItems,
        )
        return historyRepository.observeTimeline(vehicleId, query, MAX_RESULTS)
            .map { records -> records.filter { it.date != null } }
    }

    companion object {
        const val MAX_RESULTS = 200
    }
}
