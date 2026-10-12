package com.naury.chageun.feature.history.calendar

import androidx.lifecycle.SavedStateHandle
import com.naury.chageun.core.model.MaintenanceItem
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * 보는 달·고른 날·월/주 보기. 모두 SavedStateHandle에 두어 회전·접기에도 남는다.
 * 주 보기에서도 데이터는 고른 날이 속한 달 단위로 읽으므로, 주를 옮기다 달이 바뀌면 보는 달도 따라 바꾼다.
 */
internal class CalendarNavigation(private val handle: SavedStateHandle, private val clock: Clock) {
    val month: StateFlow<String> = handle.getStateFlow(KEY_MONTH, YearMonth.now(clock).toString())
    val selectedDate: StateFlow<String> = handle.getStateFlow(KEY_DATE, LocalDate.now(clock).toString())
    val mode: Flow<CalendarMode?> = handle.getStateFlow<String?>(KEY_MODE, null)
        .map { name -> CalendarMode.entries.firstOrNull { it.name == name } }

    fun selectDate(date: LocalDate) {
        handle[KEY_DATE] = date.toString()
    }

    /** 이번 달로 오면 오늘을, 다른 달이면 그 달 1일을 고른다. */
    fun showMonth(target: YearMonth) {
        val today = LocalDate.now(clock)
        handle[KEY_MONTH] = target.toString()
        selectDate(if (target == YearMonth.from(today)) today else target.atDay(1))
    }

    fun showToday() = showMonth(YearMonth.now(clock))

    /** 같은 요일을 유지한 채 [weeks]주 옮긴다. */
    fun moveWeek(weeks: Long) {
        focusOn(LocalDate.parse(selectedDate.value).plusWeeks(weeks))
    }

    /** 그날이 속한 달을 열고 그날을 고른다. */
    fun focusOn(date: LocalDate) {
        handle[KEY_MONTH] = YearMonth.from(date).toString()
        selectDate(date)
    }

    fun setMode(mode: CalendarMode) {
        handle[KEY_MODE] = mode.name
    }

    private companion object {
        const val KEY_MONTH = "calendar_month"
        const val KEY_DATE = "calendar_date"
        const val KEY_MODE = "calendar_mode"
    }
}

/** 검색칸에 넣은 값. [matchingItems]는 현지화된 이름이 검색어와 맞는 정비 항목이다. */
internal data class CalendarSearchInput(
    val isOpen: Boolean,
    val keyword: String,
    val matchingItems: Set<MaintenanceItem>,
)

/** 필터와 검색어. SavedStateHandle에 두어 회전·접기에도 남는다. */
internal class CalendarQueryState(private val handle: SavedStateHandle) {
    private val kinds = handle.getStateFlow(KEY_KINDS, arrayListOf<String>())
    private val items = handle.getStateFlow(KEY_ITEMS, arrayListOf<String>())
    private val isOpen = handle.getStateFlow(KEY_SEARCH_OPEN, false)
    private val keyword = handle.getStateFlow(KEY_KEYWORD, "")
    private val matchingItems = handle.getStateFlow(KEY_MATCHING_ITEMS, arrayListOf<String>())

    val filter: Flow<CalendarFilter> = combine(kinds, items) { kindNames, itemNames ->
        CalendarFilter(
            kinds = kindNames.mapNotNull { name -> CalendarKind.entries.firstOrNull { it.name == name } }.toSet(),
            items = itemNames.toItems(),
        )
    }

    val search: Flow<CalendarSearchInput> = combine(isOpen, keyword, matchingItems) { open, text, matching ->
        CalendarSearchInput(open, text, matching.toItems())
    }

    fun toggleKind(kind: CalendarKind) {
        val current = kinds.value
        handle[KEY_KINDS] = ArrayList(if (kind.name in current) current - kind.name else current + kind.name)
    }

    fun setItems(selected: Set<MaintenanceItem>) {
        handle[KEY_ITEMS] = ArrayList(selected.map { it.name })
    }

    fun openSearch() {
        handle[KEY_SEARCH_OPEN] = true
    }

    /** 검색칸을 닫으면 검색어도 지운다. 닫힌 채로 결과만 강조되어 남지 않게 한다. */
    fun closeSearch() {
        handle[KEY_SEARCH_OPEN] = false
        search("", emptySet())
    }

    fun search(text: String, matching: Set<MaintenanceItem>) {
        handle[KEY_KEYWORD] = text
        handle[KEY_MATCHING_ITEMS] = ArrayList(matching.map { it.name })
    }

    fun clearAll() {
        handle[KEY_KINDS] = arrayListOf<String>()
        setItems(emptySet())
        closeSearch()
    }

    private fun List<String>.toItems(): Set<MaintenanceItem> =
        mapNotNull { name -> MaintenanceItem.entries.firstOrNull { it.name == name } }.toSet()

    private companion object {
        const val KEY_KINDS = "calendar_kinds"
        const val KEY_ITEMS = "calendar_items"
        const val KEY_SEARCH_OPEN = "calendar_search_open"
        const val KEY_KEYWORD = "calendar_keyword"
        const val KEY_MATCHING_ITEMS = "calendar_matching_items"
    }
}
