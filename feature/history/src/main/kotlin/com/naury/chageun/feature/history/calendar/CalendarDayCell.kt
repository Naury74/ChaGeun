package com.naury.chageun.feature.history.calendar

import android.text.format.DateFormat
import androidx.annotation.PluralsRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.CalendarDay
import com.naury.chageun.core.model.Confidence
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.ui.currentLocale
import com.naury.chageun.core.ui.icon
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.rememberFileImage
import com.naury.chageun.core.ui.tone
import com.naury.chageun.core.ui.upcomingEventTitle
import com.naury.chageun.feature.history.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 날짜 칸 크기. 주 보기는 한 줄만 그리므로 칸이 높고 아이콘·사진을 더 크게, 더 많이 보여 준다. */
internal enum class CellStyle(
    val maxMarks: Int,
    val marksPerRow: Int,
    val markSize: Dp,
    val thumbnailHeight: Dp,
    /** 넓은 화면에서 칸이 넓어도 사진이 가는 띠처럼 늘어나지 않게 폭을 제한한다. */
    val thumbnailMaxWidth: Dp,
    val minHeight: Dp,
) {
    Month(
        MONTH_MAX_MARKS,
        MONTH_MAX_MARKS,
        MONTH_MARK_SIZE,
        MONTH_THUMBNAIL_HEIGHT,
        MONTH_THUMBNAIL_MAX_WIDTH,
        MONTH_CELL_MIN_HEIGHT,
    ),
    Week(
        WEEK_MAX_MARKS,
        WEEK_MARKS_PER_ROW,
        WEEK_MARK_SIZE,
        WEEK_THUMBNAIL_HEIGHT,
        WEEK_THUMBNAIL_MAX_WIDTH,
        WEEK_CELL_MIN_HEIGHT,
    ),
}

/**
 * 날짜 칸이 그린 아이콘 수와 사진 미리보기 여부. 칸 안의 내용은 TalkBack 문구 하나로 합쳐 읽히므로
 * 테스트는 이 값으로 무엇이 그려졌는지 확인한다.
 */
internal val DayMarksShown = SemanticsPropertyKey<Int>("DayMarksShown")
internal val DayHasThumbnail = SemanticsPropertyKey<Boolean>("DayHasThumbnail")
private var SemanticsPropertyReceiver.marksShown by DayMarksShown
private var SemanticsPropertyReceiver.hasThumbnail by DayHasThumbnail

/** 검색 중일 때 날짜 칸 강조. 결과가 있는 날은 테두리를 두르고, 없는 날은 흐리게 그린다. */
internal enum class SearchMatch { None, Match, Dimmed }

/**
 * 날짜 칸. 숫자 아래에 예정(링을 두른 항목 아이콘)과 기록(종류 아이콘)을 칸에 들어가는 만큼 두고, 넘치면 +n을 붙인다.
 * 사진이 있으면 그날 첫 사진을 칸 아래쪽에 채우고 여러 장이면 개수를 단다. 오늘은 옅게, 고른 날은 진하게 채운다.
 */
@Composable
internal fun DayCell(
    day: CalendarDay,
    isToday: Boolean,
    isSelected: Boolean,
    isInMonth: Boolean,
    style: CellStyle,
    match: SearchMatch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = dayDescription(day, isToday, isMatch = match == SearchMatch.Match)
    val shape = MaterialTheme.shapes.small
    Column(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = style.minHeight)
            .clip(shape)
            .then(
                if (match == SearchMatch.Match) {
                    Modifier.border(MATCH_BORDER, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                },
            )
            .selectable(selected = isSelected, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                selected = isSelected
                marksShown = minOf(day.upcoming.size + day.records.size, style.maxMarks)
                hasThumbnail = day.photoCount > 0
                onClick {
                    onClick()
                    true
                }
            }
            .alpha(
                when {
                    match == SearchMatch.Dimmed -> DIMMED_ALPHA
                    isInMonth -> 1f
                    else -> OTHER_MONTH_ALPHA
                },
            )
            .padding(horizontal = CELL_PADDING, vertical = ChageunTheme.spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DAY_CONTENT_GAP),
    ) {
        DayNumber(day.date.dayOfMonth, isToday, isSelected)
        DayMarks(day, style)
        if (day.photoCount > 0) {
            Spacer(Modifier.weight(1f))
            DayThumbnail(day, style)
        }
    }
}

