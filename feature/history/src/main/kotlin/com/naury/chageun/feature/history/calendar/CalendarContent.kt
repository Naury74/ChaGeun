package com.naury.chageun.feature.history.calendar

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.motion.rememberReduceMotion
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.CalendarDay
import com.naury.chageun.core.model.CalendarMonth
import com.naury.chageun.core.model.Confidence
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.ui.currentLocale
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.formatYearMonth
import com.naury.chageun.core.ui.rememberFileImage
import com.naury.chageun.core.ui.tone
import com.naury.chageun.feature.history.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlin.math.abs

/** 달 이름과 앞뒤 달 이동, 오늘로 돌아가기. */
@Composable
internal fun CalendarHeader(
    month: YearMonth,
    onShowMonth: (YearMonth) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onShowMonth(month.minusMonths(1)) }) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.calendar_previous_month),
            )
        }
        Text(
            formatYearMonth(month),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        IconButton(onClick = { onShowMonth(month.plusMonths(1)) }) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.calendar_next_month),
            )
        }
        TextButton(onClick = onToday, modifier = Modifier.heightIn(min = ChageunTheme.spacing.minTouchTarget)) {
            Text(stringResource(R.string.calendar_today))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalendarLayerChips(
    layers: Set<CalendarLayer>,
    onToggle: (CalendarLayer) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        CalendarLayer.entries.forEach { layer ->
            val selected = layer in layers
            FilterChip(
                selected = selected,
                onClick = { onToggle(layer) },
                label = { Text(stringResource(layer.labelRes)) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(CHIP_ICON_SIZE)) }
                } else {
                    null
                },
            )
        }
    }
}

