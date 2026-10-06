package com.naury.chageun.data.cloudbackup

import android.net.Uri
import com.naury.chageun.core.auth.GoogleDriveAccess
import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
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
 * 백업 순서: 로컬 ZIP 만들기 → 드라이브에 요약과 함께 올리기 → 오래된 백업 정리.
 * 드라이브는 사용자 본인 것이라 운영자에게 비용이 생기지 않는다(ADR-006).
 */
internal class DefaultCloudBackupRepository @Inject constructor(
    private val drive: GoogleDriveAccess,
    private val local: BackupRepository,
    private val vehicles: VehicleRepository,
    private val remote: CloudBackupRemote,
    private val environment: CloudBackupEnvironment,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CloudBackupRepository {

    override suspend fun list(): CloudResult<List<CloudBackup>> = connected { token -> remote.list(token) }

    override suspend fun backUpNow(): CloudResult<CloudBackup> = connected { token ->
        val summary = local.summary()
        if (summary.vehicles == 0) throw CloudBackupException(CloudBackupError.NoData)
        val name = environment.newBackupId()
        val archive = File(environment.workDirectory, "$name.zip")
        try {
            archive.parentFile?.mkdirs()
            if (!local.export(Uri.fromFile(archive).toString())) throw CloudBackupException(CloudBackupError.Unknown)
            val size = withContext(ioDispatcher) { archive.length() }
            if (size >= MAX_ARCHIVE_BYTES) throw CloudBackupException(CloudBackupError.TooLarge)
            val backup = remote.upload(
                token,
                CloudBackup(
                    id = name,
                    createdAt = environment.clock.instant(),
                    sizeBytes = size,
                    schemaVersion = SCHEMA_VERSION,
                    appVersion = environment.appVersion,
                    deviceModel = environment.deviceModel,
                    vehicleLabel = vehicleLabel(),
                    recordCount = summary.records,
                    photoCount = summary.photos,
                ),
                archive,
            )
            quietly { prune(token) }
            backup
        } finally {
            withContext(ioDispatcher) { archive.delete() }
        }
    }

    override suspend fun download(backupId: String): CloudResult<String> = connected { token ->
        val target = File(environment.workDirectory, RESTORE_FILE)
        target.parentFile?.mkdirs()
        remote.download(token, backupId, target)
        Uri.fromFile(target).toString()
    }

    override suspend fun delete(backupId: String): CloudResult<Unit> = connected { token ->
        remote.delete(token, backupId)
    }

    private suspend fun prune(token: String) {
        remote.list(token).drop(CloudBackupRepository.MAX_BACKUPS).forEach { remote.delete(token, it.id) }
    }

    private suspend fun vehicleLabel(): String? = vehicles.observePrimaryVehicle().first()
        ?.let { "${it.maker} ${it.model}".trim() }
        ?.takeIf { it.isNotEmpty() }

    private suspend fun <T> connected(block: suspend (String) -> T): CloudResult<T> {
        val token = drive.accessToken() ?: return CloudResult.Failure(CloudBackupError.NotConnected)
        return try {
            CloudResult.Success(block(token))
        } catch (e: CloudBackupException) {
            // 토큰이 만료·취소됐으면 연결을 끊어 화면이 다시 연결하도록 안내하게 한다.
            if (e.error == CloudBackupError.NotConnected) drive.disconnect()
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
        /** 백업 파일 형식. 내보내기 형식(BackupDocument)이 바뀌면 함께 올린다. */
        const val SCHEMA_VERSION = 1

        /** 한 번에 올리는 백업의 한도. 드라이브 용량을 지나치게 쓰지 않게 한다. */
        const val MAX_ARCHIVE_BYTES = 200L * 1024 * 1024

        private const val RESTORE_FILE = "restore.zip"
    }
}
