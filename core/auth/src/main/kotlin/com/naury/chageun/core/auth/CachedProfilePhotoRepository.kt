package com.naury.chageun.core.auth

import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.domain.auth.AuthRepository
import com.naury.chageun.core.domain.auth.ProfilePhotoRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 로그인한 계정의 사진 주소를 따라 사진 한 장을 받아 둔다. 같은 주소면 다시 받지 않아 오프라인에서도 보인다.
 * 홈과 계정 화면이 동시에 구독해도 한 번만 받도록 잠근다.
 */
@Singleton
internal class CachedProfilePhotoRepository @Inject constructor(
    authRepository: AuthRepository,
    private val cache: ProfilePhotoCache,
    private val downloader: ProfilePhotoDownloader,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ProfilePhotoRepository {

    private val lock = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val photoPath: Flow<String?> = authRepository.currentUser
        .map { it?.photoUrl }
        .distinctUntilChanged()
        .mapLatest(::sync)

    private suspend fun sync(url: String?): String? = withContext(ioDispatcher) {
        lock.withLock {
            val cached = url?.let(cache::cached)
            when {
                url == null -> null.also { cache.clear() }
                cached != null -> cached.path
                // 받지 못하면 지난 사진도 지운다. 바뀐 사진 대신 예전 사진이 계속 보이는 것보다 첫 글자가 낫다.
                else -> downloader.download(url)?.let { cache.store(url, it).path } ?: null.also { cache.clear() }
            }
        }
    }
}
