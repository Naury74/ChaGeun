package com.naury.chageun.feature.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.naury.chageun.core.designsystem.component.StatusBadge
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.NumericTextStyles
import com.naury.chageun.core.model.Confidence
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.ServiceHistoryEntry
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.missingInputText
import com.naury.chageun.core.ui.remainingText
import com.naury.chageun.core.ui.tone

@Composable
internal fun ManageDetailPane(
    detail: ManageDetail,
    onBack: (() -> Unit)?,
    onRecordService: (MaintenanceItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = detail.status
    val gutter = ChageunTheme.spacing.gutter
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = gutter, vertical = ChageunTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                onBack?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.manage_back),
                        )
                    }
                }
                Text(
                    stringResource(status.item.labelRes),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                StatusBadge(tone = status.state.tone, label = stringResource(status.state.labelRes))
            }
        }
        item(key = "remaining") {
            val headline = if (status.state ==
                MaintenanceState.Unknown
            ) {
                missingInputText(status)
            } else {
                remainingText(status)
            }
            headline?.let { Text(it, style = NumericTextStyles.Title) }
        }
        item(key = "record") {
            Button(
                onClick = { onRecordService(status.item) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ChageunTheme.spacing.minTouchTarget),
            ) { Text(stringResource(R.string.record_action)) }
        }
        item(key = "basis") { BasisSection(detail) }
        item(key = "history-title") {
            Text(
                stringResource(R.string.manage_history),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(top = ChageunTheme.spacing.sm)
                    .semantics { heading() },
            )
        }
        if (detail.history.isEmpty()) {
            item(key = "history-empty") {
                Text(
                    stringResource(R.string.manage_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(detail.history, key = { it.id }) { HistoryRow(it) }
    }
}

@Composable
private fun BasisSection(detail: ManageDetail) {
    val status = detail.status
    val lastService = detail.history.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        BasisRow(
            label = stringResource(R.string.manage_last_service),
            value = lastService?.let { serviceSummary(it) } ?: stringResource(R.string.manage_last_service_none),
        )
        detail.rule?.let { BasisRow(stringResource(R.string.manage_interval), intervalText(it)) }
        val nextParts = listOfNotNull(
            status.distanceDue?.let { stringResource(R.string.manage_km, formatNumber(it.value)) },
            status.dateDue?.let { formatDate(it) },
        )
        if (nextParts.isNotEmpty()) {
            BasisRow(stringResource(R.string.manage_next_threshold), nextParts.joinToString(" / "))
        }
        status.estimatedDue?.let {
            BasisRow(
                stringResource(R.string.manage_estimated_due),
                stringResource(
                    R.string.manage_estimate_with_confidence,
                    formatDate(it.date),
                    stringResource(it.confidence.labelRes),
                ),
            )
        }
        BasisRow(stringResource(R.string.manage_rule_source), stringResource(status.ruleSource.labelRes))
    }
}

@Composable
private fun BasisRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider()
        Row(Modifier.padding(vertical = ChageunTheme.spacing.xs)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(BASIS_VALUE_WEIGHT),
            )
        }
    }
}

@Composable
private fun HistoryRow(entry: ServiceHistoryEntry) {
    Column(Modifier.fillMaxWidth()) {
        Text(serviceSummary(entry), style = MaterialTheme.typography.bodyLarge)
        val extras = listOfNotNull(
            entry.costWon?.let { stringResource(R.string.manage_cost_won, formatNumber(it)) },
            entry.shopName,
        )
        if (extras.isNotEmpty()) {
            Text(
                extras.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun serviceSummary(entry: ServiceHistoryEntry): String {
    val date = entry.date?.let { formatDate(it) } ?: stringResource(R.string.manage_unknown_date)
    val mileage = entry.mileage?.let { stringResource(R.string.manage_km, formatNumber(it.value)) }
    return mileage?.let { stringResource(R.string.manage_value_or, date, it) } ?: date
}

@Composable
private fun intervalText(rule: MaintenanceRule): String {
    val km = rule.intervalKm
    val months = rule.intervalMonths
    return when {
        km != null && months != null ->
            pluralStringResource(
                R.plurals.manage_interval_km_or_months,
                months.toInt(),
                formatNumber(km),
                months.toInt(),
            )
        km != null -> stringResource(R.string.manage_interval_km, formatNumber(km))
        months != null -> pluralStringResource(R.plurals.manage_interval_months, months.toInt(), months.toInt())
        else -> ""
    }
}

@Composable
internal fun DetailPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(ChageunTheme.spacing.xl), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.manage_select_item),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private const val BASIS_VALUE_WEIGHT = 1.5f

private val RuleSource.labelRes: Int
    get() = when (this) {
        RuleSource.Generic -> R.string.manage_rule_source_generic
        RuleSource.Manufacturer -> R.string.manage_rule_source_manufacturer
        RuleSource.User -> R.string.manage_rule_source_user
    }

private val Confidence.labelRes: Int
    get() = when (this) {
        Confidence.High -> R.string.manage_confidence_high
        Confidence.Medium -> R.string.manage_confidence_medium
        Confidence.Low -> R.string.manage_confidence_low
    }
