package com.naury.chageun.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem

@get:StringRes
val TimelineEventType.labelRes: Int
    get() = when (this) {
        TimelineEventType.Maintenance -> R.string.timeline_type_maintenance
        TimelineEventType.Fuel -> R.string.timeline_type_fuel
        TimelineEventType.Inspection -> R.string.timeline_type_inspection
        TimelineEventType.Repair -> R.string.timeline_type_repair
        TimelineEventType.Note -> R.string.timeline_type_note
    }

/** Maintenance rows are named by their item; fuel rows add the station; other rows use their own title. */
@Composable
fun timelineTitle(item: TimelineItem): String {
    val typeLabel = stringResource(item.ref.type.labelRes)
    val maintenanceItem = item.maintenanceItem
    return when {
        maintenanceItem != null -> stringResource(maintenanceItem.labelRes)
        item.ref.type == TimelineEventType.Fuel -> listOfNotNull(typeLabel, item.title).joinToString(" · ")
        else -> item.title ?: typeLabel
    }
}
