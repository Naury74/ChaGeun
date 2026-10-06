package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.cloudbackup.CloudBackup
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import com.naury.chageun.core.domain.cloudbackup.CloudBackupRepository
import com.naury.chageun.core.domain.cloudbackup.CloudResult
import java.time.Instant

/** 메모리에 백업 요약을 둔다. [nextError]를 정하면 다음 요청 한 번이 그 오류로 실패한다. */
class FakeCloudBackupRepository(private val now: () -> Instant = { Instant.parse("2026-10-06T00:00:00Z") }) :
    CloudBackupRepository {
    val backups = mutableListOf<CloudBackup>()
    var nextError: CloudBackupError? = null
    private var nextId = 0

    override suspend fun list(): CloudResult<List<CloudBackup>> = respond {
        backups.sortedByDescending { it.createdAt }
    }

    override suspend fun backUpNow(): CloudResult<CloudBackup> = respond {
        backup("b${nextId++}", now().plusSeconds(nextId.toLong())).also { backups += it }
    }

    override suspend fun download(backupId: String): CloudResult<String> = respond { "file:///cache/$backupId.zip" }

    override suspend fun delete(backupId: String): CloudResult<Unit> = respond {
        backups.removeAll { it.id == backupId }
    }

    private inline fun <T> respond(block: () -> T): CloudResult<T> {
        val error = nextError ?: return CloudResult.Success(block())
        nextError = null
        return CloudResult.Failure(error)
    }

    companion object {
        fun backup(id: String, createdAt: Instant, records: Int = 12, photos: Int = 3) = CloudBackup(
            id = id,
            createdAt = createdAt,
            sizeBytes = 2_400_000,
            schemaVersion = 1,
            appVersion = "1.0.0",
            deviceModel = "Pixel 9 Pro Fold",
            vehicleLabel = "Kia Sportage",
            recordCount = records,
            photoCount = photos,
        )
    }
}
