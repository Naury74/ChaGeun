package com.naury.chageun.feature.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.vehicle.CompleteInspectionUseCase
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.ui.NumberInputField
import com.naury.chageun.core.ui.QuickDateField
import com.naury.chageun.core.ui.formatDate
import java.time.LocalDate

data class InspectionCompletion(val completedOn: LocalDate, val mileage: Kilometers?, val nextDueDate: LocalDate)

@Composable
internal fun CompleteInspectionDialog(
    today: LocalDate,
    previousDueDate: LocalDate?,
    currentMileage: Kilometers?,
    onConfirm: (InspectionCompletion) -> Unit,
    onDismiss: () -> Unit,
) {
    var completedOn by rememberSaveable { mutableStateOf(today) }
    var mileage by rememberSaveable { mutableStateOf(currentMileage?.value?.toString().orEmpty()) }
    // 사용자가 날짜를 고르기 전까지는 null로 두어 제안값이 검사일을 따라가게 한다.
    var chosenNextDue by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var isPickingNextDue by rememberSaveable { mutableStateOf(false) }
    val nextDue = chosenNextDue ?: CompleteInspectionUseCase.suggestNextDueDate(completedOn, previousDueDate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vehicle_inspection_complete_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            ) {
                Text(
                    stringResource(R.string.vehicle_inspection_complete_date),
                    style = MaterialTheme.typography.labelLarge,
                )
                QuickDateField(date = completedOn, onDateSelected = { completedOn = it }, today = today)
                NumberInputField(
                    value = mileage,
                    onValueChange = { input -> mileage = input.filter(Char::isDigit).take(MAX_DIGITS) },
                    label = stringResource(R.string.vehicle_inspection_complete_mileage),
                    unit = stringResource(R.string.vehicle_unit_km),
                )
                Text(
                    stringResource(R.string.vehicle_inspection_complete_next),
                    style = MaterialTheme.typography.labelLarge,
                )
                OutlinedButton(onClick = { isPickingNextDue = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(ChageunTheme.spacing.xs))
                    Text(formatDate(nextDue))
                }
                Text(
                    stringResource(R.string.vehicle_inspection_complete_next_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(InspectionCompletion(completedOn, mileage.toLongOrNull()?.let(::Kilometers), nextDue))
                },
            ) { Text(stringResource(R.string.vehicle_inspection_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.vehicle_inspection_cancel)) }
        },
    )
    if (isPickingNextDue) {
        InspectionDatePicker(
            initial = nextDue,
            onConfirm = {
                chosenNextDue = it
                isPickingNextDue = false
            },
            onDismiss = { isPickingNextDue = false },
        )
    }
}

private const val MAX_DIGITS = 7
