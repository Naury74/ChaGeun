package com.naury.chageun.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.designsystem.component.StatusTone
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MissingInput
import kotlin.math.absoluteValue

val MaintenanceState.tone: StatusTone
    get() = when (this) {
        MaintenanceState.Overdue -> StatusTone.Critical
        MaintenanceState.Due, MaintenanceState.Upcoming -> StatusTone.Upcoming
        MaintenanceState.Good -> StatusTone.Good
        MaintenanceState.Unknown -> StatusTone.Unknown
    }

@get:StringRes
val MaintenanceState.labelRes: Int
    get() = when (this) {
        MaintenanceState.Overdue -> R.string.maintenance_state_overdue
        MaintenanceState.Due -> R.string.maintenance_state_due
        MaintenanceState.Upcoming -> R.string.maintenance_state_upcoming
        MaintenanceState.Good -> R.string.maintenance_state_good
        MaintenanceState.Unknown -> R.string.maintenance_state_unknown
    }

/** The dimension that is already past due wins; otherwise distance is shown since it is what drivers track. */
@Composable
fun remainingText(status: MaintenanceStatus): String? {
    val km = status.remainingKm
    val days = status.remainingDays
    return when {
        km != null && km <= 0 -> stringResource(R.string.maintenance_overdue_km, formatNumber(km.absoluteValue))
        days != null && days < 0 ->
            pluralStringResource(
                R.plurals.maintenance_overdue_days,
                days.absoluteValue.toInt(),
                days.absoluteValue.toInt(),
            )
        days == 0L -> stringResource(R.string.maintenance_due_today)
        km != null -> stringResource(R.string.maintenance_remaining_km, formatNumber(km))
        days != null -> pluralStringResource(R.plurals.maintenance_remaining_days, days.toInt(), days.toInt())
        else -> null
    }
}

@Composable
fun missingInputText(status: MaintenanceStatus): String {
    val res = when {
        MissingInput.LastService in status.missingInputs -> R.string.maintenance_missing_last_service
        MissingInput.CurrentMileage in status.missingInputs -> R.string.maintenance_missing_current_mileage
        MissingInput.LastServiceMileage in status.missingInputs -> R.string.maintenance_missing_service_mileage
        else -> R.string.maintenance_missing_service_date
    }
    return stringResource(res)
}
