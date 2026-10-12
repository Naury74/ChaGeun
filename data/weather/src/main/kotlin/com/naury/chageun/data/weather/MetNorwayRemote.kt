package com.naury.chageun.data.weather

import android.content.Context
import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.model.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal sealed interface MetFetch {
    data class Fresh(val body: String, val expires: Instant?, val lastModified: String?) : MetFetch

    /** If-Modified-Since에 304로 답했다. 가진 응답을 그대로 쓰고 만료 시각만 새로 받는다. */
    data class NotModified(val expires: Instant?) : MetFetch
}

internal interface MetNorwayRemote {
    /** 실패하면 [IOException]을 던진다. [point]는 이미 반올림한 값이어야 한다. */
    suspend fun fetch(point: GeoPoint, ifModifiedSince: String?): MetFetch
}

/**
 * MET Norway 이용 조건: 앱을 알아볼 수 있는 User-Agent와 연락처를 보내고, 좌표는 소수 넷째 자리 이하로 보내며,
 * Expires 전에는 다시 묻지 않는다. 데이터는 CC BY 4.0이라 화면에 출처를 적는다.
 * https://api.met.no/doc/TermsOfService
 */
internal class HttpMetNorwayRemote @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : MetNorwayRemote {

    private val userAgent: String by lazy { chageunUserAgent(context) }

    override suspend fun fetch(point: GeoPoint, ifModifiedSince: String?): MetFetch = withContext(ioDispatcher) {
        val url = String.format(Locale.ROOT, ENDPOINT, point.latitude, point.longitude)
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
            ifModifiedSince?.let { setRequestProperty("If-Modified-Since", it) }
        }
        try {
            val expires = connection.getHeaderFieldDate("Expires", 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli)
            when (connection.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> MetFetch.NotModified(expires)
                in HTTP_OK_RANGE -> MetFetch.Fresh(
                    body = connection.inputStream.bufferedReader().use { it.readText() },
                    expires = expires,
                    lastModified = connection.getHeaderField("Last-Modified"),
                )
                else -> throw IOException("weather_http_${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val ENDPOINT = "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.1f&lon=%.1f"
        const val TIMEOUT_MILLIS = 10_000
        val HTTP_OK_RANGE = 200..299
    }
}

/** 앱을 알아볼 수 있는 User-Agent. 앱 버전과 연락처만 담는다. */
internal fun chageunUserAgent(context: Context): String {
    val version = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()
    return "Chageun/$version $USER_AGENT_CONTACT"
}

private const val USER_AGENT_CONTACT = "help@naurylab.com"
