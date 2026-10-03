package com.naury.chageun.feature.history.form

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.ChoiceCard
import com.naury.chageun.core.designsystem.component.ChoicePill
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.ToneColors
import com.naury.chageun.core.model.CheckKind
import com.naury.chageun.core.model.FuelField
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.ui.AdaptiveSheet
import com.naury.chageun.core.ui.FormHeader
import com.naury.chageun.core.ui.FormLabel
import com.naury.chageun.core.ui.NumberInputField
import com.naury.chageun.core.ui.OptionalSection
import com.naury.chageun.core.ui.QuickDateField
import com.naury.chageun.core.ui.QuickPick
import com.naury.chageun.core.ui.formatLitres
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.icon
import com.naury.chageun.feature.history.R
import java.time.LocalDate

@Composable
internal fun FuelFormHost(isExpanded: Boolean, onDismiss: () -> Unit, viewModel: FuelFormViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onDismiss() }
    AdaptiveSheet(isExpanded = isExpanded, onDismiss = onDismiss) {
        FuelFormContent(
            uiState,
            FuelFormActions(
                onDateSelected = viewModel::onDateSelected,
                onMileageChanged = viewModel::onMileageChanged,
                onTotalChanged = viewModel::onTotalChanged,
                onVolumeChanged = viewModel::onVolumeChanged,
                onUnitPriceChanged = viewModel::onUnitPriceChanged,
                onFullTankChanged = viewModel::onFullTankChanged,
                onStationChanged = viewModel::onStationChanged,
                onMemoChanged = viewModel::onMemoChanged,
                onSave = viewModel::save,
                onCancel = onDismiss,
            ),
        )
    }
}

internal data class FuelFormActions(
    val onDateSelected: (LocalDate) -> Unit,
    val onMileageChanged: (String) -> Unit,
    val onTotalChanged: (String) -> Unit,
    val onVolumeChanged: (String) -> Unit,
    val onUnitPriceChanged: (String) -> Unit,
    val onFullTankChanged: (Boolean) -> Unit,
    val onStationChanged: (String) -> Unit,
    val onMemoChanged: (String) -> Unit,
    val onSave: () -> Unit,
    val onCancel: () -> Unit,
)

