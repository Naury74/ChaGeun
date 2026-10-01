package com.naury.chageun.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.FuelType

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
fun OnboardingScreen(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit, modifier: Modifier = Modifier) {
    BackHandler(enabled = uiState.canGoBack) { onAction(OnboardingAction.Back) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .padding(ChageunTheme.spacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = CONTENT_MAX_WIDTH)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
        ) {
            when (uiState.step) {
                OnboardingStep.Intro -> IntroStep()
                OnboardingStep.Plate -> PlateStep(uiState, onAction)
                OnboardingStep.VehicleInfo -> VehicleInfoStep(uiState, onAction)
                OnboardingStep.Mileage -> MileageStep(uiState, onAction)
            }
        }
        BottomActions(
            uiState = uiState,
            onAction = onAction,
            modifier = Modifier
                .widthIn(max = CONTENT_MAX_WIDTH)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun IntroStep() {
    Spacer(Modifier.heightIn(min = 48.dp))
    Text(stringResource(R.string.onboarding_intro_title), style = MaterialTheme.typography.headlineLarge)
    Text(
        stringResource(R.string.onboarding_intro_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PlateStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepHeader(R.string.onboarding_plate_title, R.string.onboarding_plate_body)
    OnboardingTextField(
        value = uiState.plate,
        onValueChange = { onAction(OnboardingAction.PlateChanged(it)) },
        labelRes = R.string.onboarding_plate_label,
        error = uiState.errors[OnboardingField.Plate],
        placeholderRes = R.string.onboarding_plate_placeholder,
        imeAction = ImeAction.Done,
    )
    TextButton(onClick = { onAction(OnboardingAction.SkipPlate) }) {
        Text(stringResource(R.string.onboarding_plate_skip))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VehicleInfoStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepHeader(R.string.onboarding_vehicle_title, bodyRes = null)
    OnboardingTextField(
        value = uiState.maker,
        onValueChange = { onAction(OnboardingAction.MakerChanged(it)) },
        labelRes = R.string.onboarding_vehicle_maker,
        error = uiState.errors[OnboardingField.Maker],
    )
    OnboardingTextField(
        value = uiState.model,
        onValueChange = { onAction(OnboardingAction.ModelChanged(it)) },
        labelRes = R.string.onboarding_vehicle_model,
        error = uiState.errors[OnboardingField.Model],
    )
    OnboardingTextField(
        value = uiState.modelYear,
        onValueChange = { onAction(OnboardingAction.ModelYearChanged(it)) },
        labelRes = R.string.onboarding_vehicle_year,
        error = uiState.errors[OnboardingField.ModelYear],
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Done,
    )
    Text(stringResource(R.string.onboarding_vehicle_fuel), style = MaterialTheme.typography.titleSmall)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        FuelType.entries.forEach { fuel ->
            FilterChip(
                selected = uiState.fuelType == fuel,
                onClick = { onAction(OnboardingAction.FuelTypeSelected(fuel)) },
                label = { Text(stringResource(fuel.labelRes)) },
            )
        }
    }
    uiState.errors[OnboardingField.FuelType]?.let { ErrorText(it) }
}

@Composable
private fun MileageStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepHeader(R.string.onboarding_mileage_title, R.string.onboarding_mileage_body)
    OnboardingTextField(
        value = uiState.mileage,
        onValueChange = { onAction(OnboardingAction.MileageChanged(it)) },
        labelRes = R.string.onboarding_mileage_label,
        error = uiState.errors[OnboardingField.Mileage],
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Done,
        suffix = { Text(stringResource(R.string.onboarding_mileage_unit)) },
    )
    if (uiState.hasSaveFailed) {
        Text(
            stringResource(R.string.onboarding_save_failed),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BottomActions(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (labelRes, action) = when (uiState.step) {
        OnboardingStep.Intro -> R.string.onboarding_start to OnboardingAction.Start
        OnboardingStep.Plate -> R.string.onboarding_next to OnboardingAction.SubmitPlate
        OnboardingStep.VehicleInfo -> R.string.onboarding_next to OnboardingAction.SubmitVehicleInfo
        OnboardingStep.Mileage -> R.string.onboarding_finish to OnboardingAction.Finish
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Button(
            onClick = { onAction(action) },
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ChageunTheme.spacing.minTouchTarget),
        ) {
            Text(stringResource(labelRes))
        }
        if (uiState.canGoBack) {
            TextButton(onClick = { onAction(OnboardingAction.Back) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_back))
            }
        }
    }
}

@Composable
private fun StepHeader(@StringRes titleRes: Int, @StringRes bodyRes: Int?) {
    Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall)
    bodyRes?.let {
        Text(
            stringResource(it),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OnboardingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    error: FieldError?,
    @StringRes placeholderRes: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    suffix: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        placeholder = placeholderRes?.let { { Text(stringResource(it)) } },
        isError = error != null,
        supportingText = error?.let { { ErrorText(it) } },
        suffix = suffix,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorText(error: FieldError) {
    Text(
        text = stringResource(error.messageRes),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

private val FieldError.messageRes: Int
    get() = when (this) {
        FieldError.Required -> R.string.onboarding_error_required
        FieldError.InvalidPlate -> R.string.onboarding_error_plate_invalid
        FieldError.UnsupportedPlate -> R.string.onboarding_error_plate_unsupported
        FieldError.InvalidYear -> R.string.onboarding_error_year_invalid
        FieldError.InvalidMileage -> R.string.onboarding_error_mileage_invalid
    }

private val FuelType.labelRes: Int
    get() = when (this) {
        FuelType.Gasoline -> R.string.fuel_gasoline
        FuelType.Diesel -> R.string.fuel_diesel
        FuelType.Lpg -> R.string.fuel_lpg
        FuelType.Hybrid -> R.string.fuel_hybrid
        FuelType.PlugInHybrid -> R.string.fuel_plug_in_hybrid
        FuelType.Electric -> R.string.fuel_electric
        FuelType.Hydrogen -> R.string.fuel_hydrogen
    }

private val CONTENT_MAX_WIDTH = 560.dp

@Preview(widthDp = 360, heightDp = 720)
@Composable
private fun VehicleInfoStepPreview() {
    ChageunTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(
                step = OnboardingStep.VehicleInfo,
                maker = "KG Mobility",
                errors = mapOf(OnboardingField.Model to FieldError.Required),
            ),
            onAction = {},
        )
    }
}
