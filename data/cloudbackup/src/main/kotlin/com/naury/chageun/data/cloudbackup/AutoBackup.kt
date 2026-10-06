package com.naury.chageun.data.cloudbackup

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
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.domain.auth.AuthRepository
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import com.naury.chageun.core.domain.cloudbackup.CloudBackupRepository
import com.naury.chageun.core.domain.cloudbackup.CloudResult
import com.naury.chageun.core.domain.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** 자동 백업 작업 예약. 테스트에서 WorkManager 없이 바꿔 끼울 수 있게 나눴다. */
interface AutoBackupScheduler {
    fun schedule()

    fun cancel()
}

/**
 * 주 1회, 데이터 요금이 없는 네트워크에서 충전 중이고 저장공간이 넉넉할 때만 돈다.
 * 처음 켰을 때는 조건이 맞는 즉시 한 번 돈다.
 */
internal class PeriodicAutoBackupScheduler @Inject constructor(@ApplicationContext private val context: Context) :
    AutoBackupScheduler {

    override fun schedule() {
        val request = PeriodicWorkRequestBuilder<CloudBackupWorker>(INTERVAL_DAYS, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresCharging(true)
                    .setRequiresStorageNotLow(true)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MINUTES, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "cloud-auto-backup"
        const val INTERVAL_DAYS = 7L
        const val BACKOFF_MINUTES = 30L
    }
}

/**
 * 자동 백업 스위치와 로그인 상태를 보고 예약을 맞춘다. 켜져 있어도 로그아웃했거나 이메일 인증 전이면
 * 돌려도 실패하므로 예약하지 않는다.
 */
class AutoBackupSync @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val scheduler: AutoBackupScheduler,
) {
    fun start(scope: CoroutineScope) {
        combine(settingsRepository.settings, authRepository.currentUser) { settings, user ->
            settings.isCloudAutoBackupEnabled && user != null && !user.needsEmailVerification
        }
            .distinctUntilChanged()
            .onEach { shouldRun -> if (shouldRun) scheduler.schedule() else scheduler.cancel() }
            .launchIn(scope)
    }
}

/** 다시 시도해도 될 실패인지. 네트워크처럼 지나가는 문제만 다시 하고, 설정·데이터 문제는 다음 주기를 기다린다. */
internal fun CloudBackupError.isWorthRetrying(): Boolean =
    this == CloudBackupError.Network || this == CloudBackupError.Unknown

@HiltWorker
internal class CloudBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cloudBackups: CloudBackupRepository,
    private val settingsRepository: SettingsRepository,
    private val logger: AppLogger,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 예약을 취소하기 전에 이미 시작된 작업일 수 있다.
        if (!settingsRepository.settings.first().isCloudAutoBackupEnabled) return Result.success()
        return when (val result = cloudBackups.backUpNow()) {
            is CloudResult.Success -> Result.success()
            is CloudResult.Failure -> {
                logger.warn("cloud_auto_backup_failed", LogField.ErrorType(result.error.name))
                if (result.error.isWorthRetrying() &&
                    runAttemptCount < MAX_ATTEMPTS
                ) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
