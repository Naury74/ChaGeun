package com.naury.chageun.core.notification

import android.content.Intent
import com.naury.chageun.core.model.MaintenanceItem

sealed interface DeepLink {
    /** [item]이 선택된 상태로 Care 탭을 연다. */
    data class Maintenance(val item: MaintenanceItem) : DeepLink

    /** 검사일이 있는 My car 탭을 연다. */
    data object Inspection : DeepLink
}

/** 알림을 누르면 보여 줄 화면을 extra로 담아 런처 Activity를 연다. */
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
