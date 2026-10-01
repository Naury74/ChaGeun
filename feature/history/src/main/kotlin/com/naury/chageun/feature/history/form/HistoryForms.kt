package com.naury.chageun.feature.history.form

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.ui.AdaptiveSheet
import com.naury.chageun.core.ui.PastDateField
import com.naury.chageun.core.ui.formatLitres
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.feature.history.R

@Composable
internal fun FuelFormHost(isExpanded: Boolean, onDismiss: () -> Unit, viewModel: FuelFormViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onDismiss() }
    AdaptiveSheet(isExpanded = isExpanded, onDismiss = onDismiss) {
        FormColumn(
            R.string.fuel_form_title,
            uiState.isSaving,
            uiState.hasSaveFailed,
            onSave = viewModel::save,
            onCancel = onDismiss,
        ) {
            Text(stringResource(R.string.form_date), style = MaterialTheme.typography.labelLarge)
            PastDateField(date = uiState.date, placeholder = "", onDateSelected = viewModel::onDateSelected)
            NumberField(
                uiState.mileage,
                viewModel::onMileageChanged,
                R.string.form_mileage,
                R.string.unit_km,
                FormError.Required in uiState.errors,
            )
            Text(
                stringResource(R.string.fuel_form_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NumberField(uiState.total, viewModel::onTotalChanged, R.string.fuel_form_total, R.string.unit_won, false)
            NumberField(
                uiState.volumeLitres,
                viewModel::onVolumeChanged,
                R.string.fuel_form_volume,
                R.string.unit_liter,
                isError = false,
                keyboardType = KeyboardType.Decimal,
            )
            NumberField(
                uiState.unitPrice,
                viewModel::onUnitPriceChanged,
                R.string.fuel_form_unit_price,
                R.string.unit_won_per_liter,
                false,
            )
            uiState.amounts?.takeIf { it.computedField != null }?.let { amounts ->
                val calculated = when (amounts.computedField) {
                    FuelField.Total -> stringResource(R.string.history_won, formatNumber(amounts.totalPriceWon))
                    FuelField.Volume -> stringResource(R.string.history_liters, formatLitres(amounts.volumeMl))
                    FuelField.UnitPrice -> stringResource(
                        R.string.history_unit_price,
                        formatNumber(amounts.unitPriceWon),
                    )
                    null -> ""
                }
                Text(
                    stringResource(R.string.fuel_form_calculated, calculated),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (FormError.Amounts in uiState.errors) ErrorText(R.string.form_error_amounts)
            if (FormError.FutureDate in uiState.errors) ErrorText(R.string.form_error_future_date)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.fuel_form_full_tank), modifier = Modifier.weight(1f))
                Switch(checked = uiState.isFullTank, onCheckedChange = viewModel::onFullTankChanged)
            }
            TextInput(uiState.stationName, viewModel::onStationChanged, R.string.fuel_form_station)
            TextInput(uiState.memo, viewModel::onMemoChanged, R.string.form_memo, singleLine = false)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CheckFormHost(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    viewModel: CheckFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onDismiss() }
    AdaptiveSheet(isExpanded = isExpanded, onDismiss = onDismiss) {
        FormColumn(
            R.string.check_form_title,
            uiState.isSaving,
            uiState.hasSaveFailed,
            onSave = viewModel::save,
            onCancel = onDismiss,
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                CheckKind.entries.forEach { kind ->
                    FilterChip(
                        selected = uiState.kind == kind,
                        onClick = { viewModel.onKindSelected(kind) },
                        label = { Text(stringResource(kind.labelRes)) },
                    )
                }
            }
            Text(stringResource(R.string.form_date), style = MaterialTheme.typography.labelLarge)
            PastDateField(date = uiState.date, placeholder = "", onDateSelected = viewModel::onDateSelected)
            if (FormError.FutureDate in uiState.errors) ErrorText(R.string.form_error_future_date)
            TextInput(
                uiState.title,
                viewModel::onTitleChanged,
                R.string.check_form_subject,
                isError =
                FormError.Required in uiState.errors,
            )
            NumberField(
                uiState.mileage,
                viewModel::onMileageChanged,
                R.string.check_form_mileage,
                R.string.unit_km,
                false,
            )
            NumberField(uiState.cost, viewModel::onCostChanged, R.string.check_form_cost, R.string.unit_won, false)
            TextInput(uiState.memo, viewModel::onMemoChanged, R.string.form_memo, singleLine = false)
        }
    }
}

@Composable
private fun FormColumn(
    @StringRes titleRes: Int,
    isSaving: Boolean,
    hasSaveFailed: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(ChageunTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleLarge)
        content()
        if (hasSaveFailed) ErrorText(R.string.form_save_failed)
        Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.history_cancel))
            }
            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = ChageunTheme.spacing.minTouchTarget),
            ) { Text(stringResource(R.string.form_save)) }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    @StringRes unitRes: Int,
    isError: Boolean,
    keyboardType: KeyboardType = KeyboardType.Number,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        suffix = { Text(stringResource(unitRes)) },
        isError = isError,
        supportingText = if (isError) ({ Text(stringResource(R.string.form_error_required)) }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    singleLine: Boolean = true,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        isError = isError,
        supportingText = if (isError) ({ Text(stringResource(R.string.form_error_required)) }) else null,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorText(@StringRes res: Int) {
    Text(stringResource(res), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

internal val CheckKind.labelRes: Int
    get() = when (this) {
        CheckKind.Inspection -> R.string.check_form_kind_inspection
        CheckKind.Repair -> R.string.check_form_kind_repair
        CheckKind.Note -> R.string.check_form_kind_note
    }
