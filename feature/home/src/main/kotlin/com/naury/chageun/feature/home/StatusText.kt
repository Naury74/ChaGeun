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

/** [attentionCount]는 '지금 확인' 카드 수와 같아서 헤드라인과 목록 개수가 어긋나지 않는다. */
@Composable
internal fun healthHeadline(health: VehicleHealth, attentionCount: Int): String {
    val headlineItem = health.reasons.firstNotNullOfOrNull { it.item }
    return when (health.level) {
        VehicleHealthLevel.Good -> stringResource(R.string.home_health_good)
        VehicleHealthLevel.Upcoming -> if (health.reasons.firstOrNull() == HealthReason.InspectionDueSoon) {
            stringResource(R.string.home_health_inspection_due)
        } else {
            stringResource(R.string.home_health_upcoming, itemName(headlineItem))
        }
        VehicleHealthLevel.NeedsAttention ->
            pluralStringResource(R.plurals.home_health_attention, attentionCount, attentionCount)
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
