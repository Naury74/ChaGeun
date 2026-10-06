package com.naury.chageun.core.domain.cloudbackup

import java.time.Instant

/** 클라우드에 올라간 백업 하나의 요약. 목록에 보여 주고 복원할 백업을 고를 때 쓴다. */
data class CloudBackup(
    val id: String,
    val createdAt: Instant,
    val sizeBytes: Long,
    val schemaVersion: Int,
    val appVersion: String,
    val deviceModel: String?,
    val vehicleLabel: String?,
    val recordCount: Int,
    val photoCount: Int,
)

/** 화면에서 따로 안내하는 클라우드 백업 실패. */
enum class CloudBackupError {
    NotSignedIn,
    EmailNotVerified,

    /** 백업할 차량이 없다. */
    NoData,
    Network,

    /** 백업 파일이 규칙의 크기 한도(200MB)를 넘는다. */
    TooLarge,

    /** 콘솔에서 Firestore·Storage를 아직 만들지 않았거나 규칙이 막는다. */
    Unavailable,
    Unknown,
}

sealed interface CloudResult<out T> {
    data class Success<T>(val value: T) : CloudResult<T>

    data class Failure(val error: CloudBackupError) : CloudResult<Nothing>
}

/**
 * 로그인한 사용자의 클라우드 백업. 백업 파일은 기존 내보내기 ZIP과 같은 형식이라
 * 복원은 [download]로 받은 파일을 BackupRepository의 가져오기에 넘기면 된다.
 */
interface CloudBackupRepository {
    /** 최신순. */
    suspend fun list(): CloudResult<List<CloudBackup>>

    /** 성공하면 오래된 백업을 정리해 최근 [MAX_BACKUPS]개만 남긴다. */
    suspend fun backUpNow(): CloudResult<CloudBackup>

    /** 기기 임시 폴더에 받아 그 파일의 URI를 돌려준다. */
    suspend fun download(backupId: String): CloudResult<String>

    suspend fun delete(backupId: String): CloudResult<Unit>

    companion object {
        const val MAX_BACKUPS = 5
    }
}
