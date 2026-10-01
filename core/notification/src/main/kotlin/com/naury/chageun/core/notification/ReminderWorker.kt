package com.naury.chageun.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.domain.reminder.EvaluateRemindersUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
internal class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val evaluateReminders: EvaluateRemindersUseCase,
    private val logger: AppLogger,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching { evaluateReminders() }
        .fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                logger.warn("reminder_evaluation_failed", LogField.Success(false), error = error)
                if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
            },
        )

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
