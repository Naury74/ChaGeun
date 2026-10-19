package com.naury.chageun.core.designsystem.component

import androidx.compose.runtime.Composable
import com.naury.kinetica.ui.KStatus
import com.naury.kinetica.ui.KToaster
import com.naury.kinetica.ui.KToasterState

/**
 * 앱 전체가 함께 쓰는 Kinetica 토스트. 시스템 Toast 대신 화면 위에서 미끄러져 내려오고, 종류마다 아이콘과 색이 다르며
 * 옆으로 쓸어 닫는다. 편집기 결과 처리처럼 Composable 밖의 콜백에서도 [show]로 띄운다. 메인 스레드에서 부른다.
 */
object ChageunToasts {
    val state = KToasterState()

    fun show(message: String, status: KStatus = KStatus.Neutral): Long = state.show(message, status)

    fun success(message: String): Long = state.show(message, KStatus.Success)

    fun error(message: String): Long = state.show(message, KStatus.Error)
}

/** [ChageunToasts]를 그리는 자리. 앱 루트에 한 번만 둔다. */
@Composable
fun ChageunToastHost() {
    KToaster(ChageunToasts.state)
}
