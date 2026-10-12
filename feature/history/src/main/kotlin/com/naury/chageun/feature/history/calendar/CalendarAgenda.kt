package com.naury.chageun.feature.history.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.StatusBadge
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.calendar.SearchCalendarRecordsUseCase
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.CalendarDay
import com.naury.chageun.core.model.RecordPhoto
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.EmptyState
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.formatYearMonth
import com.naury.chageun.core.ui.icon
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.rememberFileImage
import com.naury.chageun.core.ui.timelineMileage
import com.naury.chageun.core.ui.timelineTitle
import com.naury.chageun.core.ui.tone
import com.naury.chageun.core.ui.upcomingEventConfidence
import com.naury.chageun.core.ui.upcomingEventStatus
import com.naury.chageun.core.ui.upcomingEventTitle
import com.naury.chageun.feature.history.R
import java.time.YearMonth

/**
 * 고른 날의 목록. 검색 중이면 검색 결과를 먼저 두고, 그 아래에 예정 → 기록 → 사진 순으로 둔다.
 * 오늘이면 앞으로 30일 예정을 이어 붙인다.
 */
internal fun LazyListScope.calendarAgenda(state: CalendarUiState, actions: CalendarActions) {
    searchResults(state, actions)
    val day = state.selectedDay ?: return
    val today = state.calendar?.today
    val canAdd = today != null && !day.date.isAfter(today)
    item(key = "agenda-title") {
        Text(
            formatLongDate(day.date),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .agendaPadding()
                .padding(top = ChageunTheme.spacing.sm)
                .semantics { heading() },
        )
    }
    if (day.isEmpty) {
        item(key = "agenda-empty") {
            EmptyState(
                icon = if (canAdd) Icons.AutoMirrored.Filled.EventNote else Icons.Filled.EventAvailable,
                title = stringResource(if (canAdd) R.string.calendar_empty_day else R.string.calendar_empty_future_day),
                actionLabel = stringResource(R.string.calendar_add_on_day).takeIf { canAdd },
                onAction = { actions.onAddOnDate(day.date) }.takeIf { canAdd },
            )
        }
    }
    dayItems(day, actions)
    if (canAdd && !day.isEmpty) {
        item(key = "agenda-add") { AddOnDayButton { actions.onAddOnDate(day.date) } }
    }
    val soon = state.visibleUpcomingSoon
    if (soon.isNotEmpty()) {
        item(key = "agenda-soon") {
            UpcomingGroup(R.string.calendar_section_next_days, soon, showDate = true, actions.onOpenUpcoming)
        }
    }
}

private fun LazyListScope.dayItems(day: CalendarDay, actions: CalendarActions) {
    if (day.upcoming.isNotEmpty()) {
        item(key = "agenda-upcoming") {
            UpcomingGroup(R.string.calendar_section_upcoming, day.upcoming, showDate = false, actions.onOpenUpcoming)
        }
    }
    if (day.records.isNotEmpty()) {
        item(key = "agenda-records") { RecordGroup(day.records, actions.onOpenRecord) }
    }
    if (day.photoCount > 0) {
        item(key = "agenda-photos") { PhotoGroup(day, actions) }
    }
}

