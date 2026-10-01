package com.naury.chageun.core.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Reminders are evaluated daily and on every app start. WorkManager restores the periodic work after reboot,
 * and a reminder arriving a few hours late is acceptable, so exact alarms are not used.
 */
class ReminderScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    fun schedule() {
        NotificationChannels.ensureCreated(context)
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(
            DAILY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build(),
        )
        workManager.enqueueUniqueWork(
            ON_START_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>().build(),
        )
    }

    private companion object {
        const val DAILY_WORK = "reminder-daily"
        const val ON_START_WORK = "reminder-on-start"
    }
}
