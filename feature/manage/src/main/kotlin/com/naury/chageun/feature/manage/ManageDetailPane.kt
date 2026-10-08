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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.StatusBadge
import com.naury.chageun.core.designsystem.component.colors
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.NumericTextStyles
import com.naury.chageun.core.model.Confidence
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.ServiceHistoryEntry
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.LabeledListRow
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.LocalFeatureFlags
import com.naury.chageun.core.ui.MaintenanceItemIcon
import com.naury.chageun.core.ui.MaintenanceProgressBar
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.missingInputText
import com.naury.chageun.core.ui.remainingText
import com.naury.chageun.core.ui.tone
import com.naury.chageun.core.ui.usedFraction

@Composable
internal fun ManageDetailPane(
    detail: ManageDetail,
    onBack: (() -> Unit)?,
    onRecordService: (MaintenanceItem) -> Unit,
    onEditRule: (MaintenanceItem) -> Unit,
    modifier: Modifier = Modifier,
    onAskAi: (MaintenanceItem) -> Unit = {},
) {
    val status = detail.status
    val gutter = ChageunTheme.spacing.gutter
    // 외부 AI 공유를 끄면 진입점도 감춘다.
    val showsAi = LocalFeatureFlags.current.aiShareEnabled
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = gutter, vertical = ChageunTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        item(key = "header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            ) {
                onBack?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.manage_back),
                        )
                    }
                }
                MaintenanceItemIcon(status.item, size = 52.dp)
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
        item(key = "remaining") { RemainingCard(detail) }
        item(key = "actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                OutlinedButton(
                    onClick = { onEditRule(status.item) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ChageunTheme.spacing.minTouchTarget),
                ) { Text(stringResource(R.string.rule_edit_action)) }
                Button(
                    onClick = { onRecordService(status.item) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ChageunTheme.spacing.minTouchTarget),
                ) { Text(stringResource(R.string.record_action)) }
            }
        }
        item(key = "basis") { BasisSection(detail) }
        if (showsAi) {
            item(key = "ai") {
                CardGroup(null) {
                    ListRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = stringResource(R.string.manage_ask_ai),
                        tone = ChageunTheme.colors.ai,
                        onClick = { onAskAi(status.item) },
                    )
                }
            }
        }
        item(key = "history") { HistorySection(detail.history) }
    }
}

/** 남은 거리와 진행 막대. 홈 카드와 같은 계산이라 두 화면의 값이 어긋나지 않는다. */
@Composable
private fun RemainingCard(detail: ManageDetail) {
    val status = detail.status
    val headline = if (status.state == MaintenanceState.Unknown) missingInputText(status) else remainingText(status)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        ) {
            headline?.let { Text(it, style = NumericTextStyles.Title) }
            usedFraction(status, detail.rule)?.let { MaintenanceProgressBar(it, status.state.tone.colors.content) }
        }
    }
}

@Composable
private fun BasisSection(detail: ManageDetail) {
    val status = detail.status
    val lastService = detail.history.firstOrNull()
    val nextParts = listOfNotNull(
        status.distanceDue?.let { stringResource(R.string.manage_km, formatNumber(it.value)) },
        status.dateDue?.let { formatDate(it) },
    )
    val rows = listOf(
        Triple(
            Icons.Filled.History,
            stringResource(R.string.manage_last_service),
            lastService?.let { serviceSummary(it) } ?: stringResource(R.string.manage_last_service_none),
        ),
        Triple(Icons.Filled.Repeat, stringResource(R.string.manage_interval), detail.rule?.let { intervalText(it) }),
        Triple(
            Icons.Filled.Flag,
            stringResource(R.string.manage_next_threshold),
            nextParts.joinToString(" / ").ifEmpty { null },
        ),
        Triple(
            Icons.Filled.Event,
            stringResource(R.string.manage_estimated_due),
            status.estimatedDue?.let {
                stringResource(
                    R.string.manage_estimate_with_confidence,
                    formatDate(it.date),
                    stringResource(it.confidence.labelRes),
                )
            },
        ),
        Triple(
            Icons.AutoMirrored.Filled.MenuBook,
            stringResource(R.string.manage_rule_source),
            stringResource(status.ruleSource.labelRes),
        ),
    ).filterNot { it.third.isNullOrBlank() }
    CardGroup(null) {
        rows.forEachIndexed { index, (icon, label, value) ->
            if (index > 0) GroupDivider()
            LabeledListRow(icon, label, value)
        }
    }
}

@Composable
private fun HistorySection(history: List<ServiceHistoryEntry>) {
    CardGroup(stringResource(R.string.manage_history)) {
        if (history.isEmpty()) {
            Text(
                stringResource(R.string.manage_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(ChageunTheme.spacing.md),
            )
        }
        history.forEachIndexed { index, entry ->
            if (index > 0) GroupDivider()
            val extras = listOfNotNull(
                entry.costWon?.let { stringResource(R.string.manage_cost_won, formatNumber(it)) },
                entry.shopName,
            )
            ListRow(
                icon = Icons.Filled.Build,
                title = serviceSummary(entry),
                body = extras.joinToString(" · ").ifEmpty { null },
            )
        }
    }
}

@Composable
private fun serviceSummary(entry: ServiceHistoryEntry): String {
    val date = entry.date?.let { formatDate(it) } ?: stringResource(R.string.manage_unknown_date)
    val mileage = entry.mileage?.let {
        val res = if (entry.isMileageEstimated) R.string.manage_km_estimated else R.string.manage_km
        stringResource(res, formatNumber(it.value))
    }
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
