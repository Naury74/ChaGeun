package com.naury.chageun.data.weather

import android.content.Context
import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** 웹에 올린 지역 목록 안내 파일과 목록 파일을 받는다. 실패하면 [IOException]을 던진다. */
internal interface KoreaRegionRemote {
    suspend fun manifest(): String

    /** [name]은 안내 파일이 말한 파일 이름이다. 경로가 섞이지 않았는지는 [KoreaRegionManifest]가 이미 봤다. */
    suspend fun file(name: String): ByteArray
}

/**
 * naurylab.com(GitHub Pages)에서 받는다. 주소는 고정이고 쿼리도 붙이지 않아, 요청에는 고른 지역이나
 * 기기를 알아볼 값이 없다. 호스팅 쪽에는 다른 웹 요청처럼 IP 주소와 User-Agent(앱 버전)만 보인다.
 */
internal class HttpKoreaRegionRemote @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : KoreaRegionRemote {

    private val userAgent: String by lazy { chageunUserAgent(context) }

    override suspend fun manifest(): String = get(MANIFEST_URL, MAX_MANIFEST_BYTES).toString(Charsets.UTF_8)

    override suspend fun file(name: String): ByteArray = get(BASE_URL + name, MAX_FILE_BYTES)

    private suspend fun get(url: String, maxBytes: Int): ByteArray = withContext(ioDispatcher) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            useCaches = false
            setRequestProperty("User-Agent", userAgent)
        }
        try {
            val code = connection.responseCode
            if (code !in HTTP_OK_RANGE) throw IOException("region_list_http_$code")
            if (connection.contentLengthLong > maxBytes) throw IOException("region_list_too_large")
            connection.inputStream.use { input -> readAtMost(input, maxBytes) }
        } finally {
            connection.disconnect()
        }
    }

    /** 안내와 달리 끝없이 오는 응답에 메모리를 다 쓰지 않도록 [maxBytes]에서 끊는다. */
    private fun readAtMost(input: InputStream, maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            if (out.size() > maxBytes) throw IOException("region_list_too_large")
        }
        return out.toByteArray()
    }

    private companion object {
        const val BASE_URL = "https://naurylab.com/chageun/data/regions/"
        const val MANIFEST_URL = BASE_URL + "regions.json"
        const val TIMEOUT_MILLIS = 10_000
        const val MAX_MANIFEST_BYTES = 256 * 1024
        const val MAX_FILE_BYTES = 2 * 1024 * 1024
        const val BUFFER_BYTES = 8 * 1024
        val HTTP_OK_RANGE = 200..299
    }
}
