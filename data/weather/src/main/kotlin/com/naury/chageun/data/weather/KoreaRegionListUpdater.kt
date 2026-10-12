package com.naury.chageun.data.weather

import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.domain.weather.KoreaRegionCheck
import com.naury.chageun.core.domain.weather.KoreaRegionUpdates
import com.naury.chageun.core.domain.weather.WeatherSettingsRepository
import java.io.IOException
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 웹에 올린 안내 파일을 보고, 가진 것보다 새 목록이 있으면 받아서 검사한 뒤 바꿔 쓴다.
 *
 * 1. 안내 파일(regions.json)을 받는다. schema가 1이 아니면(더 새 형식) 앱을 새로 받기 전까지 쓰지 않는다.
 * 2. 판이 지금 쓰는 목록보다 새것일 때만 목록 파일을 받는다.
 * 3. sha256, 줄 수, 좌표 범위, 코드 중복, 윗 지역을 모두 맞춰 본 뒤에야 앱 안에 저장한다.
 * 4. 메모리의 목록을 새로 읽고, 고른 지역과 최근 지역의 코드를 바뀐 코드 표에 따라 고친다.
 *
 * 어디서든 실패하면 가진 목록을 그대로 쓰고 [KoreaRegionCheck.Failed]를 돌려준다. 로그에는 실패 종류만 남긴다.
 */
@Singleton
internal class KoreaRegionListUpdater @Inject constructor(
    private val remote: KoreaRegionRemote,
    private val repository: AssetKoreaRegionRepository,
    private val weatherSettings: WeatherSettingsRepository,
    private val clock: Clock,
    private val logger: AppLogger,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : KoreaRegionUpdates {

    private val mutex = Mutex()
    private val store: KoreaRegionListStore get() = repository.store

    override suspend fun checkIfStale() {
        val now = clock.instant()
        val last = withContext(ioDispatcher) { store.lastCheckedAt() }
        // 기기 시계를 앞으로 돌렸다가 되돌린 경우처럼 마지막 확인이 미래면 오래된 것으로 본다.
        val isFresh = last != null && !last.isAfter(now) && Duration.between(last, now) < STALE_AFTER
        if (!isFresh) checkNow()
    }

    override suspend fun checkNow(): KoreaRegionCheck = mutex.withLock {
        withContext(ioDispatcher) {
            try {
                check()
            } catch (_: IOException) {
                logger.warn(EVENT_FAILED, LogField.ErrorType("network"))
                KoreaRegionCheck.Failed(isTemporary = true)
            } catch (_: IllegalArgumentException) {
                logger.warn(EVENT_FAILED, LogField.ErrorType("invalid"))
                KoreaRegionCheck.Failed(isTemporary = false)
            }
        }
    }

    private suspend fun check(): KoreaRegionCheck {
        // 지난번 설치 뒤 코드 고치기를 마치기 전에 앱이 꺼졌을 수 있다. 같은 판이면 아무것도 하지 않는다.
        applyCodeChanges()
        store.markChecked(clock.instant())
        val manifest = KoreaRegionManifest.parse(remote.manifest())
        if (manifest == null || !manifest.isNewerThan(repository.activeInfo().version)) {
            return KoreaRegionCheck.UpToDate
        }
        val bytes = remote.file(manifest.file)
        KoreaRegionListCheck.verify(bytes, manifest)
        store.install(manifest, bytes)
        repository.reload()
        applyCodeChanges()
        logger.debug(EVENT_UPDATED, LogField.Success(true))
        return KoreaRegionCheck.Updated(manifest.version)
    }

    private suspend fun applyCodeChanges() {
        val installed = store.installed()?.manifest ?: return
        if (repository.activeInfo().version != installed.version) return
        weatherSettings.applyRegionListChange(installed.version, installed.renamedCodes, repository.catalog())
    }

    private companion object {
        val STALE_AFTER: Duration = Duration.ofDays(7)
        const val EVENT_FAILED = "region_list_check_failed"
        const val EVENT_UPDATED = "region_list_updated"
    }
}
