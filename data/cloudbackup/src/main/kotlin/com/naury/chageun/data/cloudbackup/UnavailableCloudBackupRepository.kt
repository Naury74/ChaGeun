package com.naury.chageun.data.cloudbackup

import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import com.naury.chageun.core.domain.cloudbackup.CloudBackupRepository
import com.naury.chageun.core.domain.cloudbackup.CloudResult

/** google-services.json 없이 빌드해 Firebase가 없을 때. */
internal object UnavailableCloudBackupRepository : CloudBackupRepository {
    private val unavailable = CloudResult.Failure(CloudBackupError.Unavailable)

    override suspend fun list() = unavailable

    override suspend fun backUpNow() = unavailable

    override suspend fun download(backupId: String) = unavailable

    override suspend fun delete(backupId: String) = unavailable
}
