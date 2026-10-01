package com.naury.chageun.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
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
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A button that opens a date picker limited to today and earlier, for events that already happened. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDateField(
    date: LocalDate?,
    placeholder: String,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPickerOpen by rememberSaveable { mutableStateOf(false) }

    OutlinedButton(
        onClick = { isPickerOpen = true },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ChageunTheme.spacing.minTouchTarget),
    ) {
        Text(date?.let { formatDate(it) } ?: placeholder)
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
                ) { Text(stringResource(R.string.date_picker_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { isPickerOpen = false }) { Text(stringResource(R.string.date_picker_cancel)) }
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
