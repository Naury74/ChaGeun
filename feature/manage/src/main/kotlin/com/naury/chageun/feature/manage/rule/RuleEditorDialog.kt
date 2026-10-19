package com.naury.chageun.feature.manage.rule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.ChageunCard
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.maintenance.RuleEditError
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.ui.FormHeader
import com.naury.chageun.core.ui.GroupedNumberTransformation
import com.naury.chageun.core.ui.QuickPick
import com.naury.chageun.core.ui.ToggleListRow
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.icon
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.tone
import com.naury.chageun.feature.manage.ButtonPair
import com.naury.chageun.feature.manage.R
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KButtonSize
import com.naury.kinetica.ui.KChipRow
import com.naury.kinetica.ui.KFilterChip
import com.naury.kinetica.ui.KTextField
import com.naury.kinetica.ui.KVariant

@Composable
internal fun RuleEditorHost(item: MaintenanceItem, onDismiss: () -> Unit) {
    val viewModel = hiltViewModel<RuleEditorViewModel, RuleEditorViewModel.Factory>(key = "rule-${item.name}") {
        it.create(item)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isDone) { if (uiState.isDone) onDismiss() }

    Dialog(onDismissRequest = onDismiss) {
        ChageunCard(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.widthIn(max = 560.dp), popup = true) {
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
        ChageunCard(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            inset = true,
        ) {
            ToggleListRow(
                icon = Icons.Filled.NotificationsActive,
                title = stringResource(R.string.rule_enabled),
                body = null,
                checked = uiState.isEnabled,
                onCheckedChange = onEnabledChanged,
            )
        }
        IntervalField(
            value = uiState.intervalKm,
            onValueChange = onIntervalKmChanged,
            label = stringResource(R.string.rule_interval_km),
            unit = stringResource(R.string.rule_unit_km),
            picks = KM_PICKS.map { QuickPick(stringResource(R.string.rule_km_pick, formatNumber(it)), it.toString()) },
        )
        IntervalField(
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
            KButton(
                stringResource(R.string.rule_reset),
                onClick = onReset,
                variant = KVariant.Text,
                enabled = !uiState.isSaving,
            )
        }
        ButtonPair {
            KButton(
                stringResource(R.string.record_cancel),
                onClick = onDismiss,
                variant = KVariant.Text,
                size = KButtonSize.Large,
                fullWidth = true,
            )
            // 저장하는 동안은 폭을 그대로 둔 채 링이 돌고 다시 누를 수 없다.
            KButton(
                stringResource(R.string.record_save),
                onClick = onSave,
                loading = uiState.isSaving,
                enabled = !uiState.isLoading,
                size = KButtonSize.Large,
                fullWidth = true,
            )
        }
    }
}

/**
 * 교체 주기 입력란과 자주 쓰는 값 칩. 칩을 누르면 입력란이 채워지고, 칩에 없는 값만 직접 입력한다.
 * 비워 두면 그 기준(거리나 기간)은 쓰지 않으므로 −/+ 스테퍼 대신 비울 수 있는 입력란으로 둔다.
 */
@Composable
private fun IntervalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    picks: List<QuickPick>,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        KTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            suffix = unit,
            visualTransformation = GroupedNumberTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        // 칩마다 48dp 누름 영역을 두므로 그림자 여유는 따로 두지 않는다.
        KChipRow(fadeEdges = true, shadowRoom = false) {
            picks.forEach { pick ->
                KFilterChip(
                    label = pick.label,
                    selected = value == pick.value,
                    onSelectedChange = { onValueChange(pick.value) },
                    minTouchTarget = true,
                )
            }
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
