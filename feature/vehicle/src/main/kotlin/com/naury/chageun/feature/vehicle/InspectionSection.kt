package com.naury.chageun.feature.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.ToneColors
import com.naury.chageun.core.model.InspectionRecord
import com.naury.chageun.core.model.InspectionSource
import com.naury.chageun.core.model.InspectionState
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.ui.ItemIconBadge
import com.naury.chageun.core.ui.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.absoluteValue

@Composable
internal fun InspectionCard(
    status: InspectionStatus,
    history: List<InspectionRecord>,
    currentMileage: Kilometers?,
    onDateSelected: (LocalDate?) -> Unit,
    onCompleted: (InspectionCompletion) -> Unit,
) {
    var isPickerOpen by rememberSaveable { mutableStateOf(false) }
    var isCompleting by rememberSaveable { mutableStateOf(false) }
    val schedule = status.schedule
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            ) {
                ItemIconBadge(Icons.AutoMirrored.Filled.FactCheck, status.state.tone(), size = 44.dp)
                Text(
                    stringResource(R.string.vehicle_inspection_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            if (schedule == null) {
                Text(stringResource(R.string.vehicle_inspection_empty), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                    FilledTonalButton(onClick = { isPickerOpen = true }) {
                        Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(ChageunTheme.spacing.xs))
                        Text(stringResource(R.string.vehicle_inspection_add))
                    }
                    TextButton(onClick = { isCompleting = true }) {
                        Text(stringResource(R.string.vehicle_inspection_complete))
                    }
                }
            } else {
                Column(Modifier.semantics(mergeDescendants = true) {}) {
                    Text(formatDate(schedule.nextDueDate), style = MaterialTheme.typography.titleLarge)
                    Text(
                        remainingText(status),
                        style = MaterialTheme.typography.bodyLarge,
                        color = status.state.color(),
                    )
                    Text(
                        stringResource(schedule.source.labelRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                    FilledTonalButton(onClick = { isCompleting = true }) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(ChageunTheme.spacing.xs))
                        Text(stringResource(R.string.vehicle_inspection_complete))
                    }
                    TextButton(onClick = { isPickerOpen = true }) {
                        Text(stringResource(R.string.vehicle_inspection_change))
                    }
                    TextButton(onClick = { onDateSelected(null) }) {
                        Text(stringResource(R.string.vehicle_inspection_clear))
                    }
                }
            }
            InspectionHistory(history)
        }
    }
    if (isCompleting) {
        CompleteInspectionDialog(
            today = LocalDate.now(),
            previousDueDate = schedule?.nextDueDate,
            currentMileage = currentMileage,
            onConfirm = {
                onCompleted(it)
                isCompleting = false
            },
            onDismiss = { isCompleting = false },
        )
    }
    if (isPickerOpen) {
        InspectionDatePicker(
            initial = schedule?.nextDueDate,
            onConfirm = {
                onDateSelected(it)
                isPickerOpen = false
            },
            onDismiss = { isPickerOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InspectionDatePicker(initial: LocalDate?, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // DatePicker는 UTC 자정 기준 millis를 사용한다.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
                        onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(R.string.vehicle_inspection_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.vehicle_inspection_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun remainingText(status: InspectionStatus): String {
    val days = status.daysLeft ?: return ""
    val count = days.absoluteValue.toInt()
    return when {
        days < 0 -> pluralStringResource(R.plurals.vehicle_inspection_days_overdue, count, count)
        days == 0L -> stringResource(R.string.vehicle_inspection_due_today)
        else -> pluralStringResource(R.plurals.vehicle_inspection_days_left, count, count)
    }
}

@Composable
private fun InspectionState.tone(): ToneColors = when (this) {
    InspectionState.Overdue -> ChageunTheme.colors.critical
    InspectionState.DueSoon -> ChageunTheme.colors.upcoming
    InspectionState.Ok -> ChageunTheme.colors.good
    InspectionState.Unknown -> ChageunTheme.colors.unknown
}

@Composable
private fun InspectionState.color(): Color = when (this) {
    InspectionState.Overdue -> ChageunTheme.colors.critical.content
    InspectionState.DueSoon -> ChageunTheme.colors.upcoming.content
    InspectionState.Ok, InspectionState.Unknown -> MaterialTheme.colorScheme.onSurface
}

private val InspectionSource.labelRes: Int
    get() = when (this) {
        InspectionSource.User -> R.string.vehicle_inspection_source_user
        InspectionSource.Official -> R.string.vehicle_inspection_source_official
    }
