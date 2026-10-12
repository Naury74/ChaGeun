package com.naury.chageun.data.weather

import android.content.Context
import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.domain.weather.KoreaRegionCatalog
import com.naury.chageun.core.domain.weather.KoreaRegionListInfo
import com.naury.chageun.core.domain.weather.KoreaRegionRepository
import com.naury.chageun.core.domain.weather.KoreaRegionRow
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 지역 목록(약 3,800곳, 150KB)을 처음 필요할 때 한 번만 읽어 메모리에 둔다.
 * 지역을 직접 고르지 않은 사용자는 지역 고르기 창을 열기 전까지 읽지 않는다.
 *
 * 웹에서 받아 둔 목록이 있고 앱에 넣은 목록([KoreaRegionCsv.BUNDLED_VERSION])보다 새것이면 그것을 쓴다.
 * 받아 둔 목록이 상했으면 지우고 앱에 넣은 목록으로 돌아간다.
 */
@Singleton
internal class AssetKoreaRegionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    /** 받아 둔 목록. [KoreaRegionListUpdater]가 같은 곳에 새 목록을 넣는다. */
    val store: KoreaRegionListStore,
) : KoreaRegionRepository {

    private val mutex = Mutex()
    private val generation = MutableStateFlow(0)

    @Volatile
    private var cached: KoreaRegionCatalog? = null

    @Volatile
    private var cachedInfo: KoreaRegionListInfo? = null

    override val listInfo: Flow<KoreaRegionListInfo> = generation.map { activeInfo() }

    override suspend fun catalog(): KoreaRegionCatalog = cached ?: mutex.withLock {
        cached ?: withContext(ioDispatcher) { KoreaRegionCatalog(downloadedRows() ?: bundledRows()) }
            .also { cached = it }
    }

    /** 지금 쓰는 목록의 판과 출처. 목록 파일은 sha256만 맞춰 보고 줄은 읽지 않는다. */
    suspend fun activeInfo(): KoreaRegionListInfo = cachedInfo ?: withContext(ioDispatcher) {
        downloaded()?.manifest?.let { KoreaRegionListInfo(it.version, it.source) } ?: BUNDLED_INFO
    }.also { cachedInfo = it }

    /** 새 목록을 설치한 뒤 부른다. 다음에 [catalog]를 부를 때 새로 읽고, [listInfo]가 새 판을 낸다. */
    fun reload() {
        cached = null
        cachedInfo = null
        generation.update { it + 1 }
    }

    private fun downloaded(): KoreaRegionListStore.Installed? =
        store.installed()?.takeIf { it.manifest.isNewerThan(KoreaRegionCsv.BUNDLED_VERSION) }

    private fun downloadedRows(): List<KoreaRegionRow>? {
        val installed = downloaded() ?: return null
        return try {
            KoreaRegionListCheck.parse(installed.bytes).also(KoreaRegionListCheck::validate)
        } catch (_: IllegalArgumentException) {
            // 설치할 때 검사했으니 여기서 틀리면 파일이 상한 것이다. 다음 확인 때 다시 받는다.
            store.clear()
            cachedInfo = null
            generation.update { it + 1 }
            null
        }
    }

    private fun bundledRows(): List<KoreaRegionRow> =
        context.assets.open(KoreaRegionCsv.ASSET_PATH).bufferedReader().useLines(KoreaRegionCsv::parse)

    private companion object {
        val BUNDLED_INFO = KoreaRegionListInfo(version = KoreaRegionCsv.BUNDLED_VERSION)
    }
}
