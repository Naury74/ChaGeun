package com.naury.chageun.core.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.naury.chageun.core.domain.reminder.ReminderNotifier
import com.naury.chageun.core.model.InspectionStatus
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.MaintenanceState
import com.naury.chageun.core.model.MaintenanceStatus
import com.naury.chageun.core.notification.DeepLinks.putInspection
import com.naury.chageun.core.notification.DeepLinks.putMaintenanceItem
import com.naury.chageun.core.notification.DeepLinks.putMileageUpdate
import com.naury.chageun.core.ui.labelRes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import javax.inject.Inject
import kotlin.math.absoluteValue

internal class MaintenanceReminderNotifier @Inject constructor(@ApplicationContext private val context: Context) :
    ReminderNotifier {

    private val manager = NotificationManagerCompat.from(context)

    override fun canNotify(): Boolean = manager.areNotificationsEnabled()

    // canNotify()는 호출부에서 notify() 직전에 확인한다. lint는 그 경계 너머를 보지 못한다.
    @SuppressLint("MissingPermission")
    override fun notify(statuses: List<MaintenanceStatus>) {
        NotificationChannels.ensureCreated(context)
        statuses.forEach { status -> manager.notify(status.item.notificationId, build(status)) }
    }

    override fun cancel(item: MaintenanceItem) = manager.cancel(item.notificationId)

    @SuppressLint("MissingPermission")
    override fun notifyInspection(status: InspectionStatus) {
        val daysLeft = status.daysLeft ?: return
        NotificationChannels.ensureCreated(context)
        val resources = context.resources
        val title = if (daysLeft < 0) {
            context.getString(R.string.notification_inspection_overdue_title)
        } else {
            context.getString(R.string.notification_inspection_due_title)
        }
        val days = daysLeft.absoluteValue.toInt()
        val text = when {
            daysLeft < 0 -> resources.getQuantityString(R.plurals.notification_inspection_overdue_days, days, days)
            daysLeft == 0L -> context.getString(R.string.notification_inspection_due_today)
            else -> resources.getQuantityString(R.plurals.notification_inspection_days_left, days, days)
        }
        val notification = NotificationCompat.Builder(context, NotificationChannels.INSPECTION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(launchIntent(INSPECTION_REQUEST_CODE) { putInspection() })
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        manager.notify(INSPECTION_NOTIFICATION_ID, notification)
    }

    override fun cancelInspection() = manager.cancel(INSPECTION_NOTIFICATION_ID)

    @SuppressLint("MissingPermission")
    override fun notifyMileagePrompt() {
        NotificationChannels.ensureCreated(context)
        val notification = NotificationCompat.Builder(context, NotificationChannels.MAINTENANCE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_mileage_title))
            .setContentText(context.getString(R.string.notification_mileage_text))
            .setContentIntent(launchIntent(MILEAGE_REQUEST_CODE) { putMileageUpdate() })
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        manager.notify(MILEAGE_NOTIFICATION_ID, notification)
    }

    private fun build(status: MaintenanceStatus) = NotificationCompat.Builder(context, NotificationChannels.MAINTENANCE)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(status.state.titleRes, context.getString(status.item.labelRes)))
        .setContentText(remainingText(status))
        .setContentIntent(launchIntent(status.item.ordinal) { putMaintenanceItem(status.item) })
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .build()

    private fun launchIntent(requestCode: Int, deepLink: Intent.() -> Intent): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            ?.deepLink()
            ?: return null
        return PendingIntent.getActivity(
            context,
            requestCode,
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

    private val MaintenanceItem.notificationId: Int
        get() = ordinal + NOTIFICATION_ID_OFFSET

    private val MaintenanceState.titleRes: Int
        get() = when (this) {
            MaintenanceState.Overdue -> R.string.notification_overdue_title
            MaintenanceState.Due -> R.string.notification_due_title
            else -> R.string.notification_upcoming_title
        }

    private companion object {
        const val NOTIFICATION_ID_OFFSET = 1_000
        const val INSPECTION_NOTIFICATION_ID = 2_000
        const val INSPECTION_REQUEST_CODE = 2_000
        const val MILEAGE_NOTIFICATION_ID = 3_000
        const val MILEAGE_REQUEST_CODE = 3_000
    }
}
