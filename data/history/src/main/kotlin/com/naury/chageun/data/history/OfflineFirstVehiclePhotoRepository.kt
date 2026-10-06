package com.naury.chageun.data.history

import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.common.storage.AttachmentDirectory
import com.naury.chageun.core.database.dao.AttachmentDao
import com.naury.chageun.core.database.entity.AttachmentEntity
import com.naury.chageun.core.domain.settings.SettingsRepository
import com.naury.chageun.core.domain.vehicle.VehiclePhotoRepository
import com.naury.chageun.core.model.CutoutFailure
import com.naury.chageun.core.model.CutoutStatus
import com.naury.chageun.core.model.VehicleId
import java.io.File
import java.time.Clock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 대표 사진을 attachment 테이블에 owner_type [OWNER_TYPE]으로 저장한다.
 * 가져오기·삭제·백업 경로를 기록 첨부와 공유하고, Timeline 조회는 이 owner_type을 무시한다.
 *
 * 차만 잘라 낸 투명 PNG를 원본 옆에 만든다. 원본은 항상 JPEG이므로, 관찰하는 쪽은 확장자가 PNG면
 * 배경을 잘라 낸 사진으로 본다. 잘라 낸 파일은 기기에서 다시 만들 수 있어 백업하지 않는다.
 *
 * 잘라 내기는 모델을 처음 내려받느라 오래 걸릴 수 있어 원본을 먼저 저장해 보여 주고, 앱 수명 동안
 * 이어지는 [scope]에서 진행한다. 진행 상태는 [observeCutout]으로 화면에 알린다.
 */
