package com.naury.chageun.core.auth

import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.common.logging.AppLogger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** 프로필 사진을 받아 온다. 받지 못하면 null이고, 화면은 이름 첫 글자를 대신 보인다. */
internal fun interface ProfilePhotoDownloader {
    suspend fun download(url: String): ByteArray?
}

/**
 * HTTPS 주소만 받는다. 사진 주소와 응답 내용은 로그에 남기지 않는다(예외도 클래스 이름만 남는다).
 */
internal class HttpProfilePhotoDownloader @Inject constructor(
    private val logger: AppLogger,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ProfilePhotoDownloader {

    override suspend fun download(url: String): ByteArray? = withContext(ioDispatcher) {
        val sized = sizedPhotoUrl(url) ?: return@withContext null
        try {
            fetch(sized)
        } catch (e: IOException) {
            logger.warn("profile_photo_download_failed", error = e)
            null
        } catch (e: IllegalArgumentException) {
            logger.warn("profile_photo_download_failed", error = e)
            null
        }
    }

    private fun fetch(url: String): ByteArray? {
        val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            instanceFollowRedirects = true
        }
        return try {
            val isImage = connection.contentType.orEmpty().startsWith("image/")
            if (connection.responseCode in HTTP_OK_RANGE && isImage) {
                connection.inputStream.use { it.readAtMost(MAX_BYTES) }
            } else {
                logger.warn("profile_photo_download_rejected")
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000
        val HTTP_OK_RANGE = 200..299
    }
}

/** 사진 한 장으로는 지나치게 큰 응답을 막는다. 192px 사진은 보통 수십 KB다. */
internal const val MAX_BYTES = 1_000_000

/** [limit]보다 크면 [IOException]을 던진다. */
internal fun InputStream.readAtMost(limit: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(BUFFER_SIZE)
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        out.write(buffer, 0, read)
        if (out.size() > limit) throw IOException("profile_photo_too_large")
    }
    return out.toByteArray()
}

private const val BUFFER_SIZE = 8 * 1024

/**
 * 동그라미에 쓸 크기로 바꾼 사진 주소. HTTPS가 아니면 null이다.
 * Google 사진 주소는 끝의 `=s96-c` 같은 꼬리로 크기를 고른다. 가장 큰 계정 화면 동그라미(72dp)를
 * 고밀도 화면에서도 선명하게 보이도록 두 배 남짓인 [PHOTO_PIXELS]로 받는다.
 */
internal fun sizedPhotoUrl(url: String, pixels: Int = PHOTO_PIXELS): String? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true)) return null
    val host = uri.host.orEmpty()
    if (!host.endsWith(GOOGLE_PHOTO_HOST)) return url
    val sizeSuffix = "=s$pixels-c"
    return when {
        SIZE_SUFFIX.containsMatchIn(url) -> url.replace(SIZE_SUFFIX, sizeSuffix)
        '=' !in url.substringAfterLast('/') -> url + sizeSuffix
        else -> url
    }
}

internal const val PHOTO_PIXELS = 192
private const val GOOGLE_PHOTO_HOST = "googleusercontent.com"
private val SIZE_SUFFIX = Regex("=s\\d+(-c)?$")