@Composable
private fun DayNumber(dayOfMonth: Int, isToday: Boolean, isSelected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .padding(NUMBER_INSET)
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

/** 예정을 먼저, 기록을 뒤에 둔다. 칸에 들어가는 만큼만 그리고 나머지는 +n으로 센다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayMarks(day: CalendarDay, style: CellStyle) {
    val total = day.upcoming.size + day.records.size
    if (total == 0) return
    val upcoming = day.upcoming.take(style.maxMarks)
    val records = day.records.take(style.maxMarks - upcoming.size)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MARK_GAP, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(MARK_GAP),
        maxItemsInEachRow = style.marksPerRow,
    ) {
        upcoming.forEach { UpcomingMark(it, style.markSize) }
        records.forEach { RecordMark(it, style.markSize) }
    }
    val extra = total - upcoming.size - records.size
    if (extra > 0) {
        Text(
            stringResource(R.string.calendar_more, extra),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 기록 종류 아이콘. 정비는 항목 아이콘, 나머지는 주유기·점검·수리·메모·계기판 아이콘이다. */
@Composable
private fun RecordMark(record: TimelineItem, size: Dp) {
    val tone = record.maintenanceItem?.category?.tone() ?: record.ref.type.tone()
    Box(
        Modifier
            .size(size)
            .background(tone.container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            record.maintenanceItem?.icon ?: record.ref.type.icon,
            contentDescription = null,
            tint = tone.content,
            modifier = Modifier.size(size * RECORD_ICON_RATIO),
        )
    }
}

/**
 * 예정 아이콘. 확정 일정은 실선, 예상 일정은 점선 링을 두르고, 확신도가 낮으면 흐리게 그린다.
 * 지난 일은 빨간 링에 점을 더해 색을 구분하기 어려워도 알아볼 수 있게 한다.
 */
@Composable
private fun UpcomingMark(event: UpcomingEvent, markSize: Dp) {
    val due = event as? UpcomingEvent.MaintenanceDue
    val isEstimated = due?.isEstimated == true && !event.isOverdue
    val tone = if (event.isOverdue) ChageunTheme.colors.critical else ChageunTheme.colors.upcoming
    val color = tone.content.copy(alpha = if (due?.confidence == Confidence.Low) LOW_CONFIDENCE_ALPHA else 1f)
    Box(
        modifier = Modifier
            .size(markSize)
            .drawWithContent {
                drawContent()
                val stroke = RING_WIDTH.toPx()
                drawCircle(
                    color = color,
                    radius = (markSize.toPx() - stroke) / 2,
                    style = Stroke(
                        width = stroke,
                        pathEffect = if (isEstimated) PathEffect.dashPathEffect(floatArrayOf(DASH, DASH)) else null,
                    ),
                )
                if (event.isOverdue) {
                    val dot = OVERDUE_DOT.toPx()
                    drawCircle(color = color, radius = dot, center = Offset(size.width - dot, dot))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            event.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(markSize * UPCOMING_ICON_RATIO),
        )
    }
}

/** 그날 첫 사진. 앨범 사진이 없으면 기록 첨부 사진을 쓰고 영수증 표시를 단다. 여러 장이면 개수를 단다. */
@Composable
private fun DayThumbnail(day: CalendarDay, style: CellStyle) {
    val album = day.photos.firstOrNull()
    val path = album?.thumbnailPath ?: day.recordPhotos.firstOrNull()?.attachment?.thumbnailPath ?: return
    val image by rememberFileImage(path)
    Box(
        Modifier
            .widthIn(max = style.thumbnailMaxWidth)
            .fillMaxWidth()
            .heightIn(min = style.thumbnailHeight)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        image?.let {
            Image(
                it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        if (album == null) {
            RecordPhotoMark(size = style.markSize, modifier = Modifier.align(Alignment.TopStart))
        }
        if (day.photoCount > 1) {
            Text(
                day.photoCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.inverseSurface, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = BADGE_PADDING),
            )
        }
    }
}

/**
 * TalkBack 문구. 개수 뒤에 종류 이름을 덧붙인다.
 * 예: "10월 8일 목요일, 기록 2개(엔진오일 교체, 주유), 사진 1장, 예정 1개(정기검사)". 고른 상태는 selected로 따로 읽힌다.
 */
@Composable
internal fun dayDescription(day: CalendarDay, isToday: Boolean, isMatch: Boolean = false): String {
    val overdue = day.upcoming.filter { it.isOverdue }
    val planned = day.upcoming.filterNot { it.isOverdue }
    return listOfNotNull(
        formatLongDate(day.date),
        stringResource(R.string.calendar_a11y_today).takeIf { isToday },
        countWithNames(R.plurals.calendar_a11y_records, day.records.map { recordName(it) }),
        day.photoCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.calendar_a11y_photos, it, it) },
        countWithNames(R.plurals.calendar_a11y_upcoming, planned.map { upcomingEventTitle(it) }),
        countWithNames(R.plurals.calendar_a11y_overdue, overdue.map { upcomingEventTitle(it) }),
        stringResource(R.string.calendar_a11y_match).takeIf { isMatch },
    ).joinToString(", ")
}

@Composable
private fun countWithNames(@PluralsRes res: Int, names: List<String>): String? {
    if (names.isEmpty()) return null
    val counted = pluralStringResource(res, names.size, names.size)
    return stringResource(R.string.calendar_a11y_names, counted, names.distinct().joinToString(", "))
}

@Composable
private fun recordName(record: TimelineItem): String {
    val item = record.maintenanceItem ?: return stringResource(record.ref.type.labelRes)
    return stringResource(R.string.calendar_a11y_replacement, stringResource(item.labelRes))
}

/** 월·일·요일. 예: 한국어 "10월 12일 일요일", 영어 "Sunday, October 12". */
@Composable
internal fun formatLongDate(date: LocalDate): String {
    val locale = currentLocale()
    return date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMdEEEE"), locale))
}

private const val MONTH_MAX_MARKS = 2
private const val WEEK_MAX_MARKS = 4
private const val WEEK_MARKS_PER_ROW = 2
private val MONTH_MARK_SIZE = 16.dp
private val WEEK_MARK_SIZE = 20.dp
private val MONTH_THUMBNAIL_HEIGHT = 18.dp
private val WEEK_THUMBNAIL_HEIGHT = 40.dp
private val MONTH_THUMBNAIL_MAX_WIDTH = 64.dp
private val WEEK_THUMBNAIL_MAX_WIDTH = 96.dp
private val MONTH_CELL_MIN_HEIGHT = 64.dp
private val WEEK_CELL_MIN_HEIGHT = 128.dp
private val DAY_NUMBER_SIZE = 28.dp
private val NUMBER_INSET = 3.dp
private val CELL_PADDING = 2.dp
private val RING_WIDTH = 1.5.dp
private val OVERDUE_DOT = 2.5.dp
private val DAY_CONTENT_GAP = 2.dp
private val MARK_GAP = 2.dp
private val MATCH_BORDER = 2.dp
private val BADGE_PADDING = 3.dp
private const val RECORD_ICON_RATIO = 0.7f
private const val UPCOMING_ICON_RATIO = 0.6f
private const val OTHER_MONTH_ALPHA = 0.38f
private const val DIMMED_ALPHA = 0.3f
private const val LOW_CONFIDENCE_ALPHA = 0.45f
private const val DASH = 4f
