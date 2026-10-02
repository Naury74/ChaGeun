package com.naury.chageun.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.UriHandler

/**
 * 시스템 화면이나 다른 앱을 연다. 문서 선택기·브라우저가 없는 기기나 업무 프로필에서는 처리할 앱이 없어
 * [ActivityNotFoundException]이 나므로, 앱을 종료하지 않고 안내만 띄운다.
 */
fun Context.launchExternal(launch: () -> Unit) {
    try {
        launch()
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.external_app_unavailable, Toast.LENGTH_SHORT).show()
    }
}

/** Compose의 [UriHandler]는 처리할 앱이 없으면 IllegalArgumentException으로 감싸서 던진다. */
fun UriHandler.openUriSafely(context: Context, uri: String) {
    try {
        openUri(uri)
    } catch (_: IllegalArgumentException) {
        Toast.makeText(context, R.string.external_app_unavailable, Toast.LENGTH_SHORT).show()
    }
}
