package com.naury.chageun.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.compose.ui.platform.UriHandler
import com.naury.chageun.core.designsystem.component.ChageunToasts
import java.util.concurrent.atomic.AtomicInteger

/**
 * 시스템 화면이나 다른 앱을 연다. 문서 선택기·브라우저가 없는 기기나 업무 프로필에서는 처리할 앱이 없어
 * [ActivityNotFoundException]이 나므로, 앱을 종료하지 않고 안내 토스트만 띄운다. 메인 스레드에서 부른다.
 */
fun Context.launchExternal(launch: () -> Unit) {
    try {
        launch()
        OwnScreenLaunch.mark()
    } catch (_: ActivityNotFoundException) {
        ChageunToasts.error(getString(R.string.external_app_unavailable))
    }
}

/** Compose의 [UriHandler]는 처리할 앱이 없으면 IllegalArgumentException으로 감싸서 던진다. */
fun UriHandler.openUriSafely(context: Context, uri: String) {
    try {
        openUri(uri)
    } catch (_: IllegalArgumentException) {
        ChageunToasts.error(context.getString(R.string.external_app_unavailable))
    }
}

/**
 * 앱이 스스로 다른 화면(사진 편집기·사진 고르기·카메라·공유 창)을 띄운 횟수. 화면이 멈춘(ON_STOP) 까닭이
 * 그 화면으로 간 것인지, 사용자가 앱을 떠난 것인지 가릴 때 쓴다. 화면이 앞에 나올 때(ON_START) 센 값과 멈출 때
 * 값이 다르면 그 사이에 앱이 띄운 화면으로 간 것이다. 편집기 결과를 받고 곧바로 공유 창을 띄우는 것처럼
 * ON_START와 ON_RESUME 사이에 띄우는 경우도 있어 "띄웠다/돌아왔다" 표시 대신 횟수로 센다.
 */
object OwnScreenLaunch {
    private val launches = AtomicInteger()

    val count: Int get() = launches.get()

    internal fun mark() {
        launches.incrementAndGet()
    }
}
