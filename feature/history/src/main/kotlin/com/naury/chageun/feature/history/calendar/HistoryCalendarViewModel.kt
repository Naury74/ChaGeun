package com.naury.chageun.feature.history.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naury.chageun.core.domain.calendar.ObserveCalendarMonthUseCase
import com.naury.chageun.core.domain.calendar.ObserveUpcomingEventsUseCase
import com.naury.chageun.core.domain.settings.SettingsRepository
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import com.naury.chageun.core.model.HistoryView
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * 기록 탭의 보기(목록·달력)와 달력 상태. 보는 달·고른 날·레이어는 SavedStateHandle에 두어 회전·접기에도 남고,
 * 고른 보기는 설정에 저장해 다음에 열 때도 이어진다. 기록 상세는 목록과 같이 HistoryViewModel이 맡는다.
 * 달력 데이터는 달력 보기일 때만 읽는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryCalendarViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    vehicleRepository: VehicleRepository,
    observeCalendarMonth: ObserveCalendarMonthUseCase,
    observeUpcomingEvents: ObserveUpcomingEventsUseCase,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val month = savedStateHandle.getStateFlow(KEY_MONTH, YearMonth.now(clock).toString())
    private val selectedDate = savedStateHandle.getStateFlow(KEY_DATE, LocalDate.now(clock).toString())
    private val layers = savedStateHandle.getStateFlow(KEY_LAYERS, ArrayList(CalendarLayer.entries.map { it.name }))
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
            if (!isCalendar) return@flatMapLatest flowOf(CalendarUiState())
            val calendar = combine(month, firstDayOfWeek) { name, day -> YearMonth.parse(name) to day }
                .flatMapLatest { (shown, day) -> observeCalendarMonth(vehicle.id, shown, day) }
            val upcomingSoon = selectedDate.flatMapLatest { date ->
                val today = LocalDate.now(clock)
                if (LocalDate.parse(date) == today) {
                    observeUpcomingEvents(vehicle.id, today.plusDays(1), today.plusDays(UPCOMING_SOON_DAYS))
                } else {
                    flowOf(emptyList())
                }
            }
            combine(calendar, selectedDate, layers, upcomingSoon) { shown, date, layerNames, soon ->
                CalendarUiState(
                    isLoading = false,
                    calendar = shown,
                    selectedDate = LocalDate.parse(date),
                    layers = layerNames.mapNotNull { name -> CalendarLayer.entries.firstOrNull { it.name == name } }
                        .toSet(),
                    upcomingSoon = soon,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CalendarUiState())

    fun setView(view: HistoryView) {
        viewModelScope.launch { settingsRepository.setHistoryView(view) }
    }

    fun selectDate(date: LocalDate) {
        savedStateHandle[KEY_DATE] = date.toString()
    }

    /** 이번 달로 오면 오늘을, 다른 달이면 그 달 1일을 고른다. */
    fun showMonth(target: YearMonth) {
        val today = LocalDate.now(clock)
        savedStateHandle[KEY_MONTH] = target.toString()
        selectDate(if (target == YearMonth.from(today)) today else target.atDay(1))
    }

    fun showToday() = showMonth(YearMonth.now(clock))

    fun toggleLayer(layer: CalendarLayer) {
        val current = layers.value
        savedStateHandle[KEY_LAYERS] =
            ArrayList(if (layer.name in current) current - layer.name else current + layer.name)
    }

    /** 홈·앨범에서 날짜를 골라 들어오면 달력 보기로 바꾸고 그날을 연다. */
    fun focusOn(date: LocalDate) {
        savedStateHandle[KEY_MONTH] = YearMonth.from(date).toString()
        selectDate(date)
        setView(HistoryView.Calendar)
    }

    /** 주 시작 요일은 화면의 언어를 따른다. 앱 언어를 따로 고른 경우 기기 기본값과 다를 수 있다. */
    fun setFirstDayOfWeek(day: DayOfWeek) {
        firstDayOfWeek.value = day
    }

    private companion object {
        const val KEY_MONTH = "calendar_month"
        const val KEY_DATE = "calendar_date"
        const val KEY_LAYERS = "calendar_layers"
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val UPCOMING_SOON_DAYS = 30L
    }
}
