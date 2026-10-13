package com.naury.chageun.feature.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
 * 다른 카드와 구별되도록 AI 화면과 같은 그라데이션으로 테두리·아이콘·버튼을 칠하고 바탕에도 옅게 깐다.
 */
@Composable
internal fun AskAiBar(onAskAi: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressable(onClick = onAskAi),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, AskAiGradient),
    ) {
        Row(
            Modifier
                .background(AskAiGradient, alpha = ASK_AI_TINT_ALPHA)
                .padding(start = ChageunTheme.spacing.md, end = ChageunTheme.spacing.xs)
                .padding(vertical = ChageunTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(AskAiGradient, blendMode = BlendMode.SrcAtop)
                        }
                    },
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_ai_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
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
                    .background(AskAiGradient, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }
    }
}

// AI 화면(feature:ai)의 그라데이션과 같은 색이다. 홈에서 눌러 들어가도 같은 기능으로 느껴지게 맞춘다.
private val AskAiGradient = Brush.linearGradient(listOf(Color(0xFF4F7DF3), Color(0xFF9B72CB), Color(0xFFD96570)))
private const val ASK_AI_TINT_ALPHA = 0.08f

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
