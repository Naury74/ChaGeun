package com.naury.chageun.feature.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.ChoicePill
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.vehicle.CompleteInspectionUseCase
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.PeriodicInspectionResult
import com.naury.chageun.core.ui.ChageunDialog
import com.naury.chageun.core.ui.DialogAction
import com.naury.chageun.core.ui.DialogActionStyle
import com.naury.chageun.core.ui.FormLabel
import com.naury.chageun.core.ui.NumberInputField
import com.naury.chageun.core.ui.QuickDateField
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.labelRes
import java.time.LocalDate

data class InspectionCompletion(
    val completedOn: LocalDate,
    val mileage: Kilometers?,
    val result: PeriodicInspectionResult,
    val nextDueDate: LocalDate,
)

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
    var result by rememberSaveable { mutableStateOf(PeriodicInspectionResult.Passed) }
    // 사용자가 날짜를 고르기 전까지는 null로 두어 제안값이 검사일을 따라가게 한다.
    var chosenNextDue by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var isPickingNextDue by rememberSaveable { mutableStateOf(false) }
    val nextDue = chosenNextDue ?: CompleteInspectionUseCase.suggestNextDueDate(completedOn, previousDueDate)

    ChageunDialog(
        title = stringResource(R.string.vehicle_inspection_complete_title),
        icon = Icons.AutoMirrored.Filled.FactCheck,
        onDismiss = onDismiss,
        actions = listOf(
            DialogAction(stringResource(R.string.vehicle_inspection_confirm)) {
                onConfirm(InspectionCompletion(completedOn, mileage.toLongOrNull()?.let(::Kilometers), result, nextDue))
            },
            DialogAction(
                stringResource(R.string.vehicle_inspection_cancel),
                DialogActionStyle.Secondary,
                onClick = onDismiss,
            ),
        ),
    ) {
        FormLabel(stringResource(R.string.vehicle_inspection_complete_date))
        QuickDateField(date = completedOn, onDateSelected = { completedOn = it }, today = today)
        NumberInputField(
            value = mileage,
            onValueChange = { input -> mileage = input.filter(Char::isDigit).take(MAX_DIGITS) },
            label = stringResource(R.string.vehicle_inspection_complete_mileage),
            unit = stringResource(R.string.vehicle_unit_km),
        )
        FormLabel(stringResource(R.string.vehicle_inspection_complete_result))
        Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
            RESULT_CHOICES.forEach { choice ->
                ChoicePill(stringResource(choice.labelRes), result == choice, { result = choice })
            }
        }
        FormLabel(stringResource(R.string.vehicle_inspection_complete_next))
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

// 검사 받은 자리에서 결과를 모르는 경우는 없으므로 미기록은 고를 수 없게 한다.
private val RESULT_CHOICES = listOf(PeriodicInspectionResult.Passed, PeriodicInspectionResult.Failed)
