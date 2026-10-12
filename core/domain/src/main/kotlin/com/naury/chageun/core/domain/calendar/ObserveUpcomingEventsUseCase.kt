package com.naury.chageun.core.domain.calendar

import com.naury.chageun.core.domain.maintenance.ObserveMaintenanceOverviewUseCase
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.model.VehicleId
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [from]~[to](양 끝 포함)에 놓이는 예정 일정. 기한이 지난 일은 오늘에 모이므로 범위에 오늘이 있어야 보인다. */
class ObserveUpcomingEventsUseCase @Inject constructor(
    private val observeMaintenanceOverview: ObserveMaintenanceOverviewUseCase,
    private val clock: Clock,
) {
    operator fun invoke(vehicleId: VehicleId, from: LocalDate, to: LocalDate): Flow<List<UpcomingEvent>> =
        observeMaintenanceOverview(vehicleId).map { overview ->
            UpcomingEventPlanner.plan(overview, LocalDate.now(clock)).within(from, to)
        }
}
