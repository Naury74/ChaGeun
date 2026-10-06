package com.naury.chageun.feature.history

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.naury.chageun.core.designsystem.component.ChoicePill
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.AdaptiveSheet
import com.naury.chageun.core.ui.FormHeader
import com.naury.chageun.core.ui.FormLabel
import com.naury.chageun.core.ui.NumberInputField
import com.naury.chageun.core.ui.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 기간·비용·첨부 조건을 고르는 시트. 고르는 동안에는 목록을 바꾸지 않고, 적용을 눌러야 반영한다.
 * 회전이나 접기 뒤에도 고르던 값이 남도록 초안을 저장 가능한 상태로 둔다.
 */
@Composable
internal fun AdvancedFilterSheet(
    current: AdvancedFilter,
    isExpanded: Boolean,
    onApply: (AdvancedFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var draftEncoded by rememberSaveable { mutableStateOf(current.encode()) }
    val draft = AdvancedFilter.decode(draftEncoded)
    val update: (AdvancedFilter) -> Unit = { draftEncoded = it.encode() }
    var isPickingRange by rememberSaveable { mutableStateOf(false) }

    AdaptiveSheet(isExpanded = isExpanded, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(ChageunTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        ) {
            FormHeader(Icons.Filled.Tune, ChageunTheme.colors.unknown, stringResource(R.string.history_filter_title))
            PeriodSection(draft, update, onPickRange = { isPickingRange = true })
            CostSection(draft, update)
            AttachmentToggle(draft, update)
            Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                TextButton(onClick = { update(AdvancedFilter()) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.history_filter_reset))
                }
                Button(
                    onClick = {
                        onApply(draft)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ChageunTheme.spacing.minTouchTarget),
                ) { Text(stringResource(R.string.history_filter_apply)) }
            }
        }
    }
    if (isPickingRange) {
        RangePickerDialog(
            initialFrom = draft.customFrom,
            initialTo = draft.customTo,
            onPicked = { from, to ->
                update(draft.copy(period = HistoryPeriod.Custom, customFrom = from, customTo = to))
                isPickingRange = false
            },
            onDismiss = { isPickingRange = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodSection(draft: AdvancedFilter, update: (AdvancedFilter) -> Unit, onPickRange: () -> Unit) {
    FormLabel(stringResource(R.string.history_filter_period))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
    ) {
        HistoryPeriod.entries.filter { it != HistoryPeriod.Custom }.forEach { period ->
            ChoicePill(
                label = stringResource(period.labelRes),
                selected = draft.period == period,
                onClick = { update(draft.copy(period = period)) },
            )
        }
        val custom = draft.customFrom?.let { from ->
            draft.customTo?.let { to ->
                stringResource(R.string.history_filter_range, formatDate(from), formatDate(to))
            }
        }
        ChoicePill(
            label = custom.takeIf { draft.period == HistoryPeriod.Custom }
                ?: stringResource(R.string.history_period_custom),
            selected = draft.period == HistoryPeriod.Custom,
            onClick = onPickRange,
        )
    }
}

@Composable
private fun CostSection(draft: AdvancedFilter, update: (AdvancedFilter) -> Unit) {
    FormLabel(stringResource(R.string.history_filter_cost), Modifier.padding(top = ChageunTheme.spacing.xs))
    val isRangeReversed = draft.minCostWon != null && draft.maxCostWon != null && draft.minCostWon > draft.maxCostWon
    Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        NumberInputField(
            value = draft.minCostWon?.toString().orEmpty(),
            onValueChange = { update(draft.copy(minCostWon = it.toCostOrNull())) },
            label = stringResource(R.string.history_filter_cost_min),
            unit = stringResource(R.string.unit_won),
            modifier = Modifier.weight(1f),
        )
        NumberInputField(
            value = draft.maxCostWon?.toString().orEmpty(),
            onValueChange = { update(draft.copy(maxCostWon = it.toCostOrNull())) },
            label = stringResource(R.string.history_filter_cost_max),
            unit = stringResource(R.string.unit_won),
            modifier = Modifier.weight(1f),
            errorText = stringResource(R.string.history_filter_cost_reversed).takeIf { isRangeReversed },
        )
    }
    Text(
        stringResource(R.string.history_filter_cost_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AttachmentToggle(draft: AdvancedFilter, update: (AdvancedFilter) -> Unit) {
    // 제목을 눌러도 바뀌고, TalkBack이 제목과 상태를 한 번에 읽도록 행 전체를 스위치로 만든다.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ChageunTheme.spacing.minTouchTarget)
            .toggleable(
                value = draft.withAttachmentsOnly,
                role = Role.Switch,
                onValueChange = { update(draft.copy(withAttachmentsOnly = it)) },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.history_filter_attachments),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = draft.withAttachmentsOnly, onCheckedChange = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerDialog(
    initialFrom: LocalDate?,
    initialTo: LocalDate?,
    onPicked: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialFrom?.toUtcMillis(),
        initialSelectedEndDateMillis = initialTo?.toUtcMillis(),
    )
    val from = state.selectedStartDateMillis?.toLocalDate()
    val to = state.selectedEndDateMillis?.toLocalDate()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { if (from != null) onPicked(from, to ?: from) },
                enabled = from != null,
            ) { Text(stringResource(R.string.history_filter_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.history_cancel)) } },
    ) {
        DateRangePicker(state = state, modifier = Modifier.weight(1f))
    }
}

@get:StringRes
internal val HistoryPeriod.labelRes: Int
    get() = when (this) {
        HistoryPeriod.All -> R.string.history_period_all
        HistoryPeriod.LastMonth -> R.string.history_period_month
        HistoryPeriod.Last3Months -> R.string.history_period_3months
        HistoryPeriod.Last6Months -> R.string.history_period_6months
        HistoryPeriod.ThisYear -> R.string.history_period_year
        HistoryPeriod.Custom -> R.string.history_period_custom
    }

private fun String.toCostOrNull(): Long? = filter(Char::isDigit).take(MAX_COST_DIGITS).toLongOrNull()

// DatePicker는 UTC 자정 밀리초를 쓴다.
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private const val MAX_COST_DIGITS = 10
