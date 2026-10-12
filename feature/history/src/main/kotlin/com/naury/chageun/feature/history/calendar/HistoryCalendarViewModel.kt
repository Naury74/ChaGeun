package com.naury.chageun.feature.history.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.calendar.ObserveCalendarMonthUseCase
import com.naury.chageun.core.domain.calendar.ObserveUpcomingEventsUseCase
import com.naury.chageun.core.domain.calendar.SearchCalendarRecordsUseCase
import com.naury.chageun.core.domain.settings.SettingsRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.HistoryView
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.model.VehicleId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 기록 탭의 보기(목록·달력)와 달력 상태. 보는 달·고른 날·월/주 보기는 [navigation]이, 필터·검색은 [query]가
 * SavedStateHandle에 두어 회전·접기에도 남는다. 고른 보기는 설정에 저장해 다음에 열 때도 이어진다.
 * 기록 상세는 목록과 같이 HistoryViewModel이 맡는다. 달력 데이터는 달력 보기일 때만 읽는다.
 */
/** 달력이 읽는 데이터. 한 달, 앞으로의 예정, 검색을 함께 넘긴다. */
class CalendarDataSources @Inject constructor(
    val observeMonth: ObserveCalendarMonthUseCase,
    val observeUpcoming: ObserveUpcomingEventsUseCase,
    val searchRecords: SearchCalendarRecordsUseCase,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryCalendarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    vehicleRepository: VehicleRepository,
    private val data: CalendarDataSources,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    internal val navigation = CalendarNavigation(savedStateHandle, clock)
    internal val query = CalendarQueryState(savedStateHandle)
    private val firstDayOfWeek = MutableStateFlow(WeekFields.of(Locale.getDefault()).firstDayOfWeek)

    /** 설정을 읽기 전에는 null이다. 그동안 목록이 잠깐 보였다가 달력으로 바뀌지 않게 한다. */
    val view: StateFlow<HistoryView?> = settingsRepository.settings
        .map { it.historyView }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val uiState: StateFlow<CalendarUiState> = combine(
        vehicleRepository.observePrimaryVehicle().filterNotNull(),
        settingsRepository.settings.map { it.historyView == HistoryView.Calendar }.distinctUntilChanged(),
        ::Pair,
    )
        .flatMapLatest { (vehicle, isCalendar) ->
            if (isCalendar) calendarState(vehicle.id) else flowOf(CalendarUiState())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CalendarUiState())

    private fun calendarState(vehicleId: VehicleId): Flow<CalendarUiState> {
        val calendar = combine(navigation.month, firstDayOfWeek) { name, day -> YearMonth.parse(name) to day }
            .flatMapLatest { (shown, day) -> data.observeMonth(vehicleId, shown, day) }
        val search = query.search.flatMapLatest { input ->
            data.searchRecords(vehicleId, input.keyword, input.matchingItems).map { results ->
                CalendarSearch(input.isOpen, input.keyword, results)
            }
        }
        val selection = combine(navigation.selectedDate, navigation.mode, upcomingSoon(vehicleId), ::Selection)
        return combine(calendar, selection, query.filter, search) { shown, picked, filter, found ->
            CalendarUiState(
                isLoading = false,
                calendar = shown,
                selectedDate = LocalDate.parse(picked.date),
                mode = picked.mode,
                filter = filter,
                search = found,
                upcomingSoon = picked.upcomingSoon,
            )
        }
    }

    /** 오늘을 고르면 내일부터 30일 동안의 예정을 읽는다. 다른 날에는 읽지 않는다. */
    private fun upcomingSoon(vehicleId: VehicleId): Flow<List<UpcomingEvent>> =
        navigation.selectedDate.flatMapLatest { date ->
            val today = LocalDate.now(clock)
            if (LocalDate.parse(date) == today) {
                data.observeUpcoming(vehicleId, today.plusDays(1), today.plusDays(UPCOMING_SOON_DAYS))
            } else {
                flowOf(emptyList())
            }
        }

    fun setView(view: HistoryView) {
        viewModelScope.launch { settingsRepository.setHistoryView(view) }
    }

    /** 홈·앨범에서 날짜를 골라 들어오면 달력 보기로 바꾸고 그날을 연다. */
    fun focusOn(date: LocalDate) {
        navigation.focusOn(date)
        setView(HistoryView.Calendar)
    }

    /** 검색 결과를 누르면 그 기록이 있는 달·날로 간다. */
    fun openResult(record: TimelineItem) {
        record.date?.let(navigation::focusOn)
    }

    fun showPreviousResult() {
        uiState.value.previousResultDate?.let(navigation::focusOn)
    }

    fun showNextResult() {
        uiState.value.nextResultDate?.let(navigation::focusOn)
    }

    /** 주 시작 요일은 화면의 언어를 따른다. 앱 언어를 따로 고른 경우 기기 기본값과 다를 수 있다. */
    fun setFirstDayOfWeek(day: DayOfWeek) {
        firstDayOfWeek.value = day
    }

    private data class Selection(val date: String, val mode: CalendarMode?, val upcomingSoon: List<UpcomingEvent>)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val UPCOMING_SOON_DAYS = 30L
    }
}
