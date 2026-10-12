package com.naury.chageun.core.domain.calendar

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.RecordSource
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.VehicleId
import com.naury.chageun.core.testing.FakeHistoryRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SearchCalendarRecordsUseCaseTest {

    private val history = FakeHistoryRepository().apply { appliesKeyword = true }
    private val search = SearchCalendarRecordsUseCase(history)

    private fun record(id: String, date: LocalDate?, title: String? = null, item: MaintenanceItem? = null) =
        TimelineItem(
            RecordRef(if (item != null) TimelineEventType.Maintenance else TimelineEventType.Note, id),
            date,
            title,
            item,
            Kilometers(40_000),
            null,
            RecordSource.User,
            Instant.EPOCH,
        )

    @Test
    fun blankKeyword_findsNothing_withoutReading() = runTest {
        assertThat(search(VehicleId("v1"), "  ", emptySet()).first()).isEmpty()
        assertThat(history.lastQuery).isNull()
    }

    @Test
    fun searchesEveryMonthAndType_skippingUndatedRecords() = runTest {
        history.timeline.value = listOf(
            record("oil-oct", LocalDate.of(2026, 10, 8), item = MaintenanceItem.EngineOil),
            record("oil-undated", null, item = MaintenanceItem.EngineOil),
            record("note", LocalDate.of(2025, 3, 1), title = "Engine noise"),
            record("other", LocalDate.of(2026, 9, 1), title = "Car wash"),
        )

        val results = search(VehicleId("v1"), " engine ", setOf(MaintenanceItem.EngineOil)).first()

        assertThat(results.map { it.ref.id }).containsExactly("oil-oct", "note").inOrder()
        assertThat(history.lastQuery?.keyword).isEqualTo("engine")
        assertThat(history.lastQuery?.types).containsExactlyElementsIn(TimelineEventType.entries)
        assertThat(history.lastQuery?.dateFrom).isNull()
        assertThat(history.lastLimit).isEqualTo(SearchCalendarRecordsUseCase.MAX_RESULTS)
    }
}
