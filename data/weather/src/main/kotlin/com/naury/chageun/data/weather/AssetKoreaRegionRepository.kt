package com.naury.chageun.data.weather

import android.content.Context
import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.domain.weather.KoreaRegionCatalog
import com.naury.chageun.core.domain.weather.KoreaRegionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 앱에 넣은 지역 목록(약 3,800곳, 150KB)을 처음 필요할 때 한 번만 읽어 메모리에 둔다.
 * 지역을 직접 고르지 않은 사용자는 지역 고르기 창을 열기 전까지 읽지 않는다.
 */
@Singleton
internal class AssetKoreaRegionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : KoreaRegionRepository {

    private val mutex = Mutex()

    @Volatile
    private var cached: KoreaRegionCatalog? = null

    override suspend fun catalog(): KoreaRegionCatalog = cached ?: mutex.withLock {
        cached ?: withContext(ioDispatcher) {
            val rows = context.assets.open(KoreaRegionCsv.ASSET_PATH).bufferedReader().useLines(KoreaRegionCsv::parse)
            KoreaRegionCatalog(rows)
        }.also { cached = it }
    }
}