/** 검색 결과를 달마다 묶는다. 결과를 누르면 그 달·그날로 간다. */
private fun LazyListScope.searchResults(state: CalendarUiState, actions: CalendarActions) {
    if (!state.search.isActive) return
    val results = state.searchResults
    item(key = "search-title") {
        Text(
            pluralStringResource(R.plurals.calendar_search_results, results.size, results.size),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .agendaPadding()
                .padding(top = ChageunTheme.spacing.sm)
                .semantics { heading() },
        )
    }
    if (results.isEmpty()) {
        item(key = "search-empty") {
            EmptyState(icon = Icons.Filled.SearchOff, title = stringResource(R.string.calendar_search_empty))
        }
    }
    results.groupBy { record -> record.date?.let(YearMonth::from) }.forEach { (month, records) ->
        item(key = "search-$month") { SearchResultGroup(month, records, actions.onOpenResult) }
    }
    if (state.search.results.size >= SearchCalendarRecordsUseCase.MAX_RESULTS) {
        item(key = "search-limit") {
            Text(
                stringResource(R.string.calendar_search_limit, SearchCalendarRecordsUseCase.MAX_RESULTS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.agendaPadding(),
            )
        }
    }
}

@Composable
private fun Modifier.agendaPadding(): Modifier = padding(horizontal = ChageunTheme.spacing.gutter)

@Composable
private fun SearchResultGroup(month: YearMonth?, records: List<TimelineItem>, onOpen: (TimelineItem) -> Unit) {
    CardGroup(month?.let { formatYearMonth(it) }, Modifier.agendaPadding()) {
        records.forEachIndexed { index, record ->
            if (index > 0) GroupDivider()
            val cost = record.costWon?.let { stringResource(R.string.history_won, formatNumber(it)) }
            ListRow(
                icon = record.maintenanceItem?.icon ?: record.ref.type.icon,
                title = timelineTitle(record),
                body = listOfNotNull(record.date?.let { formatDate(it) }, cost).joinToString(" · "),
                tone = record.maintenanceItem?.category?.tone() ?: record.ref.type.tone(),
                onClick = { onOpen(record) },
            )
        }
    }
}

@Composable
private fun UpcomingGroup(
    titleRes: Int,
    events: List<UpcomingEvent>,
    showDate: Boolean,
    onOpen: (UpcomingEvent) -> Unit,
) {
    CardGroup(stringResource(titleRes), Modifier.agendaPadding()) {
        events.forEachIndexed { index, event ->
            if (index > 0) GroupDivider()
            val (tone, label) = upcomingEventStatus(event)
            val body = listOfNotNull(
                formatDate(event.date).takeIf { showDate },
                upcomingEventConfidence(event),
            ).joinToString(" · ").ifEmpty { null }
            ListRow(
                icon = event.icon,
                title = upcomingEventTitle(event),
                body = body,
                tone = event.tone(),
                onClick = { onOpen(event) },
                trailing = { StatusBadge(tone, label) },
            )
        }
    }
}

@Composable
private fun RecordGroup(records: List<TimelineItem>, onOpen: (RecordRef) -> Unit) {
    CardGroup(stringResource(R.string.calendar_section_records), Modifier.agendaPadding()) {
        records.forEachIndexed { index, record ->
            if (index > 0) GroupDivider()
            val cost = record.costWon?.let { stringResource(R.string.history_won, formatNumber(it)) }
            ListRow(
                icon = record.maintenanceItem?.icon ?: record.ref.type.icon,
                title = timelineTitle(record),
                body = listOfNotNull(timelineMileage(record), cost).joinToString(" · ").ifEmpty { null },
                tone = record.maintenanceItem?.category?.tone() ?: record.ref.type.tone(),
                onClick = { onOpen(record.ref) },
                trailing = if (record.soundCount > 0) {
                    { SoundTrailing(record.soundCount) }
                } else {
                    null
                },
            )
        }
    }
}

/** 앨범 사진을 먼저, 기록에 붙인 사진을 뒤에 둔다. 첨부 사진은 영수증 표시를 달고 누르면 그 기록을 연다. */
@Composable
private fun PhotoGroup(day: CalendarDay, actions: CalendarActions) {
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Text(
            stringResource(R.string.calendar_section_photos),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .agendaPadding()
                .padding(top = ChageunTheme.spacing.xs)
                .semantics { heading() },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .agendaPadding(),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            day.photos.forEach { photo ->
                AgendaPhoto(photo.thumbnailPath, albumPhotoDescription(photo), isVideo = photo.isVideo) {
                    actions.onOpenPhoto(photo)
                }
            }
            day.recordPhotos.forEach { photo ->
                AgendaPhoto(photo.attachment.thumbnailPath, recordPhotoDescription(photo, day), isRecordPhoto = true) {
                    actions.onOpenRecord(photo.attachment.owner)
                }
            }
        }
    }
}

