package com.naury.chageun.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.StatusBadge
import com.naury.chageun.core.designsystem.component.StatusTone
import com.naury.chageun.core.designsystem.component.colors
import com.naury.chageun.core.designsystem.component.icon
import com.naury.chageun.core.designsystem.motion.ChageunMotion
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.InspectionState
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.VehicleHealth
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.ui.MaintenanceItemIcon
import com.naury.chageun.core.ui.MaintenanceProgressBar
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.missingInputText
import com.naury.chageun.core.ui.remainingText
import com.naury.chageun.core.ui.tone
import com.naury.chageun.core.ui.usedFraction
import kotlin.math.absoluteValue

@Composable
internal fun VehicleStatusSummary(
    health: VehicleHealth,
    goodCount: Int,
    attentionCount: Int,
    modifier: Modifier = Modifier,
) {
    val tone = health.level.tone
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = tone.colors.container,
        contentColor = tone.colors.content,
    ) {
        Row(
            modifier = Modifier.padding(ChageunTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(tone.icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs)) {
                Text(healthHeadline(health, attentionCount), style = MaterialTheme.typography.titleMedium)
                val detail = when (health.level) {
                    VehicleHealthLevel.Good ->
                        pluralStringResource(R.plurals.home_health_good_detail, goodCount, goodCount)
                    VehicleHealthLevel.InsufficientData -> stringResource(R.string.home_health_insufficient_detail)
                    else -> null
                }
                detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MaintenanceStatusCard(
    status: MaintenanceStatus,
    rule: MaintenanceRule?,
    onRecordService: (MaintenanceItem) -> Unit,
    onOpenDetail: (MaintenanceItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = status.state.tone.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            ) {
                MaintenanceItemIcon(status.item)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(status.item.labelRes), style = MaterialTheme.typography.titleMedium)
                    remainingText(status)?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                StatusBadge(tone = status.state.tone, label = stringResource(status.state.labelRes))
            }
            usedFraction(status, rule)?.let { fraction ->
                MaintenanceProgressBar(fraction, tone.content)
            }
            progressCaption(status, rule)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                OutlinedButton(
                    onClick = { onOpenDetail(status.item) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ChageunTheme.spacing.minTouchTarget),
                ) { Text(stringResource(R.string.home_open_detail)) }
                Button(
                    onClick = { onRecordService(status.item) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ChageunTheme.spacing.minTouchTarget),
                ) { Text(stringResource(R.string.home_record_service)) }
            }
        }
    }
}

/** 막대 아래의 '다음 교체까지 1,920 / 10,000 km'. 거리 기준이 없으면 남은 일수와 주기로 보여 준다. */
@Composable
private fun progressCaption(status: MaintenanceStatus, rule: MaintenanceRule?): String? {
    val km = status.remainingKm
    val intervalKm = rule?.intervalKm
    if (km != null && km > 0 && intervalKm != null) {
        return stringResource(R.string.home_progress_km, formatNumber(km), formatNumber(intervalKm))
    }
    val generic = status.ruleSource == RuleSource.Generic
    val estimated = status.estimatedDue?.let { stringResource(R.string.home_estimated_due, formatDate(it.date)) }
    return listOfNotNull(estimated, stringResource(R.string.home_rule_generic).takeIf { generic })
        .joinToString(" · ")
        .ifEmpty { null }
}

/**
 * 처음 쓰는 사람은 거의 모든 항목이 '정보 부족'이라 목록이 길다. 몇 개만 먼저 보여 주고 나머지는 펼쳐 본다.
 */
@Composable
internal fun MissingInfoCard(
    statuses: List<MaintenanceStatus>,
    onOpenDetail: (MaintenanceItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val shown = if (isExpanded) statuses else statuses.take(MISSING_PREVIEW)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(motionSpec(ChageunMotion.MEDIUM_MS)),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(vertical = ChageunTheme.spacing.xs)) {
            shown.forEach { status ->
                MissingInfoRow(status, onOpenDetail, Modifier.padding(horizontal = ChageunTheme.spacing.md))
            }
            if (statuses.size > MISSING_PREVIEW) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ChageunTheme.spacing.xs),
                ) {
                    Text(
                        if (isExpanded) {
                            stringResource(R.string.home_missing_less)
                        } else {
                            pluralStringResource(
                                R.plurals.home_missing_more,
                                statuses.size - MISSING_PREVIEW,
                                statuses.size - MISSING_PREVIEW,
                            )
                        },
                    )
                    Icon(
                        if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

private const val MISSING_PREVIEW = 3

@Composable
internal fun MissingInfoRow(
    status: MaintenanceStatus,
    onOpenDetail: (MaintenanceItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button) { onOpenDetail(status.item) }
            .padding(vertical = ChageunTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MaintenanceItemIcon(status.item, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(stringResource(status.item.labelRes), style = MaterialTheme.typography.bodyLarge)
            Text(
                missingInputText(status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusBadge(tone = status.state.tone, label = stringResource(status.state.labelRes))
    }
}

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .padding(top = ChageunTheme.spacing.xs)
            .semantics { heading() },
    )
}

/** 검사는 여기서 기록할 정비가 없다. 카드를 누르면 검사일을 관리하는 My car로 이동한다. */
@Composable
internal fun InspectionStatusCard(status: InspectionStatus, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val schedule = status.schedule ?: return
    val days = status.daysLeft ?: return
    val count = days.absoluteValue.toInt()
    val tone = if (status.state == InspectionState.Overdue) StatusTone.Critical else StatusTone.Upcoming
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_inspection_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(
                    tone = tone,
                    label = stringResource(
                        if (tone ==
                            StatusTone.Critical
                        ) {
                            R.string.home_inspection_overdue
                        } else {
                            R.string.home_inspection_due_soon
                        },
                    ),
                )
            }
            Text(
                when {
                    days < 0 -> pluralStringResource(R.plurals.home_inspection_days_overdue, count, count)
                    days == 0L -> stringResource(R.string.home_inspection_due_today)
                    else -> pluralStringResource(R.plurals.home_inspection_days_left, count, count)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.home_inspection_due_date, formatDate(schedule.nextDueDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
