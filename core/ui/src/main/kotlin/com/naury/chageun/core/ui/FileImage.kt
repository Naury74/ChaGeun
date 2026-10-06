package com.naury.chageun.core.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 앱 전용 이미지 파일을 읽은 상태. 읽는 중과 읽을 수 없음을 나눠야 자리 표시를 알맞게 고를 수 있다. */
sealed interface FileImageState {
    data object Loading : FileImageState
    data object Missing : FileImageState
    data class Loaded(val image: ImageBitmap) : FileImageState
}

/**
 * 앱 전용 이미지 파일을 메인 스레드 밖에서 디코딩한다. 한 번 읽은 이미지는 메모리에 두어
 * 탭을 오갈 때 다시 읽는 동안 자리 표시가 깜빡이지 않게 한다.
 * 첨부·사진은 바꿀 때 새 이름으로 저장하므로 경로만으로 캐시해도 지난 그림이 나오지 않는다.
 */
@Composable
fun rememberFileImageState(path: String): State<FileImageState> = produceState(FileImageCache.stateOf(path), path) {
    val cached = FileImageCache.stateOf(path)
    value = cached
    if (cached is FileImageState.Loaded) return@produceState
    val image = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    value = if (image == null) {
        FileImageState.Missing
    } else {
        FileImageCache.put(path, image)
        FileImageState.Loaded(image)
    }
}

/** 불러오는 중이거나 파일이 없으면 null이다. */
@Composable
fun rememberFileImage(path: String): State<ImageBitmap?> {
    val state = rememberFileImageState(path)
    return remember(state) { derivedStateOf { (state.value as? FileImageState.Loaded)?.image } }
}

private object FileImageCache {
    // 앱이 쓸 수 있는 메모리의 1/8까지만 둔다. 대표 사진 몇 장과 썸네일 한두 화면이면 충분하다.
    private val cache = object : LruCache<String, ImageBitmap>(
        (Runtime.getRuntime().maxMemory() / CACHE_FRACTION).toInt(),
    ) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * BYTES_PER_PIXEL
    }

    fun stateOf(path: String): FileImageState = cache.get(path)?.let(FileImageState::Loaded) ?: FileImageState.Loading

    fun put(path: String, image: ImageBitmap) {
        cache.put(path, image)
    }
}

private const val CACHE_FRACTION = 8
private const val BYTES_PER_PIXEL = 4
