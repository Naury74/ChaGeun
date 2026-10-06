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
 * 알림 대상은 매일, 그리고 앱을 시작할 때마다 확인한다. 재부팅 후에는 WorkManager가 주기 작업을 복구하고
 * 알림이 몇 시간 늦게 와도 괜찮으므로 정확한 알람(exact alarm)은 쓰지 않는다.
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
        reevaluateNow()
    }

    /** 앱 시작, 시간대·시각 변경처럼 하루를 기다리지 않고 바로 다시 확인할 때 쓴다. */
    fun reevaluateNow() {
        WorkManager.getInstance(context).enqueueUniqueWork(
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
