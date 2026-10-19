package com.naury.chageun.feature.history.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.motion.rememberReduceMotion
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.CalendarMonth
import com.naury.chageun.core.ui.currentLocale
import com.naury.chageun.feature.history.R
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KVariant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import kotlin.math.abs

/** 그리드에 그리는 한 장. 월 보기는 그 달 전체, 주 보기는 고른 날이 있는 한 주다. */
private data class GridPage(val calendar: CalendarMonth, val mode: CalendarMode, val week: List<LocalDate>) {
    /** 이 값이 바뀌면 다른 장으로 넘어가는 애니메이션을 한다. */
    val key: Any get() = if (mode == CalendarMode.Week) week.first() else calendar.month

    /** 앞뒤를 가리는 기준 날짜. */
    val anchor: LocalDate get() = if (mode == CalendarMode.Week) week.first() else calendar.month.atDay(1)

    val rows: List<List<LocalDate>> get() = if (mode == CalendarMode.Week) listOf(week) else calendar.weeks
}

/**
 * 7열 달력. 월 보기는 그 달 전체를, 주 보기는 고른 날이 있는 한 주를 크게 그린다. 좌우로 밀면 달(주 보기는 주)이
 * 바뀐다. 데이터가 그 달 것으로 바뀔 때 넘어가므로, 넘어가는 동안 앞 달 칸에 새 달 표시가 찍히지 않는다.
 * 월↔주를 바꿀 때는 높이가 접히고 펼쳐지며, 애니메이션 줄이기 설정이면 바로 바뀐다.
 */
@Composable
internal fun CalendarGrid(
    state: CalendarUiState,
    calendar: CalendarMonth,
    mode: CalendarMode,
    actions: CalendarActions,
    modifier: Modifier = Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reduceMotion = rememberReduceMotion()
    val page = GridPage(calendar, mode, state.selectedWeek(calendar))
    val matches = state.resultDates.toSet().takeIf { state.search.isActive }
    Column(modifier.gridSwipe(page, isRtl, actions)) {
        WeekdayHeader(page.week)
        AnimatedContent(
            targetState = page,
            contentKey = { it.key },
            transitionSpec = { gridTransition(initialState, targetState, reduceMotion) },
            label = "calendar-grid",
        ) { shown ->
            val style = if (shown.mode == CalendarMode.Week) CellStyle.Week else CellStyle.Month
            Column {
                shown.rows.forEach { dates ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        dates.forEach { date ->
                            DayCell(
                                day = state.visibleDay(date, shown.calendar),
                                isToday = date == shown.calendar.today,
                                isSelected = date == state.selectedDate,
                                isInMonth = YearMonth.from(date) == shown.calendar.month,
                                style = style,
                                match = when {
                                    matches == null -> SearchMatch.None
                                    date in matches -> SearchMatch.Match
                                    else -> SearchMatch.Dimmed
                                },
                                onClick = { actions.onSelectDate(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun gridTransition(from: GridPage, to: GridPage, reduceMotion: Boolean): ContentTransform = when {
    reduceMotion -> ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.None,
        sizeTransform = SizeTransform { _, _ -> snap() },
    )
    from.mode != to.mode -> fadeIn() togetherWith fadeOut()
    else -> {
        val direction = if (to.anchor > from.anchor) 1 else -1
        (slideInHorizontally { width -> direction * width / SLIDE_DIVISOR } + fadeIn()) togetherWith
            (slideOutHorizontally { width -> -direction * width / SLIDE_DIVISOR } + fadeOut())
    }
}

/** 손가락을 충분히 민 뒤 떼면 다음·이전 달(주)로 간다. 오른쪽에서 왼쪽으로 쓰는 언어에서는 방향이 반대다. */
@Composable
private fun Modifier.gridSwipe(page: GridPage, isRtl: Boolean, actions: CalendarActions): Modifier {
    val currentActions by rememberUpdatedState(actions)
    return pointerInput(page.key, page.mode, isRtl) {
        val threshold = SWIPE_THRESHOLD.toPx()
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                if (abs(total) >= threshold) {
                    val step = if (isRtl == total > 0) 1L else -1L
                    if (page.mode == CalendarMode.Week) {
                        currentActions.onMoveWeek(step)
                    } else {
                        currentActions.onShowMonth(page.calendar.month.plusMonths(step))
                    }
                }
            },
        ) { change, dragAmount ->
            change.consume()
            total += dragAmount
        }
    }
}

/** 요일 머리글. 날짜 칸이 요일까지 읽어 주므로 TalkBack에서는 건너뛴다. */
@Composable
private fun WeekdayHeader(week: List<LocalDate>) {
    val locale = currentLocale()
    Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
        week.forEach { date ->
            Text(
                date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = ChageunTheme.spacing.xxs),
            )
        }
    }
}

/**
 * 그리드 아래 줄. 휴대폰에서는 가운데 손잡이를 위로 밀면 주 보기로 접히고 아래로 당기면 월 보기로 펼쳐진다
 * (눌러도 바뀐다). 손잡이는 끝의 [주 보기/월 보기] 버튼과 같은 일을 하므로 TalkBack에서는 버튼만 읽는다.
 */
@Composable
internal fun CalendarModeBar(
    mode: CalendarMode,
    showHandle: Boolean,
    onSetMode: (CalendarMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val target = if (mode == CalendarMode.Month) CalendarMode.Week else CalendarMode.Month
    Box(modifier.fillMaxWidth()) {
        if (showHandle) DragHandle(mode, onSetMode, Modifier.align(Alignment.Center))
        KButton(
            stringResource(
                if (target ==
                    CalendarMode.Week
                ) {
                    R.string.calendar_week_view
                } else {
                    R.string.calendar_month_view
                },
            ),
            onClick = { onSetMode(target) },
            variant = KVariant.Text,
            icon = if (target == CalendarMode.Week) Icons.Filled.CalendarViewWeek else Icons.Filled.CalendarViewMonth,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun DragHandle(mode: CalendarMode, onSetMode: (CalendarMode) -> Unit, modifier: Modifier = Modifier) {
    val currentMode by rememberUpdatedState(mode)
    val currentOnSetMode by rememberUpdatedState(onSetMode)
    Box(
        modifier = modifier
            .size(HANDLE_TOUCH_WIDTH, ChageunTheme.spacing.minTouchTarget)
            .clearAndSetSemantics {}
            .pointerInput(Unit) {
                detectTapGestures {
                    currentOnSetMode(if (currentMode == CalendarMode.Month) CalendarMode.Week else CalendarMode.Month)
                }
            }
            .pointerInput(Unit) {
                val threshold = HANDLE_DRAG_THRESHOLD.toPx()
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total <= -threshold -> currentOnSetMode(CalendarMode.Week)
                            total >= threshold -> currentOnSetMode(CalendarMode.Month)
                        }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    total += dragAmount
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(HANDLE_WIDTH, HANDLE_HEIGHT)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HANDLE_ALPHA), CircleShape),
        )
    }
}

private val SWIPE_THRESHOLD = 48.dp
private val HANDLE_TOUCH_WIDTH = 72.dp
private val HANDLE_WIDTH = 32.dp
private val HANDLE_HEIGHT = 4.dp
private val HANDLE_DRAG_THRESHOLD = 24.dp
private const val HANDLE_ALPHA = 0.4f
private const val SLIDE_DIVISOR = 4
