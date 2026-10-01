package com.naury.chageun.core.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.naury.chageun.core.domain.reminder.ReminderNotifier
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.notification.DeepLinks.putMaintenanceItem
import com.naury.chageun.core.ui.labelRes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import javax.inject.Inject
import kotlin.math.absoluteValue

internal class MaintenanceReminderNotifier @Inject constructor(@ApplicationContext private val context: Context) :
    ReminderNotifier {

    private val manager = NotificationManagerCompat.from(context)

    override fun canNotify(): Boolean = manager.areNotificationsEnabled()

    // canNotify() is checked by the caller right before notify(); lint cannot see across that boundary.
    @SuppressLint("MissingPermission")
    override fun notify(statuses: List<MaintenanceStatus>) {
        NotificationChannels.ensureCreated(context)
        statuses.forEach { status -> manager.notify(status.item.ordinal + NOTIFICATION_ID_OFFSET, build(status)) }
    }

    private fun build(status: MaintenanceStatus) = NotificationCompat.Builder(context, NotificationChannels.MAINTENANCE)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(status.state.titleRes, context.getString(status.item.labelRes)))
        .setContentText(remainingText(status))
        .setContentIntent(openItem(status))
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .build()

    private fun openItem(status: MaintenanceStatus): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            ?.putMaintenanceItem(status.item)
            ?: return null
        return PendingIntent.getActivity(
            context,
            status.item.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun remainingText(status: MaintenanceStatus): String? {
        val km = status.remainingKm
        val days = status.remainingDays
        val format = NumberFormat.getIntegerInstance()
        val resources = context.resources
        return when {
            km != null && km <= 0 -> context.getString(
                R.string.notification_overdue_km,
                format.format(km.absoluteValue),
            )
            days != null && days < 0 -> days.absoluteValue.toInt().let {
                resources.getQuantityString(R.plurals.notification_overdue_days, it, it)
            }
            km != null -> context.getString(R.string.notification_remaining_km, format.format(km))
            days != null -> resources.getQuantityString(
                R.plurals.notification_remaining_days,
                days.toInt(),
                days.toInt(),
            )
            else -> null
        }
    }

    private val MaintenanceState.titleRes: Int
        get() = when (this) {
            MaintenanceState.Overdue -> R.string.notification_overdue_title
            MaintenanceState.Due -> R.string.notification_due_title
            else -> R.string.notification_upcoming_title
        }

    private companion object {
        const val NOTIFICATION_ID_OFFSET = 1_000
    }
}
