package com.naury.chageun.data.maintenance

import com.naury.chageun.core.database.entity.MaintenanceRecordEntity
import com.naury.chageun.core.database.entity.MaintenanceRuleEntity
import com.naury.chageun.core.database.entity.MileageRecordEntity
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceRule
import com.naury.chageun.core.model.MaintenanceThresholds
import com.naury.chageun.core.model.MileageReading
import com.naury.chageun.core.model.RuleSource
import com.naury.chageun.core.model.ServiceRecord

internal fun String.toMaintenanceItemOrNull() = MaintenanceItem.entries.firstOrNull { it.name == this }

/** Rows written by a newer app version may reference items this build does not know; they are skipped. */
internal fun MaintenanceRuleEntity.asExternalModelOrNull(): MaintenanceRule? {
    val item = itemType.toMaintenanceItemOrNull() ?: return null
    return MaintenanceRule(
        item = item,
        intervalKm = intervalKm,
        intervalMonths = intervalMonths,
        thresholds = MaintenanceThresholds(
            dueSoonKm = dueSoonKm,
            dueSoonDays = dueSoonDays,
            upcomingKm = upcomingKm,
            upcomingDays = upcomingDays,
        ),
        source = RuleSource.entries.firstOrNull { it.name == ruleSource } ?: RuleSource.Generic,
        sourceTitle = sourceTitle,
        sourceUrl = sourceUrl,
        isEnabled = isEnabled,
    )
}

internal fun MaintenanceRecordEntity.asServiceRecord() = ServiceRecord(
    date = serviceDate,
    mileage = mileageKm?.let(::Kilometers),
)

internal fun MileageRecordEntity.asExternalModel() = MileageReading(
    date = recordedOn,
    mileage = Kilometers(mileageKm),
)
