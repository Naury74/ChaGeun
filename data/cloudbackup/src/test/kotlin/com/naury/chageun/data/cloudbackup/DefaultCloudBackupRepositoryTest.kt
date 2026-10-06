package com.naury.chageun.data.cloudbackup

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.backup.BackupRepository
import com.naury.chageun.core.domain.backup.LocalDataSummary
import com.naury.chageun.core.domain.cloudbackup.CloudBackup
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import com.naury.chageun.core.domain.cloudbackup.CloudResult
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.VehicleRegistration
import com.naury.chageun.core.testing.FakeAuthRepository
import com.naury.chageun.core.testing.FakeBackupRepository
import com.naury.chageun.core.testing.FakeVehicleRepository
import java.io.File
import java.nio.file.Files
import java.time.Clock
import java.time.Duration
import java.time.Instant
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
    private val auth = FakeAuthRepository()
    private val local = FakeBackupRepository().apply { summary = LocalDataSummary(1, 12, 3) }
    private val vehicles = FakeVehicleRepository()
    private val remote = FakeRemote()
    private var nextId = 0
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
        auth = auth,
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
                override fun withZone(zone: java.time.ZoneId?) = this
            },
            newBackupId = { "b${nextId++}" },
        ),
        ioDispatcher = StandardTestDispatcher(testScheduler),
    )

    private suspend fun signIn(verified: Boolean = true) {
        auth.signUpWithEmail("driver@example.com", "chageun1")
        if (verified) {
            auth.verifyEmailOutside("driver@example.com")
            auth.reload()
        }
    }

    @Test
    fun backUp_uploadsThenWritesSummary_andCleansLocalFile() = runTest {
        signIn()
        vehicles.register(VehicleRegistration("Kia", "Sportage", 2023, FuelType.Hybrid, Kilometers(40_000)))

        val result = repository().backUpNow()

        val backup = (result as CloudResult.Success).value
        assertThat(remote.calls).containsExactly("upload b0", "metadata b0", "touch", "list").inOrder()
        assertThat(backup.vehicleLabel).isEqualTo("Kia Sportage")
        assertThat(backup.recordCount).isEqualTo(12)
        assertThat(backup.photoCount).isEqualTo(3)
        assertThat(backup.sizeBytes).isEqualTo(3)
        assertThat(directory.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun failedSummary_removesUploadedFile() = runTest {
        signIn()
        remote.failOn = "metadata"

        val result = repository().backUpNow()

        assertThat(result).isEqualTo(CloudResult.Failure(CloudBackupError.Network))
        assertThat(remote.archives).isEmpty()
        assertThat(remote.calls).contains("delete_archive b0")
    }

    @Test
    fun keepsOnlyFiveNewest() = runTest {
        signIn()
        val repository = repository()

        repeat(7) {
            repository.backUpNow()
            now = now.plus(Duration.ofDays(1))
        }

        val ids = (repository.list() as CloudResult.Success).value.map { it.id }
        assertThat(ids).containsExactly("b6", "b5", "b4", "b3", "b2").inOrder()
        assertThat(remote.archives.keys).containsExactly("b6", "b5", "b4", "b3", "b2")
    }

    @Test
    fun guardsSignInVerificationAndEmptyData() = runTest {
        val repository = repository()
        assertThat(repository.backUpNow()).isEqualTo(CloudResult.Failure(CloudBackupError.NotSignedIn))

        signIn(verified = false)
        assertThat(repository.backUpNow()).isEqualTo(CloudResult.Failure(CloudBackupError.EmailNotVerified))
        assertThat(repository.list()).isInstanceOf(CloudResult.Success::class.java)

        auth.verifyEmailOutside("driver@example.com")
        auth.reload()
        local.summary = LocalDataSummary(0, 0, 0)
        remote.calls.clear()
        assertThat(repository.backUpNow()).isEqualTo(CloudResult.Failure(CloudBackupError.NoData))
        assertThat(remote.calls).isEmpty()
    }

    @Test
    fun download_returnsLocalFileUri_andDeleteRemovesBoth() = runTest {
        signIn()
        val repository = repository()
        repository.backUpNow()

        val uri = (repository.download("b0") as CloudResult.Success).value
        repository.delete("b0")

        assertThat(File(checkNotNull(Uri.parse(uri).path)).readText()).isEqualTo("zip")
        assertThat(remote.archives).isEmpty()
        assertThat(remote.metadata).isEmpty()
    }

    private class FakeRemote : CloudBackupRemote {
        val archives = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, CloudBackup>()
        val calls = mutableListOf<String>()
        var failOn: String? = null

        private fun record(call: String) {
            calls += call
            if (failOn != null && call.startsWith(failOn!!)) throw CloudBackupException(CloudBackupError.Network)
        }

        override suspend fun uploadArchive(uid: String, backupId: String, file: File) {
            record("upload $backupId")
            archives[backupId] = file.readText()
        }

        override suspend fun writeMetadata(uid: String, backup: CloudBackup) {
            record("metadata ${backup.id}")
            metadata[backup.id] = backup
        }

        override suspend fun touchUser(uid: String, backedUpAt: Instant) = record("touch")

        override suspend fun listMetadata(uid: String): List<CloudBackup> {
            record("list")
            return metadata.values.sortedByDescending { it.createdAt }
        }

        override suspend fun downloadArchive(uid: String, backupId: String, target: File) {
            record("download $backupId")
            target.writeText(archives.getValue(backupId))
        }

        override suspend fun deleteArchive(uid: String, backupId: String) {
            record("delete_archive $backupId")
            archives -= backupId
        }

        override suspend fun deleteMetadata(uid: String, backupId: String) {
            record("delete_metadata $backupId")
            metadata -= backupId
        }
    }
}
