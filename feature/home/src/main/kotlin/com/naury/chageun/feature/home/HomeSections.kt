package com.naury.chageun.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.StatusBadge
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.model.UpcomingEvent
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.TimelineItemIcon
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatMonthDay
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.icon
import com.naury.chageun.core.ui.timelineMileage
import com.naury.chageun.core.ui.timelineTitle
import com.naury.chageun.core.ui.tone
import com.naury.chageun.core.ui.upcomingEventStatus
import com.naury.chageun.core.ui.upcomingEventTitle
import java.time.LocalDate

@Composable
internal fun MileagePromptCard(onUpdate: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressable(onClick = onUpdate),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs),
        ) {
            Text(stringResource(R.string.home_mileage_prompt_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.home_mileage_prompt_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun RecentRecords(records: List<TimelineItem>, onOpenHistory: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_section_recent),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (records.isNotEmpty()) {
                TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.home_recent_all)) }
            }
        }
        if (records.isEmpty()) {
            Text(
                stringResource(R.string.home_recent_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.home_recent_add)) }
        }
        records.forEach { record ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            ) {
                TimelineItemIcon(record)
                Column(Modifier.weight(1f)) {
                    Text(timelineTitle(record), style = MaterialTheme.typography.bodyLarge)
                    val meta = listOfNotNull(
                        record.date?.let { formatDate(it) },
                        timelineMileage(record),
                    )
                    Text(
                        meta.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                record.costWon?.let {
                    Text(
                        stringResource(R.string.home_recent_won, formatNumber(it)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/**
 * Hero 바로 아래 두는 AI 질문 입력줄. Gemini 입력창처럼 생겨서 누르면 바로 묻는 화면으로 간다.
 * 홈 맨 아래에 있으면 거의 보이지 않아 첫 화면에서 보이는 자리로 올렸다.
 */
@Composable
internal fun AskAiBar(onAskAi: () -> Unit, modifier: Modifier = Modifier) {
    val ai = ChageunTheme.colors.ai
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressable(onClick = onAskAi),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            Modifier.padding(start = ChageunTheme.spacing.md, end = ChageunTheme.spacing.xs)
                .padding(vertical = ChageunTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = ai.content,
                modifier = Modifier.size(24.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_ai_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.home_ai_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .background(ai.content, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ai.container)
            }
        }
    }
}

/** 2주 안의 예정 일정. 항목을 누르면 달력의 그날로, [달력에서 보기]를 누르면 오늘로 간다. */
@Composable
internal fun UpcomingScheduleCard(
    events: List<UpcomingEvent>,
    today: LocalDate,
    onOpenCalendar: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_section_schedule),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            TextButton(onClick = { onOpenCalendar(today) }) { Text(stringResource(R.string.home_schedule_calendar)) }
        }
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column {
                events.forEachIndexed { index, event ->
                    if (index > 0) GroupDivider()
                    val (tone, label) = upcomingEventStatus(event)
                    ListRow(
                        icon = event.icon,
                        title = upcomingEventTitle(event),
                        body = if (event.date == today) {
                            stringResource(R.string.home_schedule_today)
                        } else {
                            formatMonthDay(event.date)
                        },
                        tone = event.tone(),
                        onClick = { onOpenCalendar(event.date) },
                        trailing = { StatusBadge(tone, label) },
                    )
                }
            }
        }
    }
}
