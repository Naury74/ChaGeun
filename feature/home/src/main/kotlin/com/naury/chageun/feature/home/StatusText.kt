package com.naury.chageun.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.designsystem.component.StatusTone
import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.model.MissingInput
import com.naury.chageun.core.model.VehicleHealth
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.labelRes
import kotlin.math.absoluteValue

internal val MaintenanceState.tone: StatusTone
    get() = when (this) {
        MaintenanceState.Overdue -> StatusTone.Critical
        MaintenanceState.Due, MaintenanceState.Upcoming -> StatusTone.Upcoming
        MaintenanceState.Good -> StatusTone.Good
        MaintenanceState.Unknown -> StatusTone.Unknown
    }

internal val MaintenanceState.labelRes: Int
    get() = when (this) {
        MaintenanceState.Overdue -> R.string.home_state_overdue
        MaintenanceState.Due -> R.string.home_state_due
        MaintenanceState.Upcoming -> R.string.home_state_upcoming
        MaintenanceState.Good -> R.string.home_state_good
        MaintenanceState.Unknown -> R.string.home_state_unknown
    }

internal val VehicleHealthLevel.tone: StatusTone
    get() = when (this) {
        VehicleHealthLevel.NeedsAttention -> StatusTone.Critical
        VehicleHealthLevel.Upcoming -> StatusTone.Upcoming
        VehicleHealthLevel.InsufficientData -> StatusTone.Unknown
        VehicleHealthLevel.Good -> StatusTone.Good
    }

@Composable
internal fun healthHeadline(health: VehicleHealth): String {
    val headlineItem = health.reasons.firstNotNullOfOrNull { it.item }
    return when (health.level) {
        VehicleHealthLevel.Good -> stringResource(R.string.home_health_good)
        VehicleHealthLevel.Upcoming -> stringResource(R.string.home_health_upcoming, itemName(headlineItem))
        VehicleHealthLevel.NeedsAttention -> {
            val count = health.reasons.count { it.isAttention }
            pluralStringResource(R.plurals.home_health_attention, count, count)
        }
        VehicleHealthLevel.InsufficientData ->
            headlineItem?.let { stringResource(R.string.home_health_insufficient, itemName(it)) }
                ?: stringResource(R.string.home_health_insufficient_generic)
    }
}

/** The dimension that is already past due wins; otherwise distance is shown since it is what drivers track. */
@Composable
internal fun remainingText(status: MaintenanceStatus): String? {
    val km = status.remainingKm
    val days = status.remainingDays
    return when {
        km != null && km <= 0 -> stringResource(R.string.home_overdue_km, formatNumber(km.absoluteValue))
        days != null && days < 0 ->
            pluralStringResource(R.plurals.home_overdue_days, days.absoluteValue.toInt(), days.absoluteValue.toInt())
        days == 0L -> stringResource(R.string.home_due_today)
        km != null -> stringResource(R.string.home_remaining_km, formatNumber(km))
        days != null -> pluralStringResource(R.plurals.home_remaining_days, days.toInt(), days.toInt())
        else -> null
    }
}

@Composable
internal fun missingInputText(status: MaintenanceStatus): String {
    val res = when {
        MissingInput.LastService in status.missingInputs -> R.string.home_missing_last_service
        MissingInput.CurrentMileage in status.missingInputs -> R.string.home_missing_current_mileage
        MissingInput.LastServiceMileage in status.missingInputs -> R.string.home_missing_service_mileage
        else -> R.string.home_missing_service_date
    }
    return stringResource(res)
}

@Composable
private fun itemName(item: MaintenanceItem?): String = item?.let { stringResource(it.labelRes) }.orEmpty()

private val HealthReason.item: MaintenanceItem?
    get() = when (this) {
        is HealthReason.SafetyItemOverdue -> item
        is HealthReason.ItemOverdue -> item
        is HealthReason.ItemDueSoon -> item
        is HealthReason.SafetyItemUnknown -> item
        is HealthReason.ItemUnknown -> item
        HealthReason.ActiveSafetyRecall, HealthReason.InspectionOverdue, HealthReason.InspectionDueSoon -> null
    }

private val HealthReason.isAttention: Boolean
    get() = this is HealthReason.ActiveSafetyRecall ||
        this is HealthReason.InspectionOverdue ||
        this is HealthReason.SafetyItemOverdue ||
        this is HealthReason.ItemOverdue
