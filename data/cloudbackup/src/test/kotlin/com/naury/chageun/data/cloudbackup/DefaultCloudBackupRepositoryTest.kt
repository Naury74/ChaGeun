package com.naury.chageun.data.cloudbackup

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.backup.BackupRepository
import com.naury.chageun.core.domain.backup.LocalDataSummary
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import com.naury.chageun.core.domain.cloudbackup.CloudResult
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeBackupRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import java.io.File
import java.nio.file.Files
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultCloudBackupRepositoryTest {

    private val directory: File = Files.createTempDirectory("cloud").toFile()
    private val drive = FakeDriveAccess()
    private val local = FakeBackupRepository().apply { summary = LocalDataSummary(1, 12, 3) }
    private val vehicles = FakeVehicleRepository()
    private val remote = FakeRemote()
    private var nextName = 0
    private var now = Instant.parse("2026-10-06T00:00:00Z")

    /** 실제 내보내기처럼 받은 URI의 파일에 내용을 쓴다. */
    private val writingLocal = object : BackupRepository by local {
        override suspend fun export(destinationUri: String): Boolean {
            if (!local.export(destinationUri)) return false
            File(checkNotNull(Uri.parse(destinationUri).path)).writeText("zip")
            return true
        }
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun TestScope.repository() = DefaultCloudBackupRepository(
        drive = drive,
        local = writingLocal,
        vehicles = vehicles,
        remote = remote,
        environment = CloudBackupEnvironment(
            workDirectory = directory,
            appVersion = "1.2.0",
            deviceModel = "Pixel 9 Pro Fold",
            clock = object : Clock() {
                override fun instant() = now

                override fun getZone() = ZoneOffset.UTC

                override fun withZone(zone: ZoneId?) = this
            },
            newBackupId = { "b${nextName++}" },
        ),
        ioDispatcher = StandardTestDispatcher(testScheduler),
    )

    @Test
    fun backUp_uploadsWithSummary_andCleansLocalFile() = runTest {
        vehicles.register(VehicleRegistration("Kia", "Sportage", 2023, FuelType.Hybrid, Kilometers(40_000)))

        val backup = (repository().backUpNow() as CloudResult.Success).value

        assertThat(remote.calls).containsExactly("upload b0", "list").inOrder()
        assertThat(backup.id).isEqualTo("drive-0")
        assertThat(backup.vehicleLabel).isEqualTo("Kia Sportage")
        assertThat(backup.recordCount).isEqualTo(12)
        assertThat(backup.sizeBytes).isEqualTo(3)
        assertThat(directory.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun keepsOnlyFiveNewest() = runTest {
        val repository = repository()

        repeat(7) {
            repository.backUpNow()
            now = now.plus(Duration.ofDays(1))
        }

        val ids = (repository.list() as CloudResult.Success).value.map { it.id }
        assertThat(ids).containsExactly("drive-6", "drive-5", "drive-4", "drive-3", "drive-2").inOrder()
    }

    @Test
    fun needsConnection_andVehicle() = runTest {
        val repository = repository()
        drive.disconnect()
        assertThat(repository.backUpNow()).isEqualTo(CloudResult.Failure(CloudBackupError.NotConnected))

        drive.isConnected.value = true
        local.summary = LocalDataSummary(0, 0, 0)
        assertThat(repository.backUpNow()).isEqualTo(CloudResult.Failure(CloudBackupError.NoData))
        assertThat(remote.calls).isEmpty()
    }

    @Test
    fun expiredPermission_disconnects() = runTest {
        remote.failOn = "list"
        remote.failWith = CloudBackupError.NotConnected

        assertThat(repository().list()).isEqualTo(CloudResult.Failure(CloudBackupError.NotConnected))
        assertThat(drive.isConnected.value).isFalse()
    }

    @Test
    fun pruneFailure_stillReportsBackup() = runTest {
        remote.failOn = "list"

        assertThat(repository().backUpNow()).isInstanceOf(CloudResult.Success::class.java)
    }

    @Test
    fun download_returnsLocalFileUri_andDeleteRemoves() = runTest {
        val repository = repository()
        val id = (repository.backUpNow() as CloudResult.Success).value.id

        val uri = (repository.download(id) as CloudResult.Success).value
        repository.delete(id)

        assertThat(File(checkNotNull(Uri.parse(uri).path)).readText()).isEqualTo("zip")
        assertThat(remote.files).isEmpty()
    }
}
