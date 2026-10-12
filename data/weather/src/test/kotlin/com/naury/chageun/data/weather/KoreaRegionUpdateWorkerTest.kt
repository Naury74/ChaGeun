package com.naury.chageun.data.weather

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.weather.KoreaRegionCheck
import com.naury.chageun.core.testing.FakeKoreaRegionUpdates
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KoreaRegionUpdateWorkerTest {

    private val updates = FakeKoreaRegionUpdates()

    @Test
    fun succeeds_whenUpToDateOrUpdated() = runTest {
        assertThat(run()).isEqualTo(ListenableWorker.Result.success())

        updates.result = KoreaRegionCheck.Updated("2026-10")
        assertThat(run()).isEqualTo(ListenableWorker.Result.success())
        assertThat(updates.checks).isEqualTo(2)
    }

    @Test
    fun retriesNetworkFailuresAFewTimes_andGivesUpOnBadData() = runTest {
        updates.result = KoreaRegionCheck.Failed(isTemporary = true)
        assertThat(run()).isEqualTo(ListenableWorker.Result.retry())
        assertThat(run(attempt = 3)).isEqualTo(ListenableWorker.Result.failure())

        updates.result = KoreaRegionCheck.Failed(isTemporary = false)
        assertThat(run()).isEqualTo(ListenableWorker.Result.failure())
    }

    private suspend fun run(attempt: Int = 0): ListenableWorker.Result {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<KoreaRegionUpdateWorker>(context)
            .setRunAttemptCount(attempt)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ) = KoreaRegionUpdateWorker(appContext, workerParameters, updates)
                },
            )
            .build()
            .doWork()
    }
}
