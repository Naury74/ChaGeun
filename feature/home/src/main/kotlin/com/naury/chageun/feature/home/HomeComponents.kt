package com.naury.chageun.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.StatusBadge
import com.naury.chageun.core.designsystem.component.colors
import com.naury.chageun.core.designsystem.component.icon
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.VehicleHealth
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.missingInputText
import com.naury.chageun.core.ui.remainingText
import com.naury.chageun.core.ui.tone

@Composable
internal fun VehicleStatusSummary(health: VehicleHealth, goodCount: Int, modifier: Modifier = Modifier) {
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
                Text(healthHeadline(health), style = MaterialTheme.typography.titleMedium)
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
    onRecordService: (MaintenanceItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(status.item.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(tone = status.state.tone, label = stringResource(status.state.labelRes))
            }
            remainingText(status)?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            status.estimatedDue?.let {
                Text(
                    stringResource(R.string.home_estimated_due, formatDate(it.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (status.ruleSource == RuleSource.Generic) {
                Text(
                    stringResource(R.string.home_rule_generic),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(
                onClick = { onRecordService(status.item) },
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = ChageunTheme.spacing.minTouchTarget),
            ) { Text(stringResource(R.string.home_record_service)) }
        }
    }
}

@Composable
internal fun MissingInfoRow(status: MaintenanceStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ChageunTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
