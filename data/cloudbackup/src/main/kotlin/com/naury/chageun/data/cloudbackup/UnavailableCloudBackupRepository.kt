package com.naury.chageun.data.cloudbackup

import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import com.naury.chageun.core.domain.cloudbackup.CloudBackupRepository
import com.naury.chageun.core.domain.cloudbackup.CloudResult

/** 백업 저장소를 쓸 수 없을 때. Google 드라이브 연결을 붙이기 전까지 이 구현을 쓴다. */
internal object UnavailableCloudBackupRepository : CloudBackupRepository {
    private val unavailable = CloudResult.Failure(CloudBackupError.Unavailable)

    override suspend fun list() = unavailable

    override suspend fun backUpNow() = unavailable

    override suspend fun download(backupId: String) = unavailable

    override suspend fun delete(backupId: String) = unavailable
}
