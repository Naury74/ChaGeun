package com.naury.chageun.core.notification

import android.content.Intent
import com.naury.chageun.core.model.MaintenanceItem

/** Notification taps open the launcher activity with the item to show in the Care tab. */
object DeepLinks {
    private const val EXTRA_MAINTENANCE_ITEM = "com.naury.chageun.extra.MAINTENANCE_ITEM"

    fun Intent.putMaintenanceItem(item: MaintenanceItem): Intent = putExtra(EXTRA_MAINTENANCE_ITEM, item.name)

    fun Intent.maintenanceItemOrNull(): MaintenanceItem? =
        getStringExtra(EXTRA_MAINTENANCE_ITEM)?.let { name -> MaintenanceItem.entries.firstOrNull { it.name == name } }
}
