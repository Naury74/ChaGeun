package com.naury.chageun.data.cloudbackup

import com.naury.chageun.core.common.dispatcher.ChageunDispatchers
import com.naury.chageun.core.common.dispatcher.Dispatcher
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.domain.cloudbackup.CloudBackup
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Google Drive REST v3의 앱 전용 폴더(`appDataFolder`). 사용자만 볼 수 있고 다른 앱이나 운영자는 접근할 수 없다.
 * 요약은 파일의 appProperties에 둬서 목록 한 번으로 화면을 그린다. 키와 값을 합쳐 124바이트를 넘으면 드라이브가 거절한다.
 */
internal class DriveCloudBackupRemote @Inject constructor(
    private val logger: AppLogger,
    @param:Dispatcher(ChageunDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CloudBackupRemote {

    override suspend fun upload(token: String, backup: CloudBackup, file: File): CloudBackup = call("upload") {
        // 큰 파일도 한 번에 보낼 수 있게 이어 올리기(resumable) 세션을 연다.
        val metadata = JSONObject()
            .put("name", "${backup.id}.zip")
            .put("parents", JSONArray().put(APP_FOLDER))
            .put("mimeType", ZIP_MIME_TYPE)
            .put("appProperties", backup.toAppProperties())
            .toString()
            .toByteArray()
        val session = open("$UPLOAD_URL?uploadType=resumable&fields=$FILE_FIELDS", "POST", token).run {
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("X-Upload-Content-Type", ZIP_MIME_TYPE)
            setRequestProperty("X-Upload-Content-Length", file.length().toString())
            doOutput = true
            setFixedLengthStreamingMode(metadata.size)
            outputStream.use { it.write(metadata) }
            expectSuccess()
            getHeaderField("Location") ?: throw CloudBackupException(CloudBackupError.Unknown)
        }
        val uploaded = open(session, "PUT", token).run {
            setRequestProperty("Content-Type", ZIP_MIME_TYPE)
            doOutput = true
            setFixedLengthStreamingMode(file.length())
            file.inputStream().use { input -> outputStream.use { input.copyTo(it) } }
            expectSuccess()
            JSONObject(inputStream.bufferedReader().use { it.readText() })
        }
        uploaded.toCloudBackup() ?: backup
    }

    override suspend fun list(token: String): List<CloudBackup> = call("list") {
        val query = "spaces=$APP_FOLDER&orderBy=${encode("createdTime desc")}&pageSize=$PAGE_SIZE" +
            "&fields=${encode("files($FILE_FIELDS)")}"
        val body = open("$FILES_URL?$query", "GET", token).run {
            expectSuccess()
            JSONObject(inputStream.bufferedReader().use { it.readText() })
        }
        val files = body.optJSONArray("files") ?: return@call emptyList()
        (0 until files.length()).mapNotNull { files.getJSONObject(it).toCloudBackup() }
    }

    override suspend fun download(token: String, backupId: String, target: File) = call("download") {
        open("$FILES_URL/${encode(backupId)}?alt=media", "GET", token).run {
            expectSuccess()
            inputStream.use { input -> target.outputStream().use { input.copyTo(it) } }
        }
        Unit
    }

    override suspend fun delete(token: String, backupId: String) = call("delete") {
        open("$FILES_URL/${encode(backupId)}", "DELETE", token).run {
            if (responseCode != HttpURLConnection.HTTP_NOT_FOUND) expectSuccess()
        }
        Unit
    }

    private fun open(url: String, method: String, token: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Authorization", "Bearer $token")
        }

    /** 2xx가 아니면 드라이브 오류 이유를 보고 [CloudBackupError]로 바꿔 던진다. */
    private fun HttpURLConnection.expectSuccess() {
        val code = responseCode
        if (code in HTTP_OK_RANGE) return
        val reason = errorStream?.bufferedReader()?.use { it.readText() }?.let(::driveErrorReason)
        throw CloudBackupException(driveError(code, reason))
    }

    private suspend fun <T> call(operation: String, block: suspend () -> T): T = withContext(ioDispatcher) {
        try {
            block()
        } catch (e: IOException) {
            throw CloudBackupException(CloudBackupError.Network, e)
        } catch (e: CloudBackupException) {
            if (e.error == CloudBackupError.Unknown || e.error == CloudBackupError.Unavailable) {
                logger.warn("drive_${operation}_failed", LogField.ErrorType(e.error.name))
            }
            throw e
        }
    }

    private companion object {
        const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        const val APP_FOLDER = "appDataFolder"
        const val ZIP_MIME_TYPE = "application/zip"
        const val FILE_FIELDS = "id,name,size,createdTime,appProperties"
        const val PAGE_SIZE = 50
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 60_000
        val HTTP_OK_RANGE = 200..299

        fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    }
}

internal const val PROPERTY_SCHEMA = "schema"
internal const val PROPERTY_APP_VERSION = "app"
internal const val PROPERTY_DEVICE = "device"
internal const val PROPERTY_VEHICLE = "vehicle"
internal const val PROPERTY_RECORDS = "records"
internal const val PROPERTY_PHOTOS = "photos"

/** 드라이브는 appProperties의 키+값을 124바이트(UTF-8)로 제한한다. 한글 차량 이름은 잘라서 넣는다. */
private const val MAX_PROPERTY_BYTES = 124

internal fun CloudBackup.toAppProperties(): JSONObject = JSONObject().apply {
    put(PROPERTY_SCHEMA, schemaVersion.toString())
    putFitting(PROPERTY_APP_VERSION, appVersion)
    deviceModel?.let { putFitting(PROPERTY_DEVICE, it) }
    vehicleLabel?.let { putFitting(PROPERTY_VEHICLE, it) }
    put(PROPERTY_RECORDS, recordCount.toString())
    put(PROPERTY_PHOTOS, photoCount.toString())
}

private fun JSONObject.putFitting(key: String, value: String) {
    var fitted = value
    while ((key + fitted).toByteArray().size > MAX_PROPERTY_BYTES) fitted = fitted.dropLast(1)
    put(key, fitted)
}

/** 요약이 없는 파일(사용자가 손댄 경우 등)은 목록에서 뺀다. */
internal fun JSONObject.toCloudBackup(): CloudBackup? {
    val properties = optJSONObject("appProperties") ?: return null
    val schema = properties.optString(PROPERTY_SCHEMA).toIntOrNull() ?: return null
    return CloudBackup(
        id = optString("id").takeIf { it.isNotEmpty() } ?: return null,
        createdAt = runCatching { Instant.parse(optString("createdTime")) }.getOrNull() ?: return null,
        sizeBytes = optString("size").toLongOrNull() ?: 0,
        schemaVersion = schema,
        appVersion = properties.optString(PROPERTY_APP_VERSION),
        deviceModel = properties.optString(PROPERTY_DEVICE).takeIf { it.isNotEmpty() },
        vehicleLabel = properties.optString(PROPERTY_VEHICLE).takeIf { it.isNotEmpty() },
        recordCount = properties.optString(PROPERTY_RECORDS).toIntOrNull() ?: 0,
        photoCount = properties.optString(PROPERTY_PHOTOS).toIntOrNull() ?: 0,
    )
}

internal fun driveErrorReason(body: String): String? = runCatching {
    JSONObject(body).getJSONObject("error").getJSONArray("errors").getJSONObject(0).getString("reason")
}.getOrNull()

/** 401은 권한이 만료·취소된 경우, 403은 용량·설정 문제다. 5xx와 429는 잠시 뒤 다시 하면 된다. */
internal fun driveError(code: Int, reason: String?): CloudBackupError = when {
    code == HttpURLConnection.HTTP_UNAUTHORIZED -> CloudBackupError.NotConnected
    reason == "storageQuotaExceeded" -> CloudBackupError.StorageFull
    code == HTTP_TOO_MANY_REQUESTS || code >= HttpURLConnection.HTTP_INTERNAL_ERROR -> CloudBackupError.Network
    reason == "userRateLimitExceeded" || reason == "rateLimitExceeded" -> CloudBackupError.Network
    code == HttpURLConnection.HTTP_FORBIDDEN -> CloudBackupError.Unavailable
    else -> CloudBackupError.Unknown
}

private const val HTTP_TOO_MANY_REQUESTS = 429
