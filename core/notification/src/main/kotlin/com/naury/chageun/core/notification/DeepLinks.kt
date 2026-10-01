package com.naury.chageun.core.notification

import android.content.Intent
import com.naury.chageun.core.model.MaintenanceItem

sealed interface DeepLink {
    /** Opens the Care tab with [item] selected. */
    data class Maintenance(val item: MaintenanceItem) : DeepLink

    /** Opens the My car tab, where the inspection date lives. */
    data object Inspection : DeepLink
}

/** Notification taps open the launcher activity with the screen to show as extras. */
object DeepLinks {
    private const val EXTRA_MAINTENANCE_ITEM = "com.naury.chageun.extra.MAINTENANCE_ITEM"
    private const val EXTRA_INSPECTION = "com.naury.chageun.extra.INSPECTION"

    fun Intent.putMaintenanceItem(item: MaintenanceItem): Intent = putExtra(EXTRA_MAINTENANCE_ITEM, item.name)

    fun Intent.putInspection(): Intent = putExtra(EXTRA_INSPECTION, true)

    fun Intent.deepLinkOrNull(): DeepLink? {
        if (getBooleanExtra(EXTRA_INSPECTION, false)) return DeepLink.Inspection
        val itemName = getStringExtra(EXTRA_MAINTENANCE_ITEM) ?: return null
        return MaintenanceItem.entries.firstOrNull { it.name == itemName }?.let(DeepLink::Maintenance)
    }
}
