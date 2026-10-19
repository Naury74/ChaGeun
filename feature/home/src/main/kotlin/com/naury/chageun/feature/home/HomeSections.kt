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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.ChageunButton
import com.naury.chageun.core.designsystem.component.StatusBadge
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
import com.naury.kinetica.ui.KVariant
import com.naury.kinetica.ui.glowBorder
import com.naury.kinetica.ui.sparkle
import java.time.LocalDate

@Composable
internal fun MileagePromptCard(onUpdate: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressableCard(MaterialTheme.shapes.large, onUpdate),
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
            if (records.isNotEmpty()) TextAction(stringResource(R.string.home_recent_all), onOpenHistory)
        }
        if (records.isEmpty()) {
            Text(
                stringResource(R.string.home_recent_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextAction(stringResource(R.string.home_recent_add), onOpenHistory)
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

@Composable
internal fun AiQuestionCard(onAskAi: () -> Unit, modifier: Modifier = Modifier) {
    val ai = ChageunTheme.colors.ai
    val shape = MaterialTheme.shapes.large
    // Apple Intelligence처럼 가장자리를 따라 색이 천천히 돈다. 번짐이 잘리지 않게 카드 바깥 여백 안에서 그린다.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressableCard(shape, onAskAi)
            .glowBorder(shape, width = AI_GLOW_WIDTH, glow = AI_GLOW_BLUR, intensity = AI_GLOW_INTENSITY),
        shape = shape,
        color = ai.container,
        contentColor = ai.content,
    ) {
        Row(
            Modifier.padding(ChageunTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .sparkle(color = ai.content, count = AI_SPARKLES, starSize = AI_SPARKLE_SIZE)
                    .background(ai.content, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ai.container)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs)) {
                Text(stringResource(R.string.home_ai_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.home_ai_body), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
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
            TextAction(stringResource(R.string.home_schedule_calendar)) { onOpenCalendar(today) }
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

/** 머리글 끝이나 빈 목록 아래에 놓는 글자 버튼. 눈에 보이는 크기는 작아도 누르는 영역은 48dp를 지킨다. */
@Composable
private fun TextAction(text: String, onClick: () -> Unit) {
    ChageunButton(
        text,
        onClick = onClick,
        modifier = Modifier.minimumInteractiveComponentSize(),
        variant = KVariant.Text,
    )
}

private val AI_GLOW_WIDTH = 2.dp
private val AI_GLOW_BLUR = 10.dp
private const val AI_GLOW_INTENSITY = 0.85f
private const val AI_SPARKLES = 4
private val AI_SPARKLE_SIZE = 8.dp
