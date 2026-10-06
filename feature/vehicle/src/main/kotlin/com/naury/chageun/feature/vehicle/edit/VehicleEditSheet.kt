package com.naury.chageun.feature.vehicle.edit

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.ui.AdaptiveSheet
import com.naury.chageun.core.ui.FormHeader
import com.naury.chageun.core.ui.FormLabel
import com.naury.chageun.core.ui.PastDateField
import com.naury.chageun.core.ui.VehicleInfoField
import com.naury.chageun.core.ui.VehicleInfoFields
import com.naury.chageun.feature.vehicle.R
import java.time.LocalDate

/** Compact 창에서는 Bottom Sheet, 그 외에는 너비를 제한한 Dialog로 띄운다. 저장하면 스스로 닫힌다. */
@Composable
fun VehicleEditHost(isExpanded: Boolean, onDismiss: () -> Unit) {
    val viewModel = hiltViewModel<VehicleEditViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onDismiss() }
    AdaptiveSheet(isExpanded = isExpanded, onDismiss = onDismiss) {
        VehicleEditContent(
            uiState = uiState,
            actions = VehicleEditActions(
                onMakerChanged = viewModel::onMakerChanged,
                onModelChanged = viewModel::onModelChanged,
                onModelYearChanged = viewModel::onModelYearChanged,
                onFuelTypeSelected = viewModel::onFuelTypeSelected,
                onTrimChanged = viewModel::onTrimChanged,
                onPlateChanged = viewModel::onPlateChanged,
                onRemovePlate = viewModel::onRemovePlate,
                onKeepPlate = viewModel::onKeepPlate,
                onFirstRegistrationDateChanged = viewModel::onFirstRegistrationDateChanged,
                onSave = viewModel::save,
                onDismiss = onDismiss,
            ),
        )
    }
}

data class VehicleEditActions(
    val onMakerChanged: (String) -> Unit = {},
    val onModelChanged: (String) -> Unit = {},
    val onModelYearChanged: (String) -> Unit = {},
    val onFuelTypeSelected: (FuelType) -> Unit = {},
    val onTrimChanged: (String) -> Unit = {},
    val onPlateChanged: (String) -> Unit = {},
    val onRemovePlate: () -> Unit = {},
    val onKeepPlate: () -> Unit = {},
    val onFirstRegistrationDateChanged: (LocalDate?) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)

@Composable
fun VehicleEditContent(uiState: VehicleEditUiState, actions: VehicleEditActions, modifier: Modifier = Modifier) {
    if (!uiState.isLoaded) {
        Box(modifier.fillMaxWidth().padding(ChageunTheme.spacing.xl), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(ChageunTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        FormHeader(Icons.Filled.DirectionsCar, ChageunTheme.colors.unknown, stringResource(R.string.vehicle_edit_title))
        VehicleInfoFields(
            maker = uiState.maker,
            model = uiState.model,
            modelYear = uiState.modelYear,
            fuelType = uiState.fuelType,
            onMakerChanged = actions.onMakerChanged,
            onModelChanged = actions.onModelChanged,
            onModelYearChanged = actions.onModelYearChanged,
            onFuelTypeSelected = actions.onFuelTypeSelected,
            error = { field -> uiState.errors[field.editField]?.let { ErrorText(it) } },
        )
        if (uiState.isFuelChanged) {
            Text(
                stringResource(R.string.vehicle_edit_fuel_changed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = uiState.trim,
            onValueChange = actions.onTrimChanged,
            label = { Text(stringResource(R.string.vehicle_edit_trim)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        FirstRegistrationSection(uiState.firstRegistrationDate, actions.onFirstRegistrationDateChanged)
        PlateSection(uiState, actions)
        Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
            TextButton(onClick = actions.onDismiss, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.vehicle_edit_cancel))
            }
            Button(
                onClick = actions.onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = ChageunTheme.spacing.minTouchTarget),
            ) { Text(stringResource(R.string.vehicle_edit_save)) }
        }
    }
}

@Composable
private fun FirstRegistrationSection(date: LocalDate?, onChanged: (LocalDate?) -> Unit) {
    FormLabel(stringResource(R.string.vehicle_edit_first_registration), Modifier.padding(top = ChageunTheme.spacing.xs))
    PastDateField(
        date = date,
        placeholder = stringResource(R.string.vehicle_edit_first_registration_pick),
        onDateSelected = onChanged,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.vehicle_edit_first_registration_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (date != null) {
            TextButton(onClick = { onChanged(null) }) {
                Text(stringResource(R.string.vehicle_edit_first_registration_clear))
            }
        }
    }
}

/** 번호는 암호화해 저장하므로 지금 번호를 그대로 보여 줄 수 없다. 바꿀 때만 새로 입력한다. */
@Composable
private fun PlateSection(uiState: VehicleEditUiState, actions: VehicleEditActions) {
    FormLabel(stringResource(R.string.vehicle_edit_plate), Modifier.padding(top = ChageunTheme.spacing.xs))
    if (uiState.isPlateRemoved) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.vehicle_edit_plate_will_remove),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = actions.onKeepPlate) { Text(stringResource(R.string.vehicle_edit_plate_undo)) }
        }
        return
    }
    OutlinedTextField(
        value = uiState.newPlate,
        onValueChange = actions.onPlateChanged,
        label = { Text(stringResource(R.string.vehicle_edit_plate_new)) },
        placeholder = uiState.currentPlateMasked?.let { { Text(it) } },
        supportingText = {
            val error = uiState.errors[VehicleEditField.Plate]
            Text(
                if (error != null) {
                    stringResource(error.messageRes)
                } else {
                    stringResource(R.string.vehicle_edit_plate_hint)
                },
            )
        },
        isError = uiState.errors[VehicleEditField.Plate] != null,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
    if (uiState.currentPlateMasked != null) {
        TextButton(onClick = actions.onRemovePlate) { Text(stringResource(R.string.vehicle_edit_plate_remove)) }
    }
}

@Composable
private fun ErrorText(error: VehicleEditError) {
    Text(
        stringResource(error.messageRes),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

private val VehicleInfoField.editField: VehicleEditField
    get() = when (this) {
        VehicleInfoField.Maker -> VehicleEditField.Maker
        VehicleInfoField.Model -> VehicleEditField.Model
        VehicleInfoField.ModelYear -> VehicleEditField.ModelYear
        VehicleInfoField.FuelType -> VehicleEditField.FuelType
    }

@get:StringRes
private val VehicleEditError.messageRes: Int
    get() = when (this) {
        VehicleEditError.Required -> R.string.vehicle_edit_error_required
        VehicleEditError.InvalidYear -> R.string.vehicle_edit_error_year
        VehicleEditError.InvalidPlate -> R.string.vehicle_edit_error_plate
        VehicleEditError.UnsupportedPlate -> R.string.vehicle_edit_error_plate_unsupported
    }