/** 동영상은 앞에 "동영상"을 붙여 사진과 구분해 읽힌다. */
@Composable
private fun albumPhotoDescription(photo: AlbumPhoto): String = when {
    !photo.isVideo -> photo.comment ?: stringResource(R.string.calendar_photo_on, formatDate(photo.takenOn))
    photo.comment != null -> stringResource(R.string.calendar_video_named, photo.comment.orEmpty())
    else -> stringResource(R.string.calendar_video_on, formatDate(photo.takenOn))
}

@Composable
private fun recordPhotoDescription(photo: RecordPhoto, day: CalendarDay): String {
    val owner = photo.attachment.owner
    val record = day.records.firstOrNull { it.ref == owner }
    val title = record?.let { timelineTitle(it) } ?: stringResource(owner.type.labelRes)
    return stringResource(R.string.calendar_record_photo, title)
}

@Composable
private fun AgendaPhoto(
    path: String,
    description: String,
    isRecordPhoto: Boolean = false,
    isVideo: Boolean = false,
    onClick: () -> Unit,
) {
    val image by rememberFileImage(path)
    Box(
        Modifier
            .size(PHOTO_SIZE)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pressable(onClick = onClick, role = Role.Image)
            .semantics { contentDescription = description },
    ) {
        image?.let {
            Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (isRecordPhoto) {
            RecordPhotoMark(
                size = RECEIPT_MARK_SIZE,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(ChageunTheme.spacing.xxs),
            )
        }
        if (isVideo) VideoMark(size = PLAY_MARK_SIZE, modifier = Modifier.align(Alignment.Center))
    }
}

/** 동영상이라는 표시. 사진 위에 겹쳐도 보이도록 어두운 원에 흰 재생 표시를 그린다. 설명은 사진 쪽에서 읽어 준다. */
@Composable
internal fun VideoMark(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .background(VIDEO_MARK_FILL, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * PLAY_ICON_RATIO),
        )
    }
}

/** 기록에 녹음한 소리가 붙었다는 표시와 여는 화살표. 개수는 "소리 n개 첨부"로 읽힌다. */
@Composable
private fun SoundTrailing(count: Int) {
    val description = pluralStringResource(R.plurals.history_sound_count, count, count)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.GraphicEq,
                contentDescription = null,
                tint = muted,
                modifier = Modifier.size(SOUND_ICON),
            )
            Text(count.toString(), style = MaterialTheme.typography.bodySmall, color = muted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = muted)
    }
}

/** 기록에 붙인 사진(영수증 등)이라는 표시. 설명은 사진 쪽에서 읽어 주므로 따로 두지 않는다. */
@Composable
internal fun RecordPhotoMark(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Receipt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(size * RECEIPT_ICON_RATIO),
        )
    }
}

@Composable
private fun AddOnDayButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .agendaPadding()
            .heightIn(min = ChageunTheme.spacing.minTouchTarget),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(ADD_ICON_SIZE))
            Text(
                stringResource(R.string.calendar_add_on_day),
                modifier = Modifier.padding(start = ChageunTheme.spacing.xs),
            )
        }
    }
}

private val PHOTO_SIZE = 88.dp
private val ADD_ICON_SIZE = 18.dp
private val RECEIPT_MARK_SIZE = 24.dp
private const val RECEIPT_ICON_RATIO = 0.7f
private val PLAY_MARK_SIZE = 32.dp
private val SOUND_ICON = 16.dp
private val VIDEO_MARK_FILL = Color.Black.copy(alpha = 0.55f)
private const val PLAY_ICON_RATIO = 0.75f
