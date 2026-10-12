package com.naury.chageun.data.weather

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.naury.chageun.core.domain.weather.KoreaRegionCheck
import com.naury.chageun.core.domain.weather.KoreaRegionUpdates
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 지역 목록 확인을 주 1회 예약한다. 네트워크가 연결되어 있고 배터리가 부족하지 않을 때만 돈다.
 * 이미 예약되어 있으면 그대로 둔다.
 */
class KoreaRegionUpdateScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    fun schedule() {
        val request = PeriodicWorkRequestBuilder<KoreaRegionUpdateWorker>(INTERVAL_DAYS, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_HOURS, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NAME = "korea-region-list-update"
        const val INTERVAL_DAYS = 7L
        const val BACKOFF_HOURS = 1L
    }
}

/** 네트워크 문제만 몇 번 다시 하고, 받은 파일이 틀렸으면 다음 주기를 기다린다. 어느 쪽이든 가진 목록은 그대로다. */
@HiltWorker
internal class KoreaRegionUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updates: KoreaRegionUpdates,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (val result = updates.checkNow()) {
        KoreaRegionCheck.UpToDate, is KoreaRegionCheck.Updated -> Result.success()
        is KoreaRegionCheck.Failed ->
            if (result.isTemporary && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
