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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.StatusBadge
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.EmptyState
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.icon
import com.naury.chageun.core.ui.rememberFileImage
import com.naury.chageun.core.ui.timelineMileage
import com.naury.chageun.core.ui.timelineTitle
import com.naury.chageun.core.ui.tone
import com.naury.chageun.core.ui.upcomingEventConfidence
import com.naury.chageun.core.ui.upcomingEventStatus
import com.naury.chageun.core.ui.upcomingEventTitle
import com.naury.chageun.feature.history.R
import java.time.LocalDate
import java.time.YearMonth

/** 달력에서 하는 일. 이동할 곳은 앱 셸이 정한다. */
data class CalendarActions(
    val onSelectDate: (LocalDate) -> Unit = {},
    val onShowMonth: (YearMonth) -> Unit = {},
    val onToday: () -> Unit = {},
    val onToggleLayer: (CalendarLayer) -> Unit = {},
    val onOpenUpcoming: (UpcomingEvent) -> Unit = {},
    val onOpenRecord: (RecordRef) -> Unit = {},
    val onOpenPhoto: (AlbumPhoto) -> Unit = {},
    /** 지난 날과 오늘에만 쓴다. 앞으로의 날에는 기록을 남길 수 없다. */
    val onAddOnDate: (LocalDate) -> Unit = {},
)

/** 고른 날의 목록. 예정 → 기록 → 사진 순이고, 오늘이면 앞으로 30일 예정을 이어 붙인다. */
internal fun LazyListScope.calendarAgenda(state: CalendarUiState, actions: CalendarActions) {
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
    if (day.upcoming.isNotEmpty()) {
        item(key = "agenda-upcoming") {
            UpcomingGroup(R.string.calendar_section_upcoming, day.upcoming, showDate = false, actions.onOpenUpcoming)
        }
    }
    if (day.records.isNotEmpty()) {
        item(key = "agenda-records") { RecordGroup(day.records, actions.onOpenRecord) }
    }
    if (day.photos.isNotEmpty()) {
        item(key = "agenda-photos") { PhotoGroup(day.photos, actions.onOpenPhoto) }
    }
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

@Composable
private fun Modifier.agendaPadding(): Modifier = padding(horizontal = ChageunTheme.spacing.gutter)

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
            )
        }
    }
}

@Composable
private fun PhotoGroup(photos: List<AlbumPhoto>, onOpen: (AlbumPhoto) -> Unit) {
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
            photos.forEach { photo -> AgendaPhoto(photo) { onOpen(photo) } }
        }
    }
}

@Composable
private fun AgendaPhoto(photo: AlbumPhoto, onClick: () -> Unit) {
    val image by rememberFileImage(photo.thumbnailPath)
    val description = photo.comment ?: stringResource(R.string.calendar_photo_on, formatDate(photo.takenOn))
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
