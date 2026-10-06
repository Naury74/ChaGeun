package com.naury.chageun.core.auth

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.naury.chageun.core.common.logging.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/** 드라이브 연결 요청 결과. 처음 연결할 때는 사용자가 계정과 권한을 고르는 화면을 거친다. */
sealed interface DriveConnectRequest {
    data object Connected : DriveConnectRequest

    /** 화면에서 [intentSender]를 띄우고 결과를 [GoogleDriveAccess.completeConsent]에 넘긴다. */
    data class NeedsConsent(val intentSender: IntentSender) : DriveConnectRequest

    data object Failed : DriveConnectRequest
}

/**
 * 사용자 본인 Google 드라이브의 앱 전용 숨김 폴더(`drive.appdata`) 권한. 차근 계정 로그인과는 따로 동작한다.
 * 운영자 서버를 거치지 않고 기기에서 드라이브로 바로 올리므로 개발자 비용이 없다(ADR-006).
 */
interface GoogleDriveAccess {
    val isConnected: StateFlow<Boolean>

    suspend fun connect(): DriveConnectRequest

    /** 권한 화면에서 돌아온 결과. 허용했으면 true. */
    suspend fun completeConsent(data: Intent?): Boolean

    /** 화면 없이 접근 토큰을 받는다. 연결하지 않았거나 다시 동의가 필요하면 null. 자동 백업에서도 쓴다. */
    suspend fun accessToken(): String?

    /** 이 기기에서 연결을 끊는다. 드라이브의 백업 파일은 그대로 남는다. */
    fun disconnect()
}

@Singleton
internal class PlayServicesGoogleDriveAccess @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: AppLogger,
) : GoogleDriveAccess {

    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val connected = MutableStateFlow(preferences.getBoolean(KEY_CONNECTED, false))
    override val isConnected: StateFlow<Boolean> = connected.asStateFlow()

    private val request = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE))).build()

    override suspend fun connect(): DriveConnectRequest {
        val result = authorize() ?: return DriveConnectRequest.Failed
        val consent = result.pendingIntent
        return when {
            result.hasResolution() && consent != null -> DriveConnectRequest.NeedsConsent(consent.intentSender)
            result.accessToken != null -> {
                setConnected(true)
                DriveConnectRequest.Connected
            }
            else -> DriveConnectRequest.Failed
        }
    }

    override suspend fun completeConsent(data: Intent?): Boolean {
        val token = try {
            Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data).accessToken
        } catch (e: ApiException) {
            logger.warn("drive_consent_failed", error = e)
            null
        }
        setConnected(token != null)
        return token != null
    }

    override suspend fun accessToken(): String? {
        if (!connected.value) return null
        val result = authorize() ?: return null
        // 사용자가 Google 계정 설정에서 권한을 거둔 경우다. 다시 연결해야 한다.
        if (result.hasResolution()) {
            setConnected(false)
            return null
        }
        return result.accessToken
    }

    override fun disconnect() = setConnected(false)

    @Suppress("TooGenericExceptionCaught") // Play 서비스가 없거나 낡은 기기에서도 앱이 멈추지 않게 한다.
    private suspend fun authorize(): AuthorizationResult? = try {
        Identity.getAuthorizationClient(context).authorize(request).await()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.warn("drive_authorize_failed", error = e)
        null
    }

    private fun setConnected(value: Boolean) {
        preferences.edit().putBoolean(KEY_CONNECTED, value).apply()
        connected.value = value
    }

    private companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val PREFERENCES = "google_drive_backup"
        const val KEY_CONNECTED = "connected"
    }
}
