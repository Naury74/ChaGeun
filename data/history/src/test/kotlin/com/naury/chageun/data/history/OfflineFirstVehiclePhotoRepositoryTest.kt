package com.naury.chageun.data.history

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.database.ChageunDatabase
import com.naury.chageun.core.database.entity.VehicleEntity
import com.naury.chageun.core.model.CutoutFailure
import com.naury.chageun.core.model.CutoutStatus
import com.naury.chageun.core.model.VehicleId
import java.io.File
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstVehiclePhotoRepositoryTest {
    private lateinit var database: ChageunDatabase
    private val directory: File = Files.createTempDirectory("vehicle-photo").toFile()
    private val now = Instant.parse("2026-10-01T00:00:00Z")
    private val vehicleId = VehicleId("v1")
    private val silentLogger = object : AppLogger {
        override fun debug(event: String, vararg fields: LogField) = Unit

        override fun warn(event: String, vararg fields: LogField, error: Throwable?) = Unit

        override fun error(event: String, vararg fields: LogField, error: Throwable?) = Unit
    }
    private val importer = ImageImporter { _, target, thumbnail, _ ->
        target.parentFile?.mkdirs()
        target.writeText("image")
        thumbnail.writeText("thumb")
        ImportedImage(target, thumbnail, target.length(), null)
    }

    /** [results]를 차례로 돌려준다. 성공하면 잘라 낸 파일을 쓰고, 그 전에 모델을 내려받는 것처럼 상태를 알린다. */
    private class ScriptedCutter(vararg results: CutoutResult) : SubjectCutter {
        private val queue = ArrayDeque(results.toList())
        val seenStatuses = mutableListOf<CutoutStatus>()

        override suspend fun cutout(source: File, target: File, onStatus: (CutoutStatus) -> Unit): CutoutResult {
            // 실제로는 내려받기와 처리에 시간이 걸린다. 사이를 띄워야 상태가 합쳐지지 않고 하나씩 보인다.
            listOf(CutoutStatus.DownloadingModel(0.5f), CutoutStatus.Processing).forEach {
                delay(STEP_MILLIS)
                seenStatuses += it
                onStatus(it)
            }
            delay(STEP_MILLIS)
            val result = queue.removeFirst()
            if (result == CutoutResult.Success) target.writeText("cutout")
            return result
        }
    }

    private companion object {
        const val STEP_MILLIS = 100L
    }

    @Before
    fun setUp() = runTest {
        database =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ChageunDatabase::class.java)
                .allowMainThreadQueries()
                // 잘라 내기는 Repository 안의 코루틴에서 이어지므로, 쿼리를 그 자리에서 실행해야
                // 테스트 시계를 다 돌렸을 때 DB 작업까지 끝나 있다.
                .setQueryExecutor { it.run() }
                .setTransactionExecutor { it.run() }
                .build()
        database.vehicleDao().upsert(
            VehicleEntity(
                id = "v1", plateNumberEncrypted = null, plateMasked = null, maker = "Maker", model = "Model",
                modelYear = 2023, trim = null, fuelType = "Gasoline", firstRegistrationDate = null, vinEncrypted = null,
                registrationMode = "Manual", isPrimary = true, createdAt = now, updatedAt = now,
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
        directory.deleteRecursively()
    }

    private fun repository(cutter: SubjectCutter, dispatcher: TestDispatcher) = OfflineFirstVehiclePhotoRepository(
        database.attachmentDao(),
        VehiclePhotoImages(importer, cutter),
        directory,
        Clock.fixed(now, ZoneOffset.UTC),
        silentLogger,
        dispatcher,
    )

    @Test
    fun replace_showsOriginalFirst_thenSwitchesToCutout() = runTest {
        val repository = repository(ScriptedCutter(CutoutResult.Success), StandardTestDispatcher(testScheduler))
        val paths = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.observe(vehicleId).toList(paths) }

        repository.replace(vehicleId, "content://car")
        repository.observe(vehicleId).first { it?.endsWith(".png") == true }

        assertThat(paths.filterNotNull().map { it.substringAfterLast('_').substringAfterLast('.') })
            .containsExactly("jpg", "png").inOrder()
        assertThat(repository.observeCutout(vehicleId).first()).isEqualTo(CutoutStatus.Idle)
        assertThat(directory.listFiles().orEmpty().filter { it.name.endsWith(".partial") }).isEmpty()
    }

    @Test
    fun replace_reportsDownloadAndProcessing_whileCutting() = runTest {
        val repository = repository(ScriptedCutter(CutoutResult.Success), StandardTestDispatcher(testScheduler))
        val statuses = mutableListOf<CutoutStatus>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeCutout(vehicleId).toList(statuses)
        }

        repository.replace(vehicleId, "content://car")
        testScheduler.advanceUntilIdle()

        assertThat(statuses).containsExactly(
            CutoutStatus.Idle,
            CutoutStatus.Processing,
            CutoutStatus.DownloadingModel(0.5f),
            CutoutStatus.Processing,
            CutoutStatus.Idle,
        ).inOrder()
    }

    @Test
    fun failedCutout_keepsOriginal_andRemoveBackgroundRetries() = runTest {
        val cutter = ScriptedCutter(CutoutResult.ModelUnavailable, CutoutResult.Success)
        val repository = repository(cutter, StandardTestDispatcher(testScheduler))

        repository.replace(vehicleId, "content://car")
        testScheduler.advanceUntilIdle()
        val failedPath = repository.observe(vehicleId).first()
        val failedStatus = repository.observeCutout(vehicleId).first()
        repository.removeBackground(vehicleId)
        testScheduler.advanceUntilIdle()

        assertThat(failedPath).endsWith(".jpg")
        assertThat(failedStatus).isEqualTo(CutoutStatus.Failed(CutoutFailure.ModelUnavailable))
        assertThat(repository.observe(vehicleId).first()).endsWith("_cutout.png")
        assertThat(repository.observeCutout(vehicleId).first()).isEqualTo(CutoutStatus.Idle)
    }

    @Test
    fun noSubject_isReported_andNoPartialFileRemains() = runTest {
        val repository = repository(ScriptedCutter(CutoutResult.NoSubject), StandardTestDispatcher(testScheduler))

        repository.replace(vehicleId, "content://car")
        testScheduler.advanceUntilIdle()

        assertThat(repository.observeCutout(vehicleId).first()).isEqualTo(CutoutStatus.Failed(CutoutFailure.NoSubject))
        assertThat(directory.listFiles().orEmpty().filter { it.name.endsWith(".partial") }).isEmpty()
    }
}
