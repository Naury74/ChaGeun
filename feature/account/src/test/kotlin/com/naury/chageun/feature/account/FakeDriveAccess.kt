package com.naury.chageun.feature.account

import android.content.Intent
import com.naury.chageun.core.auth.DriveConnectRequest
import com.naury.chageun.core.auth.GoogleDriveAccess
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeDriveAccess(connected: Boolean = false) : GoogleDriveAccess {
    override val isConnected = MutableStateFlow(connected)

    /** 정하면 다음 연결 요청이 이 결과를 돌려준다. 기본은 바로 연결된다. */
    var nextRequest: DriveConnectRequest? = null
    var grantConsent = true

    override suspend fun connect(): DriveConnectRequest {
        val request = nextRequest ?: DriveConnectRequest.Connected
        nextRequest = null
        if (request == DriveConnectRequest.Connected) isConnected.value = true
        return request
    }

    override suspend fun completeConsent(data: Intent?): Boolean {
        isConnected.value = grantConsent
        return grantConsent
    }

    override suspend fun accessToken(): String? = if (isConnected.value) "token" else null

    override fun disconnect() {
        isConnected.value = false
    }
}
