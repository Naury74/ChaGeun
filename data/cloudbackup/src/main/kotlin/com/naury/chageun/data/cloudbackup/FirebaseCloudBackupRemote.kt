package com.naury.chageun.data.cloudbackup

import android.net.Uri
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storageMetadata
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.domain.cloudbackup.CloudBackup
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import java.io.File
import java.time.Instant
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Storage `users/{uid}/backups/{id}.zip`과 Firestore `users/{uid}/backups/{id}`. 필드와 경로는 firebase/ 규칙과 같다.
 *
 * Firestore 쓰기는 오프라인이면 끝나지 않고 대기열에 남으므로 시간 제한을 둔다. 목록은 캐시가 아닌 서버에서 읽는다.
 */
internal class FirebaseCloudBackupRemote(
    firestore: () -> FirebaseFirestore,
    storage: () -> FirebaseStorage,
    private val logger: AppLogger,
) : CloudBackupRemote {

    // 콘솔에서 Firestore·Storage를 만들기 전에도 앱이 시작되도록 처음 쓸 때 가져온다.
    private val firestore by lazy(firestore)
    private val storage by lazy {
        storage().apply {
            maxUploadRetryTimeMillis = TRANSFER_RETRY_MILLIS
            maxDownloadRetryTimeMillis = TRANSFER_RETRY_MILLIS
            maxOperationRetryTimeMillis = OPERATION_RETRY_MILLIS
        }
    }

    override suspend fun uploadArchive(uid: String, backupId: String, file: File) = guard("upload") {
        archive(uid, backupId).putFile(Uri.fromFile(file), storageMetadata { contentType = ZIP_MIME_TYPE }).await()
        Unit
    }

    override suspend fun writeMetadata(uid: String, backup: CloudBackup) = guard("write_metadata") {
        val fields = buildMap {
            put(FIELD_CREATED_AT, backup.createdAt.toTimestamp())
            put(FIELD_SIZE, backup.sizeBytes)
            put(FIELD_SCHEMA, backup.schemaVersion.toLong())
            put(FIELD_APP_VERSION, backup.appVersion)
            backup.deviceModel?.let { put(FIELD_DEVICE, it) }
            backup.vehicleLabel?.let { put(FIELD_VEHICLE, it) }
            put(FIELD_RECORDS, backup.recordCount.toLong())
            put(FIELD_PHOTOS, backup.photoCount.toLong())
        }
        withTimeout(WRITE_TIMEOUT_MILLIS) { backups(uid).document(backup.id).set(fields).await() }
        Unit
    }

    override suspend fun touchUser(uid: String, backedUpAt: Instant) = guard("touch_user") {
        val user = firestore.collection(USERS).document(uid)
        val at = backedUpAt.toTimestamp()
        withTimeout(WRITE_TIMEOUT_MILLIS) {
            val exists = user.get(Source.SERVER).await().exists()
            val fields = if (exists) {
                mapOf(FIELD_LAST_BACKUP to at)
            } else {
                mapOf(
                    FIELD_CREATED_AT to at,
                    FIELD_LAST_BACKUP to at,
                )
            }
            user.set(fields, SetOptions.merge()).await()
        }
        Unit
    }

    override suspend fun listMetadata(uid: String): List<CloudBackup> = guard("list") {
        backups(uid).orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .get(Source.SERVER)
            .await()
            .documents
            .mapNotNull { it.toCloudBackup() }
    }

    override suspend fun downloadArchive(uid: String, backupId: String, target: File) = guard("download") {
        archive(uid, backupId).getFile(target).await()
        Unit
    }

    override suspend fun deleteArchive(uid: String, backupId: String) = guard("delete_archive") {
        try {
            archive(uid, backupId).delete().await()
        } catch (e: StorageException) {
            if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw e
        }
        Unit
    }

    override suspend fun deleteMetadata(uid: String, backupId: String) = guard("delete_metadata") {
        withTimeout(WRITE_TIMEOUT_MILLIS) { backups(uid).document(backupId).delete().await() }
        Unit
    }

    private fun backups(uid: String) = firestore.collection(USERS).document(uid).collection(BACKUPS)

    private fun archive(uid: String, backupId: String) = storage.reference.child("$USERS/$uid/$BACKUPS/$backupId.zip")

    @Suppress("TooGenericExceptionCaught") // Firebase Task는 여러 예외를 던지고 모두 CloudBackupError로 바꾼다.
    private suspend fun <T> guard(operation: String, block: suspend () -> T): T = try {
        block()
    } catch (e: TimeoutCancellationException) {
        throw CloudBackupException(CloudBackupError.Network, e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val error = e.toCloudBackupError()
        if (error == CloudBackupError.Unknown) logger.warn("cloud_backup_${operation}_failed", error = e)
        throw CloudBackupException(error, e)
    }

    private companion object {
        const val USERS = "users"
        const val BACKUPS = "backups"
        const val ZIP_MIME_TYPE = "application/zip"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_LAST_BACKUP = "lastBackupAt"
        const val FIELD_SIZE = "sizeBytes"
        const val FIELD_SCHEMA = "schemaVersion"
        const val FIELD_APP_VERSION = "appVersion"
        const val FIELD_DEVICE = "deviceModel"
        const val FIELD_VEHICLE = "vehicleLabel"
        const val FIELD_RECORDS = "recordCount"
        const val FIELD_PHOTOS = "photoCount"
        const val WRITE_TIMEOUT_MILLIS = 30_000L
        const val TRANSFER_RETRY_MILLIS = 60_000L
        const val OPERATION_RETRY_MILLIS = 30_000L

        fun Instant.toTimestamp() = Timestamp(Date.from(this))

        /** 필드가 빠지거나 형식이 다른 문서는 목록에서 뺀다. 규칙이 막지만 손으로 고친 문서가 있을 수 있다. */
        fun DocumentSnapshot.toCloudBackup(): CloudBackup? {
            val createdAt = getTimestamp(FIELD_CREATED_AT) ?: return null
            return CloudBackup(
                id = id,
                createdAt = createdAt.toDate().toInstant(),
                sizeBytes = getLong(FIELD_SIZE) ?: return null,
                schemaVersion = getLong(FIELD_SCHEMA)?.toInt() ?: return null,
                appVersion = getString(FIELD_APP_VERSION).orEmpty(),
                deviceModel = getString(FIELD_DEVICE),
                vehicleLabel = getString(FIELD_VEHICLE),
                recordCount = getLong(FIELD_RECORDS)?.toInt() ?: 0,
                photoCount = getLong(FIELD_PHOTOS)?.toInt() ?: 0,
            )
        }
    }
}

/** 네트워크 문제와 콘솔 설정 문제를 나눠 안내한다. */
internal fun Throwable.toCloudBackupError(): CloudBackupError = when (this) {
    is FirebaseNetworkException -> CloudBackupError.Network
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        -> CloudBackupError.Network
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED,
        FirebaseFirestoreException.Code.NOT_FOUND,
        FirebaseFirestoreException.Code.FAILED_PRECONDITION,
        -> CloudBackupError.Unavailable
        else -> CloudBackupError.Unknown
    }
    is StorageException -> when (errorCode) {
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> CloudBackupError.Network
        StorageException.ERROR_NOT_AUTHORIZED,
        StorageException.ERROR_NOT_AUTHENTICATED,
        StorageException.ERROR_BUCKET_NOT_FOUND,
        StorageException.ERROR_PROJECT_NOT_FOUND,
        StorageException.ERROR_QUOTA_EXCEEDED,
        -> CloudBackupError.Unavailable
        else -> if (cause is java.io.IOException) CloudBackupError.Network else CloudBackupError.Unknown
    }
    else -> CloudBackupError.Unknown
}