@Composable
internal fun FuelFormContent(uiState: FuelFormUiState, actions: FuelFormActions) {
    FormColumn(
        icon = Icons.Filled.LocalGasStation,
        tone = ChageunTheme.colors.good,
        titleRes = R.string.fuel_form_title,
        isSaving = uiState.isSaving,
        hasSaveFailed = uiState.hasSaveFailed,
        onSave = actions.onSave,
        onCancel = actions.onCancel,
    ) {
        FormLabel(stringResource(R.string.form_date))
        QuickDateField(date = uiState.date, onDateSelected = actions.onDateSelected)
        if (FormError.FutureDate in uiState.errors) ErrorText(R.string.form_error_future_date)
        NumberInputField(
            value = uiState.total,
            onValueChange = actions.onTotalChanged,
            label = stringResource(R.string.fuel_form_total),
            unit = stringResource(R.string.unit_won),
            picks = FUEL_AMOUNTS.map {
                QuickPick(stringResource(R.string.fuel_form_amount_pick, formatNumber(it)), it.toString())
            },
        )
        Text(
            stringResource(R.string.fuel_form_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
            NumberInputField(
                value = uiState.unitPrice,
                onValueChange = actions.onUnitPriceChanged,
                label = stringResource(R.string.fuel_form_unit_price),
                unit = stringResource(R.string.unit_won_per_liter),
                modifier = Modifier.weight(1f),
            )
            NumberInputField(
                value = uiState.volumeLitres,
                onValueChange = actions.onVolumeChanged,
                label = stringResource(R.string.fuel_form_volume),
                unit = stringResource(R.string.unit_liter),
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
        }
        CalculatedAmount(uiState)
        if (FormError.Amounts in uiState.errors) ErrorText(R.string.form_error_amounts)
        FormLabel(stringResource(R.string.fuel_form_fill_label))
        Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
            ChoicePill(stringResource(R.string.fuel_form_full), uiState.isFullTank, { actions.onFullTankChanged(true) })
            ChoicePill(
                stringResource(R.string.fuel_form_partial),
                !uiState.isFullTank,
                { actions.onFullTankChanged(false) },
            )
        }
        NumberInputField(
            value = uiState.mileage,
            onValueChange = actions.onMileageChanged,
            label = stringResource(R.string.form_mileage),
            unit = stringResource(R.string.unit_km),
            errorText = stringResource(R.string.form_error_required).takeIf { FormError.Required in uiState.errors },
        )
        StationPicker(uiState.stationName, actions.onStationChanged)
        OptionalSection(hasValue = uiState.memo.isNotEmpty()) {
            TextInput(uiState.memo, actions.onMemoChanged, R.string.form_memo, singleLine = false)
        }
    }
}

@Composable
private fun CalculatedAmount(uiState: FuelFormUiState) {
    val amounts = uiState.amounts?.takeIf { it.computedField != null } ?: return
    val calculated = when (amounts.computedField) {
        FuelField.Total -> stringResource(R.string.history_won, formatNumber(amounts.totalPriceWon))
        FuelField.Volume -> stringResource(R.string.history_liters, formatLitres(amounts.volumeMl))
        FuelField.UnitPrice -> stringResource(R.string.history_unit_price, formatNumber(amounts.unitPriceWon))
        null -> return
    }
    Text(
        stringResource(R.string.fuel_form_calculated, calculated),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** 주유소는 대부분 큰 브랜드 중 하나라 칩으로 고르고, 목록에 없을 때만 이름을 입력한다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StationPicker(stationName: String, onStationChanged: (String) -> Unit) {
    val brands = stringArrayResource(R.array.fuel_station_brands).toList()
    var isOther by rememberSaveable { mutableStateOf(stationName.isNotEmpty() && stationName !in brands) }
    FormLabel(stringResource(R.string.fuel_form_station_label))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
    ) {
        brands.forEach { brand ->
            ChoicePill(brand, selected = !isOther && stationName == brand, onClick = {
                isOther = false
                // 같은 칩을 다시 누르면 선택을 지운다. 주유소는 선택 입력이다.
                onStationChanged(if (stationName == brand) "" else brand)
            })
        }
        ChoicePill(stringResource(R.string.fuel_form_station_other), selected = isOther, onClick = {
            isOther = true
            if (stationName in brands) onStationChanged("")
        })
    }
    AnimatedVisibility(visible = isOther) {
        TextInput(stationName, onStationChanged, R.string.fuel_form_station_name)
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
            icon = Icons.Filled.Build,
            tone = ChageunTheme.colors.ai,
            titleRes = R.string.check_form_title,
            isSaving = uiState.isSaving,
            hasSaveFailed = uiState.hasSaveFailed,
            onSave = viewModel::save,
            onCancel = onDismiss,
        ) {
            FormLabel(stringResource(R.string.check_form_kind_label))
            Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                CheckKind.entries.forEach { kind ->
                    ChoiceCard(
                        label = stringResource(kind.labelRes),
                        icon = kind.icon,
                        selected = uiState.kind == kind,
                        onClick = { viewModel.onKindSelected(kind) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            FormLabel(stringResource(R.string.check_form_subject_label))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            ) {
                stringArrayResource(uiState.kind.titlesRes).forEach { title ->
                    ChoicePill(title, uiState.title == title, { viewModel.onTitleChanged(title) })
                }
            }
            TextInput(
                uiState.title,
                viewModel::onTitleChanged,
                R.string.check_form_subject,
                isError = FormError.Required in uiState.errors,
            )
            FormLabel(stringResource(R.string.form_date))
            QuickDateField(date = uiState.date, onDateSelected = viewModel::onDateSelected)
            if (FormError.FutureDate in uiState.errors) ErrorText(R.string.form_error_future_date)
            NumberInputField(
                value = uiState.mileage,
                onValueChange = viewModel::onMileageChanged,
                label = stringResource(R.string.check_form_mileage),
                unit = stringResource(R.string.unit_km),
            )
            NumberInputField(
                value = uiState.cost,
                onValueChange = viewModel::onCostChanged,
                label = stringResource(R.string.check_form_cost),
                unit = stringResource(R.string.unit_won),
            )
            OptionalSection(hasValue = uiState.memo.isNotEmpty()) {
                TextInput(uiState.memo, viewModel::onMemoChanged, R.string.form_memo, singleLine = false)
            }
        }
    }
}

@Composable
private fun FormColumn(
    icon: ImageVector,
    tone: ToneColors,
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
        FormHeader(icon, tone, stringResource(titleRes))
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

private val CheckKind.icon: ImageVector
    get() = when (this) {
        CheckKind.Inspection -> TimelineEventType.Inspection.icon
        CheckKind.Repair -> TimelineEventType.Repair.icon
        CheckKind.Note -> TimelineEventType.Note.icon
    }

private val CheckKind.titlesRes: Int
    get() = when (this) {
        CheckKind.Inspection -> R.array.check_titles_inspection
        CheckKind.Repair -> R.array.check_titles_repair
        CheckKind.Note -> R.array.check_titles_note
    }

/** 한 번에 넣는 흔한 금액. 칩에 없는 금액만 직접 입력한다. */
private val FUEL_AMOUNTS = listOf(30_000L, 50_000L, 70_000L, 100_000L)
