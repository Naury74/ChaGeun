package com.naury.chageun.data.cloudbackup

import android.net.Uri
import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.domain.auth.AuthRepository
import com.naury.chageun.core.domain.backup.BackupRepository
import com.naury.chageun.core.domain.cloudbackup.CloudBackup
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import com.naury.chageun.core.domain.cloudbackup.CloudBackupRepository
import com.naury.chageun.core.domain.cloudbackup.CloudResult
import com.naury.chageun.core.domain.vehicle.VehicleRepository
import java.io.File
import java.time.Clock
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** 백업 요약에 남기는 이 기기 정보와 작업 폴더. 테스트에서 바꿔 끼울 수 있게 묶었다. */
internal data class CloudBackupEnvironment(
    val workDirectory: File,
    val appVersion: String,
    val deviceModel: String?,
    val clock: Clock,
    val newBackupId: () -> String,
)

/**
 * 백업 순서: 로컬 ZIP 만들기 → Storage 업로드 → Firestore 요약 기록 → 오래된 백업 정리.
 * 요약은 파일이 올라간 뒤에만 쓰므로 목록에 보이는 백업은 항상 내려받을 수 있다.
 */
internal class DefaultCloudBackupRepository @Inject constructor(
    private val auth: AuthRepository,
    private val local: BackupRepository,
    private val vehicles: VehicleRepository,
    private val remote: CloudBackupRemote,
    private val environment: CloudBackupEnvironment,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CloudBackupRepository {

    override suspend fun list(): CloudResult<List<CloudBackup>> = signedIn { uid -> remote.listMetadata(uid) }

    override suspend fun backUpNow(): CloudResult<CloudBackup> = signedIn(requireVerified = true) { uid ->
        val summary = local.summary()
        if (summary.vehicles == 0) throw CloudBackupException(CloudBackupError.NoData)
        val id = environment.newBackupId()
        val archive = File(environment.workDirectory, "$id.zip")
        try {
            archive.parentFile?.mkdirs()
            if (!local.export(Uri.fromFile(archive).toString())) throw CloudBackupException(CloudBackupError.Unknown)
            val size = withContext(ioDispatcher) { archive.length() }
            if (size >= MAX_ARCHIVE_BYTES) throw CloudBackupException(CloudBackupError.TooLarge)

            remote.uploadArchive(uid, id, archive)
            val backup = CloudBackup(
                id = id,
                createdAt = environment.clock.instant(),
                sizeBytes = size,
                schemaVersion = SCHEMA_VERSION,
                appVersion = environment.appVersion.take(MAX_LABEL_LENGTH_APP),
                deviceModel = environment.deviceModel?.take(MAX_LABEL_LENGTH),
                vehicleLabel = vehicleLabel(),
                recordCount = summary.records,
                photoCount = summary.photos,
            )
            try {
                remote.writeMetadata(uid, backup)
            } catch (e: CloudBackupException) {
                // 요약 없이 남은 파일은 목록에 보이지 않으니 지운다.
                quietly { remote.deleteArchive(uid, id) }
                throw e
            }
            quietly { remote.touchUser(uid, backup.createdAt) }
            quietly { prune(uid) }
            backup
        } finally {
            withContext(ioDispatcher) { archive.delete() }
        }
    }

    override suspend fun download(backupId: String): CloudResult<String> = signedIn { uid ->
        val target = File(environment.workDirectory, RESTORE_FILE)
        target.parentFile?.mkdirs()
        remote.downloadArchive(uid, backupId, target)
        Uri.fromFile(target).toString()
    }

    override suspend fun delete(backupId: String): CloudResult<Unit> = signedIn { uid ->
        remote.deleteArchive(uid, backupId)
        remote.deleteMetadata(uid, backupId)
    }

    private suspend fun prune(uid: String) {
        remote.listMetadata(uid).drop(CloudBackupRepository.MAX_BACKUPS).forEach {
            remote.deleteArchive(uid, it.id)
            remote.deleteMetadata(uid, it.id)
        }
    }

    private suspend fun vehicleLabel(): String? = vehicles.observePrimaryVehicle().first()
        ?.let { "${it.maker} ${it.model}".trim().take(MAX_LABEL_LENGTH) }
        ?.takeIf { it.isNotEmpty() }

    private suspend fun <T> signedIn(requireVerified: Boolean = false, block: suspend (String) -> T): CloudResult<T> {
        val user = auth.currentUser.first() ?: return CloudResult.Failure(CloudBackupError.NotSignedIn)
        if (requireVerified && user.needsEmailVerification) {
            return CloudResult.Failure(CloudBackupError.EmailNotVerified)
        }
        return try {
            CloudResult.Success(block(user.uid))
        } catch (e: CloudBackupException) {
            CloudResult.Failure(e.error)
        }
    }

    /** 정리 같은 뒷일은 실패해도 백업 자체는 성공이다. 다음 백업 때 다시 정리된다. */
    private suspend fun quietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: CloudBackupException) {
            Unit
        }
    }

    companion object {
        /** 백업 파일 형식. 규칙의 schemaVersion과 같고, 내보내기 형식(BackupDocument)이 바뀌면 함께 올린다. */
        const val SCHEMA_VERSION = 1

        /** Storage 규칙의 한도와 같다. */
        const val MAX_ARCHIVE_BYTES = 200L * 1024 * 1024

        private const val MAX_LABEL_LENGTH = 64
        private const val MAX_LABEL_LENGTH_APP = 32
        private const val RESTORE_FILE = "restore.zip"
    }
}
