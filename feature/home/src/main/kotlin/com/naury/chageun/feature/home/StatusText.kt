package com.naury.chageun.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.designsystem.component.StatusTone
import com.naury.chageun.core.model.HealthReason
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.VehicleHealth
import com.naury.chageun.core.model.VehicleHealthLevel
import com.naury.chageun.core.ui.labelRes

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
