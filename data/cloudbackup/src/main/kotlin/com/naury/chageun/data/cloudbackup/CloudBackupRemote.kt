package com.naury.chageun.data.cloudbackup

import com.naury.chageun.core.domain.cloudbackup.CloudBackup
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import java.io.File

/** 원격 저장소 요청이 실패했을 때 던진다. 구현이 플랫폼 예외를 미리 [error]로 바꿔 둔다. */
internal class CloudBackupException(val error: CloudBackupError, cause: Throwable? = null) : Exception(cause)

/**
 * 백업 파일을 두는 원격 저장소. 순서와 보관 개수 같은 규칙은 [DefaultCloudBackupRepository]에 두고
 * 여기서는 요청만 보낸다. 실패하면 [CloudBackupException]을 던진다.
 */
internal interface CloudBackupRemote {
    /** 파일과 요약을 함께 올리고, 저장소가 정한 백업 ID를 담아 돌려준다. */
    suspend fun upload(token: String, backup: CloudBackup, file: File): CloudBackup

    /** 최신순. */
    suspend fun list(token: String): List<CloudBackup>

    suspend fun download(token: String, backupId: String, target: File)

    /** 이미 없는 파일이면 성공으로 본다. */
    suspend fun delete(token: String, backupId: String)
}