/** 이번 달 지출·기록 수·사진 수. */
@Composable
internal fun MonthSummary(calendar: CalendarMonth, modifier: Modifier = Modifier) {
    val parts = listOfNotNull(
        stringResource(R.string.history_month_total, formatNumber(calendar.monthTotalWon))
            .takeIf { calendar.monthTotalWon > 0 },
        pluralStringResource(R.plurals.calendar_summary_records, calendar.recordCount, calendar.recordCount),
        pluralStringResource(R.plurals.calendar_summary_photos, calendar.photoCount, calendar.photoCount),
    )
    Text(
        parts.joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * 7열 월 그리드. 좌우로 밀면 달이 바뀐다. 데이터가 그 달 것으로 바뀔 때 넘어가므로, 넘어가는 동안 앞 달 칸에
 * 새 달 점이 찍히지 않는다.
 */
@Composable
internal fun MonthGrid(
    state: CalendarUiState,
    calendar: CalendarMonth,
    onSelect: (LocalDate) -> Unit,
    onShowMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reduceMotion = rememberReduceMotion()
    Column(modifier.monthSwipe(calendar.month, isRtl, onShowMonth)) {
        WeekdayHeader(calendar.weeks.firstOrNull().orEmpty())
        AnimatedContent(
            targetState = calendar,
            contentKey = { it.month },
            transitionSpec = { monthTransition(initialState.month, targetState.month, reduceMotion) },
            label = "calendar-month",
        ) { shown ->
            Column {
                shown.weeks.forEach { week ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        week.forEach { date ->
                            DayCell(
                                day = state.visibleDay(date, shown),
                                isToday = date == shown.today,
                                isSelected = date == state.selectedDate,
                                isInMonth = YearMonth.from(date) == shown.month,
                                onClick = { onSelect(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun monthTransition(from: YearMonth, to: YearMonth, reduceMotion: Boolean): ContentTransform {
    if (reduceMotion) return EnterTransition.None togetherWith ExitTransition.None
    val direction = if (to > from) 1 else -1
    return (slideInHorizontally { width -> direction * width / SLIDE_DIVISOR } + fadeIn()) togetherWith
        (slideOutHorizontally { width -> -direction * width / SLIDE_DIVISOR } + fadeOut())
}

/** 손가락을 충분히 민 뒤 떼면 다음·이전 달로 간다. 오른쪽에서 왼쪽으로 쓰는 언어에서는 방향이 반대다. */
@Composable
private fun Modifier.monthSwipe(month: YearMonth, isRtl: Boolean, onShowMonth: (YearMonth) -> Unit): Modifier {
    val currentOnShowMonth by rememberUpdatedState(onShowMonth)
    return pointerInput(month, isRtl) {
        val threshold = SWIPE_THRESHOLD.toPx()
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                val towardsNext = if (isRtl) total > 0 else total < 0
                if (abs(total) >= threshold) {
                    currentOnShowMonth(if (towardsNext) month.plusMonths(1) else month.minusMonths(1))
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
 * 날짜 칸. 숫자 아래에 기록 종류 점(최대 3개, 넘치면 +n)과 그날 첫 사진을 둔다. 예정이 있으면 숫자를 링으로
 * 두르고, 예상 일정만 있으면 점선으로, 확신도가 낮으면 흐리게 그린다. 오늘은 옅게, 고른 날은 진하게 채운다.
 */
@Composable
private fun DayCell(
    day: CalendarDay,
    isToday: Boolean,
    isSelected: Boolean,
    isInMonth: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = dayDescription(day, isToday)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = DAY_CELL_MIN_HEIGHT)
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = isSelected, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                selected = isSelected
                onClick {
                    onClick()
                    true
                }
            }
            .alpha(if (isInMonth) 1f else OTHER_MONTH_ALPHA)
            .padding(vertical = ChageunTheme.spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DAY_CONTENT_GAP),
    ) {
        DayNumber(day.date.dayOfMonth, isToday, isSelected, day.upcoming)
        RecordDots(day.records)
        day.photos.firstOrNull()?.let { DayThumbnail(it.thumbnailPath) }
    }
}

@Composable
private fun DayNumber(dayOfMonth: Int, isToday: Boolean, isSelected: Boolean, upcoming: List<UpcomingEvent>) {
    val colors = MaterialTheme.colorScheme
    val ring = upcomingRing(upcoming)
    Box(
        modifier = Modifier
            .defaultMinSize(DAY_NUMBER_SIZE + RING_INSET * 2, DAY_NUMBER_SIZE + RING_INSET * 2)
            .drawBehind {
                ring ?: return@drawBehind
                val stroke = RING_WIDTH.toPx()
                drawCircle(
                    color = ring.color,
                    radius = (minOf(size.width, size.height) - stroke) / 2,
                    center = Offset(size.width / 2, size.height / 2),
                    style = Stroke(
                        width = stroke,
                        pathEffect = if (ring.isDashed) PathEffect.dashPathEffect(floatArrayOf(DASH, DASH)) else null,
                    ),
                )
            }
            .padding(RING_INSET),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(DAY_NUMBER_SIZE, DAY_NUMBER_SIZE)
                .background(
                    when {
                        isSelected -> colors.primary
                        isToday -> colors.primaryContainer
                        else -> Color.Transparent
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> colors.onPrimary
                    isToday -> colors.onPrimaryContainer
                    else -> colors.onSurface
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class Ring(val color: Color, val isDashed: Boolean)

@Composable
private fun upcomingRing(upcoming: List<UpcomingEvent>): Ring? {
    if (upcoming.isEmpty()) return null
    val estimates = upcoming.map { (it as? UpcomingEvent.MaintenanceDue)?.takeIf { due -> due.isEstimated } }
    val onlyEstimates = estimates.all { it != null }
    val tone = if (upcoming.any { it.isOverdue }) ChageunTheme.colors.critical else ChageunTheme.colors.upcoming
    val isLowConfidence = onlyEstimates && estimates.all { it?.confidence == Confidence.Low }
    return Ring(
        color = if (isLowConfidence) tone.content.copy(alpha = LOW_CONFIDENCE_ALPHA) else tone.content,
        isDashed = onlyEstimates,
    )
}

@Composable
private fun RecordDots(records: List<TimelineItem>) {
    if (records.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(DOT_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        records.take(MAX_DOTS).forEach { record ->
            val tone = record.maintenanceItem?.category?.tone() ?: record.ref.type.tone()
            Box(
                Modifier
                    .size(DOT_SIZE)
                    .background(tone.content, CircleShape),
            )
        }
        if (records.size > MAX_DOTS) {
            Text(
                stringResource(R.string.calendar_more, records.size - MAX_DOTS),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayThumbnail(path: String) {
    val image by rememberFileImage(path)
    Box(
        Modifier
            .size(THUMBNAIL_SIZE)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        image?.let {
            Image(
                it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(THUMBNAIL_SIZE),
            )
        }
    }
}

/** TalkBack 문구. 예: "10월 12일 일요일, 오늘, 기록 2개, 사진 1장, 예정 1개". 고른 상태는 selected로 따로 읽힌다. */
@Composable
internal fun dayDescription(day: CalendarDay, isToday: Boolean): String {
    val overdue = day.upcoming.count { it.isOverdue }
    val planned = day.upcoming.size - overdue
    return listOfNotNull(
        formatLongDate(day.date),
        stringResource(R.string.calendar_a11y_today).takeIf { isToday },
        day.records.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.calendar_a11y_records, it, it) },
        day.photos.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.calendar_a11y_photos, it, it) },
        planned.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.calendar_a11y_upcoming, it, it) },
        overdue.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.calendar_a11y_overdue, it, it) },
    ).joinToString(", ")
}

/** 월·일·요일. 예: 한국어 "10월 12일 일요일", 영어 "Sunday, October 12". */
@Composable
internal fun formatLongDate(date: LocalDate): String {
    val locale = currentLocale()
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMdEEEE"), locale))
}

internal val CalendarLayer.labelRes: Int
    get() = when (this) {
        CalendarLayer.Records -> R.string.calendar_layer_records
        CalendarLayer.Photos -> R.string.calendar_layer_photos
        CalendarLayer.Upcoming -> R.string.calendar_layer_upcoming
    }

private val DAY_CELL_MIN_HEIGHT = 64.dp
private val DAY_NUMBER_SIZE = 28.dp
private val RING_INSET = 3.dp
private val RING_WIDTH = 2.dp
private val DAY_CONTENT_GAP = 2.dp
private val DOT_SIZE = 6.dp
private val DOT_GAP = 2.dp
private val THUMBNAIL_SIZE = 20.dp
private val CHIP_ICON_SIZE = 18.dp
private val SWIPE_THRESHOLD = 48.dp
private const val MAX_DOTS = 3
private const val OTHER_MONTH_ALPHA = 0.38f
private const val LOW_CONFIDENCE_ALPHA = 0.45f
private const val DASH = 6f
private const val SLIDE_DIVISOR = 4
