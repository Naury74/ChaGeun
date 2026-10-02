package com.naury.chageun.core.notification

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

/** 종류마다 채널을 하나씩 둬서 사용자가 시스템 설정에서 각각 끌 수 있게 한다. */
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
