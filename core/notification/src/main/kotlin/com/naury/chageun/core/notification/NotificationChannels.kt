package com.naury.chageun.core.notification

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

/** One channel per kind so users can turn each off in system settings. */
object NotificationChannels {
    const val MAINTENANCE = "maintenance"
    const val INSPECTION = "inspection"
    const val RECALL = "recall"

    fun ensureCreated(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        listOf(
            Triple(
                MAINTENANCE,
                R.string.notification_channel_maintenance,
                R.string.notification_channel_maintenance_description,
            ),
            Triple(
                INSPECTION,
                R.string.notification_channel_inspection,
                R.string.notification_channel_inspection_description,
            ),
            Triple(RECALL, R.string.notification_channel_recall, R.string.notification_channel_recall_description),
        ).forEach { (id, name, description) ->
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(id, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(name))
                    .setDescription(context.getString(description))
                    .build(),
            )
        }
    }
}