@Singleton
internal class OfflineFirstVehiclePhotoRepository @Inject constructor(
    private val attachmentDao: AttachmentDao,
    private val images: VehiclePhotoImages,
    @param:AttachmentDirectory private val directory: File,
    private val clock: Clock,
    private val logger: AppLogger,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : VehiclePhotoRepository {

    // 화면을 떠나도 잘라 내기가 끊기지 않도록 Repository와 같은 수명으로 둔다.
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val cutoutJobs = ConcurrentHashMap<VehicleId, Job>()
    private val cutoutStatus = MutableStateFlow<Map<VehicleId, CutoutStatus>>(emptyMap())

    // 상태가 바뀔 때도 다시 계산해서, 잘라 낸 파일이 생기면 바로 PNG 경로로 바뀐다.
    override fun observe(vehicleId: VehicleId): Flow<String?> = combine(
        attachmentDao.observe(vehicleId.value, OWNER_TYPE, vehicleId.value),
        cutoutStatus.map { it[vehicleId] },
        observeBackgroundRemoval(vehicleId),
    ) { rows, _, removeBackground ->
        rows.maxByOrNull { it.createdAt }?.let { row ->
            val cutout = row.cutoutFile()
            (if (removeBackground && cutout.exists()) cutout else File(directory, row.fileName)).absolutePath
        }
    }
        .distinctUntilChanged()
        .flowOn(ioDispatcher)

    // 배경 지우기를 꺼 두었으면 지난 실패 안내도 보이지 않게 한다.
    override fun observeCutout(vehicleId: VehicleId): Flow<CutoutStatus> =
        combine(cutoutStatus.map { it[vehicleId] ?: CutoutStatus.Idle }, observeBackgroundRemoval(vehicleId)) {
                status,
                enabled,
            ->
            if (enabled) status else CutoutStatus.Idle
        }.distinctUntilChanged()

    // 지금은 차량이 하나라 앱 설정 하나로 둔다. 여러 대를 다루게 되면 차량별로 나눈다.
    override fun observeBackgroundRemoval(vehicleId: VehicleId): Flow<Boolean> =
        images.settings.settings.map { it.isVehiclePhotoCutoutEnabled }.distinctUntilChanged()

    override suspend fun setBackgroundRemoval(vehicleId: VehicleId, enabled: Boolean) {
        images.settings.setVehiclePhotoCutoutEnabled(enabled)
        if (enabled) removeBackground(vehicleId) else cutoutJobs.remove(vehicleId)?.cancel()
    }

    override suspend fun replace(vehicleId: VehicleId, sourceUri: String, removeBackground: Boolean): Boolean =
        withContext(ioDispatcher) {
            val id = UUID.randomUUID().toString()
            val imported = runCatching {
                images.importer.import(
                    sourceUri,
                    File(directory, "$id.jpg"),
                    File(directory, "${id}_thumb.jpg"),
                    ImageImporter.DEFAULT_EDGE_PX,
                )
            }.onFailure { logger.warn("vehicle_photo_import_failed", error = it) }.getOrNull()
                ?: return@withContext false
            cutoutJobs.remove(vehicleId)?.cancel()
            val previous = attachmentDao.findFor(vehicleId.value, OWNER_TYPE, vehicleId.value)
            attachmentDao.insert(
                AttachmentEntity(
                    id = id,
                    vehicleId = vehicleId.value,
                    ownerType = OWNER_TYPE,
                    ownerId = vehicleId.value,
                    fileName = imported.file.name,
                    thumbnailName = imported.thumbnail.name,
                    mimeType = MIME_JPEG,
                    sizeBytes = imported.sizeBytes,
                    createdAt = clock.instant(),
                ),
            )
            previous.forEach { row ->
                attachmentDao.delete(vehicleId.value, row.id)
                row.deleteFiles()
            }
            // 사진을 넣을 때 고른 값을 앞으로의 보기 방식으로도 쓴다.
            images.settings.setVehiclePhotoCutoutEnabled(removeBackground)
            if (removeBackground) removeBackground(vehicleId)
            true
        }

    override fun replaceInBackground(vehicleId: VehicleId, sourceUri: String, removeBackground: Boolean) {
        scope.launch {
            // 앱 전체 수명의 scope라 예외가 새면 앱이 끝난다. 실패는 기록만 하고 사진 없이 둔다.
            runCatching { replace(vehicleId, sourceUri, removeBackground) }
                .onFailure { logger.warn("vehicle_photo_background_import_failed", error = it) }
        }
    }

    override fun removeBackground(vehicleId: VehicleId) {
        cutoutJobs.remove(vehicleId)?.cancel()
        cutoutJobs[vehicleId] = scope.launch {
            val row = attachmentDao.findFor(vehicleId.value, OWNER_TYPE, vehicleId.value).maxByOrNull { it.createdAt }
            val original = row?.let { File(directory, it.fileName) }
            if (row == null || original?.exists() != true || row.cutoutFile().exists()) return@launch
            updateCutout(vehicleId, CutoutStatus.Processing)
            // 처리 도중 프로세스가 죽어도 깨진 PNG가 남지 않도록 임시 파일에 쓰고 끝나면 이름을 바꾼다.
            val partial = File(directory, "${row.id}$PARTIAL_SUFFIX")
            val result = cutout(original, partial) { updateCutout(vehicleId, it) }
            if (result == CutoutResult.Success && !partial.renameTo(row.cutoutFile())) partial.delete()
            if (result != CutoutResult.Success) {
                partial.delete()
                logger.warn("vehicle_photo_cutout_failed", LogField.ErrorType(result.name))
            }
            updateCutout(vehicleId, result.toStatus())
        }
    }

    @Suppress("TooGenericExceptionCaught") // 잘라 내기 실패가 앱 전체를 끝내지 않도록 모두 실패 결과로 바꾼다.
    private suspend fun cutout(source: File, target: File, onStatus: (CutoutStatus) -> Unit): CutoutResult = try {
        images.cutter.cutout(source, target, onStatus)
    } catch (cancelled: CancellationException) {
        target.delete()
        throw cancelled
    } catch (error: Exception) {
        logger.warn("vehicle_photo_cutout_failed", error = error)
        CutoutResult.Failed
    }

    private fun updateCutout(vehicleId: VehicleId, status: CutoutStatus) {
        cutoutStatus.update { it + (vehicleId to status) }
    }

    private fun CutoutResult.toStatus(): CutoutStatus = when (this) {
        CutoutResult.Success -> CutoutStatus.Idle
        CutoutResult.NoSubject -> CutoutStatus.Failed(CutoutFailure.NoSubject)
        CutoutResult.ModelUnavailable -> CutoutStatus.Failed(CutoutFailure.ModelUnavailable)
        CutoutResult.Failed -> CutoutStatus.Failed(CutoutFailure.Error)
    }

    override suspend fun clear(vehicleId: VehicleId) = withContext(ioDispatcher) {
        cutoutJobs.remove(vehicleId)?.cancel()
        updateCutout(vehicleId, CutoutStatus.Idle)
        val rows = attachmentDao.findFor(vehicleId.value, OWNER_TYPE, vehicleId.value)
        attachmentDao.deleteFor(vehicleId.value, OWNER_TYPE, vehicleId.value)
        rows.forEach { it.deleteFiles() }
    }

    private fun AttachmentEntity.deleteFiles() {
        File(directory, fileName).delete()
        File(directory, thumbnailName).delete()
        cutoutFile().delete()
    }

    private fun AttachmentEntity.cutoutFile() = File(directory, "${fileName.substringBeforeLast('.')}$CUTOUT_SUFFIX")

    private companion object {
        const val OWNER_TYPE = "VehiclePhoto"
        const val CUTOUT_SUFFIX = "_cutout.png"
        const val PARTIAL_SUFFIX = "_cutout.partial"
    }
}

/** 차량 사진을 가져와 배경을 잘라 내는 두 단계와, 잘라 낸 모습으로 볼지에 대한 사용자 설정을 묶는다. */
internal class VehiclePhotoImages @Inject constructor(
    val importer: ImageImporter,
    val cutter: SubjectCutter,
    val settings: SettingsRepository,
)
