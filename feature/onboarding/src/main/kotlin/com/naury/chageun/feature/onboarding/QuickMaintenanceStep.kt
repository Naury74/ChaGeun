package com.naury.chageun.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.labelRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
internal fun QuickMaintenanceStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    Text(stringResource(R.string.onboarding_quick_title), style = MaterialTheme.typography.headlineSmall)
    Text(
        stringResource(R.string.onboarding_quick_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    QUICK_SERVICE_ITEMS.forEach { item ->
        QuickServiceCard(
            item = item,
            input = uiState.quickServices[item] ?: QuickServiceInput(),
            error = uiState.quickServiceErrors[item],
            onAction = onAction,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickServiceCard(
    item: MaintenanceItem,
    input: QuickServiceInput,
    error: FieldError?,
    onAction: (OnboardingAction) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        ) {
            Text(stringResource(item.labelRes), style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            ) {
                QuickServiceMode.entries.forEach { mode ->
                    FilterChip(
                        selected = input.mode == mode,
                        onClick = { onAction(OnboardingAction.QuickServiceModeSelected(item, mode)) },
                        label = { Text(stringResource(mode.labelRes)) },
                    )
                }
            }
            if (input.needsDate) {
                ServiceDateField(
                    date = input.date,
                    onDateSelected = { onAction(OnboardingAction.QuickServiceDateSelected(item, it)) },
                )
            }
            if (input.needsMileage) {
                OutlinedTextField(
                    value = input.mileage,
                    onValueChange = { onAction(OnboardingAction.QuickServiceMileageChanged(item, it)) },
                    label = { Text(stringResource(R.string.onboarding_quick_mileage)) },
                    suffix = { Text(stringResource(R.string.onboarding_mileage_unit)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            error?.let { ErrorText(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceDateField(date: LocalDate?, onDateSelected: (LocalDate) -> Unit) {
    var isPickerOpen by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(
        onClick = { isPickerOpen = true },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ChageunTheme.spacing.minTouchTarget),
    ) {
        Text(date?.let { formatDate(it) } ?: stringResource(R.string.onboarding_quick_pick_date))
    }

    if (isPickerOpen) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            selectableDates = PastDates,
        )
        DatePickerDialog(
            onDismissRequest = { isPickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        isPickerOpen = false
                    },
                ) { Text(stringResource(R.string.onboarding_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { isPickerOpen = false }) { Text(stringResource(R.string.onboarding_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private object PastDates : SelectableDates {
    // DatePicker reports UTC midnight; compare in UTC so today stays selectable in every timezone.
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private val QuickServiceMode.labelRes: Int
    get() = when (this) {
        QuickServiceMode.DateAndMileage -> R.string.onboarding_quick_mode_both
        QuickServiceMode.DateOnly -> R.string.onboarding_quick_mode_date
        QuickServiceMode.MileageOnly -> R.string.onboarding_quick_mode_mileage
        QuickServiceMode.Unknown -> R.string.onboarding_quick_mode_unknown
    }
