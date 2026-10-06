package com.naury.chageun.data.cloudbackup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import com.naury.chageun.core.testing.FakeCloudBackupRepository
import com.naury.chageun.core.testing.FakeSettingsRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AutoBackupTest {

    private val settings = FakeSettingsRepository()
    private val drive = FakeDriveAccess(connected = false)
    private val cloud = FakeCloudBackupRepository()
    private val silentLogger = object : AppLogger {
        override fun debug(event: String, vararg fields: LogField) = Unit

        override fun warn(event: String, vararg fields: LogField, error: Throwable?) = Unit

        override fun error(event: String, vararg fields: LogField, error: Throwable?) = Unit
    }

    @Test
    fun sync_schedulesOnlyWhenEnabledAndConnected() = runTest(UnconfinedTestDispatcher()) {
        val scheduler = RecordingScheduler()
        AutoBackupSync(settings, drive, scheduler).start(backgroundScope)
        assertThat(scheduler.calls).containsExactly("cancel")

        settings.setCloudAutoBackupEnabled(true)
        assertThat(scheduler.calls.last()).isEqualTo("cancel")

        drive.connect()
        assertThat(scheduler.calls.last()).isEqualTo("schedule")

        drive.disconnect()
        assertThat(scheduler.calls).containsExactly("cancel", "schedule", "cancel").inOrder()
    }

    @Test
    fun worker_backsUp_retriesNetwork_andStopsOnDataProblems() = runTest {
        settings.setCloudAutoBackupEnabled(true)

        assertThat(runWorker()).isEqualTo(ListenableWorker.Result.success())
        assertThat(cloud.backups).hasSize(1)

        cloud.nextError = CloudBackupError.Network
        assertThat(runWorker()).isEqualTo(ListenableWorker.Result.retry())

        cloud.nextError = CloudBackupError.Network
        assertThat(runWorker(attempt = 3)).isEqualTo(ListenableWorker.Result.failure())

        cloud.nextError = CloudBackupError.NoData
        assertThat(runWorker()).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun worker_doesNothing_whenTurnedOff() = runTest {
        assertThat(runWorker()).isEqualTo(ListenableWorker.Result.success())
        assertThat(cloud.backups).isEmpty()
    }

    private suspend fun runWorker(attempt: Int = 0): ListenableWorker.Result {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<CloudBackupWorker>(context)
            .setRunAttemptCount(attempt)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ) = CloudBackupWorker(appContext, workerParameters, cloud, settings, silentLogger)
                },
            )
            .build()
        return worker.doWork()
    }

    private class RecordingScheduler : AutoBackupScheduler {
        val calls = mutableListOf<String>()

        override fun schedule() {
            calls += "schedule"
        }

        override fun cancel() {
            calls += "cancel"
        }
    }
}
