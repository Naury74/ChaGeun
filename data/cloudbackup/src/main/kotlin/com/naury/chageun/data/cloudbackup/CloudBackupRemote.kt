package com.naury.chageun.data.cloudbackup

import com.naury.chageun.core.domain.cloudbackup.CloudBackup
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import java.io.File
import java.time.Instant

/** 원격 저장소 요청이 실패했을 때 던진다. 구현이 플랫폼 예외를 미리 [error]로 바꿔 둔다. */
internal class CloudBackupException(val error: CloudBackupError, cause: Throwable? = null) : Exception(cause)

/**
 * 백업 파일(Storage)과 요약(Firestore)을 다루는 얇은 층. 순서와 정리 같은 규칙은
 * [DefaultCloudBackupRepository]에 두고 여기서는 요청만 보낸다. 실패하면 [CloudBackupException]을 던진다.
 */
internal interface CloudBackupRemote {
    suspend fun uploadArchive(uid: String, backupId: String, file: File)

    suspend fun writeMetadata(uid: String, backup: CloudBackup)

    /** 사용자 문서의 마지막 백업 시각을 갱신한다. 처음이면 만든 시각도 함께 쓴다. */
    suspend fun touchUser(uid: String, backedUpAt: Instant)

    /** 최신순. */
    suspend fun listMetadata(uid: String): List<CloudBackup>

    suspend fun downloadArchive(uid: String, backupId: String, target: File)

    /** 이미 없는 파일이면 성공으로 본다. */
    suspend fun deleteArchive(uid: String, backupId: String)

    suspend fun deleteMetadata(uid: String, backupId: String)
}
