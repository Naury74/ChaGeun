package com.naury.chageun.feature.manage.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.StatusTone
import com.naury.chageun.core.designsystem.component.colors
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.ui.PastDateField
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.feature.manage.R
import java.time.LocalDate

/** Entry point used by the app shell; a bottom sheet on compact windows, a width-limited dialog otherwise. */
@Composable
fun RecordServiceHost(item: MaintenanceItem, isExpanded: Boolean, onDismiss: () -> Unit) {
    val viewModel = hiltViewModel<RecordServiceViewModel, RecordServiceViewModel.Factory>(key = item.name) {
        it.create(item)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val content: @Composable () -> Unit = {
        RecordServiceContent(
            uiState = uiState,
            actions = RecordServiceActions(
                onDateSelected = viewModel::onDateSelected,
                onMileageChanged = viewModel::onMileageChanged,
                onCostChanged = viewModel::onCostChanged,
                onShopNameChanged = viewModel::onShopNameChanged,
                onMemoChanged = viewModel::onMemoChanged,
                onSave = viewModel::save,
                onConfirmLowerMileage = viewModel::confirmLowerMileage,
                onEditLowerMileage = viewModel::dismissLowerMileageWarning,
                onDismiss = onDismiss,
            ),
        )
    }
    if (isExpanded) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.widthIn(max = 560.dp)) { content() }
        }
    } else {
        RecordServiceBottomSheet(onDismiss, content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordServiceBottomSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) { content() }
}

data class RecordServiceActions(
    val onDateSelected: (LocalDate) -> Unit,
    val onMileageChanged: (String) -> Unit,
    val onCostChanged: (String) -> Unit,
    val onShopNameChanged: (String) -> Unit,
    val onMemoChanged: (String) -> Unit,
    val onSave: () -> Unit,
    val onConfirmLowerMileage: () -> Unit,
    val onEditLowerMileage: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
fun RecordServiceContent(uiState: RecordServiceUiState, actions: RecordServiceActions, modifier: Modifier = Modifier) {
    val itemName = stringResource(uiState.item.labelRes)
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(ChageunTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        val saved = uiState.savedResult
        if (saved != null) {
            SavedContent(itemName, saved, actions.onDismiss)
            return@Column
        }
        Text(stringResource(R.string.record_title, itemName), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.record_date), style = MaterialTheme.typography.labelLarge)
        PastDateField(date = uiState.date, placeholder = "", onDateSelected = actions.onDateSelected)
        uiState.errors[RecordServiceField.Date]?.let { ErrorText(it) }
        NumberField(
            value = uiState.mileage,
            onValueChange = actions.onMileageChanged,
            label = stringResource(R.string.record_mileage),
            suffix = stringResource(R.string.record_unit_km),
            error = uiState.errors[RecordServiceField.Mileage],
        )
        NumberField(
            value = uiState.cost,
            onValueChange = actions.onCostChanged,
            label = stringResource(R.string.record_cost),
            suffix = stringResource(R.string.record_unit_won),
            error = uiState.errors[RecordServiceField.Cost],
            supporting = stringResource(R.string.record_cost_hint),
        )
        OutlinedTextField(
            value = uiState.shopName,
            onValueChange = actions.onShopNameChanged,
            label = { Text(stringResource(R.string.record_shop)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.memo,
            onValueChange = actions.onMemoChanged,
            label = { Text(stringResource(R.string.record_memo)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        uiState.lowerMileageWarning?.let { previous ->
            LowerMileageWarning(itemName, formatNumber(previous.value), actions)
        }
        if (uiState.hasSaveFailed) {
            Text(stringResource(R.string.record_save_failed), color = MaterialTheme.colorScheme.error)
        }
        if (uiState.lowerMileageWarning == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                TextButton(onClick = actions.onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.record_cancel))
                }
                Button(
                    onClick = actions.onSave,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ChageunTheme.spacing.minTouchTarget),
                ) { Text(stringResource(R.string.record_save)) }
            }
        }
    }
}

@Composable
private fun SavedContent(itemName: String, saved: SavedResult, onDone: () -> Unit) {
    Text(stringResource(R.string.record_saved_title, itemName), style = MaterialTheme.typography.titleLarge)
    val km = saved.nextDistanceDue?.let { formatNumber(it.value) }
    val date = saved.nextDateDue?.let { formatDate(it) }
    val message = when {
        km != null && date != null -> stringResource(R.string.record_saved_next_both, km, date)
        km != null -> stringResource(R.string.record_saved_next_km, km)
        date != null -> stringResource(R.string.record_saved_next_date, date)
        else -> null
    }
    message?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = ChageunTheme.spacing.minTouchTarget)) {
        Text(stringResource(R.string.record_done))
    }
}

@Composable
private fun LowerMileageWarning(itemName: String, previousKm: String, actions: RecordServiceActions) {
    val tone = StatusTone.Upcoming.colors
    Surface(color = tone.container, contentColor = tone.content, shape = MaterialTheme.shapes.medium) {
        Column(
            Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            Text(stringResource(R.string.record_lower_mileage_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.record_lower_mileage_body, itemName, previousKm),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                TextButton(onClick = actions.onEditLowerMileage, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.record_lower_mileage_edit))
                }
                Button(onClick = actions.onConfirmLowerMileage, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.record_lower_mileage_save))
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    error: RecordServiceError?,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        isError = error != null,
        supportingText = (error?.let { { ErrorText(it) } }) ?: supporting?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorText(error: RecordServiceError) {
    val res = when (error) {
        RecordServiceError.Required -> R.string.record_error_required
        RecordServiceError.FutureDate -> R.string.record_error_future_date
        RecordServiceError.InvalidNumber -> R.string.record_error_invalid_number
    }
    Text(stringResource(res), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
