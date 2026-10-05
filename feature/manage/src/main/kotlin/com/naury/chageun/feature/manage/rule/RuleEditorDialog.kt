package com.naury.chageun.feature.manage.rule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.maintenance.RuleEditError
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.ui.FormHeader
import com.naury.chageun.core.ui.NumberInputField
import com.naury.chageun.core.ui.QuickPick
import com.naury.chageun.core.ui.ToggleListRow
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.icon
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.tone
import com.naury.chageun.feature.manage.R

@Composable
internal fun RuleEditorHost(item: MaintenanceItem, onDismiss: () -> Unit) {
    val viewModel = hiltViewModel<RuleEditorViewModel, RuleEditorViewModel.Factory>(key = "rule-${item.name}") {
        it.create(item)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isDone) { if (uiState.isDone) onDismiss() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.widthIn(max = 560.dp)) {
            RuleEditorContent(
                uiState = uiState,
                onIntervalKmChanged = viewModel::onIntervalKmChanged,
                onIntervalMonthsChanged = viewModel::onIntervalMonthsChanged,
                onEnabledChanged = viewModel::onEnabledChanged,
                onSave = viewModel::save,
                onReset = viewModel::resetToGeneric,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
internal fun RuleEditorContent(
    uiState: RuleEditorUiState,
    onIntervalKmChanged: (String) -> Unit,
    onIntervalMonthsChanged: (String) -> Unit,
    onEnabledChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(ChageunTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        FormHeader(
            uiState.item.icon,
            uiState.item.category.tone(),
            stringResource(R.string.rule_title, stringResource(uiState.item.labelRes)),
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            ToggleListRow(
                icon = Icons.Filled.NotificationsActive,
                title = stringResource(R.string.rule_enabled),
                body = null,
                checked = uiState.isEnabled,
                onCheckedChange = onEnabledChanged,
            )
        }
        NumberInputField(
            value = uiState.intervalKm,
            onValueChange = onIntervalKmChanged,
            label = stringResource(R.string.rule_interval_km),
            unit = stringResource(R.string.rule_unit_km),
            picks = KM_PICKS.map { QuickPick(stringResource(R.string.rule_km_pick, formatNumber(it)), it.toString()) },
        )
        NumberInputField(
            value = uiState.intervalMonths,
            onValueChange = onIntervalMonthsChanged,
            label = stringResource(R.string.rule_interval_months),
            unit = stringResource(R.string.rule_unit_months),
            picks = MONTH_PICKS.map {
                QuickPick(pluralStringResource(R.plurals.rule_months_pick, it, it), it.toString())
            },
        )
        Text(
            stringResource(R.string.rule_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        uiState.error?.let { error ->
            Text(
                stringResource(error.messageRes),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (uiState.source != RuleSource.Generic) {
            TextButton(onClick = onReset, enabled = !uiState.isSaving) { Text(stringResource(R.string.rule_reset)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.record_cancel))
            }
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving && !uiState.isLoading,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = ChageunTheme.spacing.minTouchTarget),
            ) { Text(stringResource(R.string.record_save)) }
        }
    }
}

private val RuleEditError.messageRes: Int
    get() = when (this) {
        RuleEditError.NoInterval -> R.string.rule_error_no_interval
        RuleEditError.NonPositiveInterval -> R.string.rule_error_non_positive
        RuleEditError.UnknownItem -> R.string.rule_error_unknown
    }

/** 흔히 쓰는 교체 주기. 칩에 없는 값만 직접 입력한다. */
private val KM_PICKS = listOf(5_000L, 10_000L, 20_000L, 40_000L)
private val MONTH_PICKS = listOf(6, 12, 24, 36)
