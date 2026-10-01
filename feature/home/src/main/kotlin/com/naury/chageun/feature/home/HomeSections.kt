package com.naury.chageun.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.timelineTitle

@Composable
internal fun MileagePromptCard(onUpdate: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onUpdate),
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(timelineTitle(record), style = MaterialTheme.typography.bodyLarge)
                    val meta = listOfNotNull(
                        record.date?.let { formatDate(it) },
                        record.mileage?.let { stringResource(R.string.home_mileage, formatNumber(it.value)) },
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
