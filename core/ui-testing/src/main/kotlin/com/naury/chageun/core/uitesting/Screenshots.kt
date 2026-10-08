package com.naury.chageun.core.uitesting

import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.naury.chageun.core.designsystem.theme.ChageunSpacing
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.Gutters

/** Screenshot 크기별 Robolectric qualifier. 글자가 실제로 그려지도록 `@GraphicsMode(NATIVE)`와 함께 쓴다. */
object ScreenshotDevices {
    const val PHONE = "w360dp-h800dp-hdpi"
    const val TABLET = "w1280dp-h800dp-hdpi"
    const val PHONE_DARK = "w360dp-h800dp-night-hdpi"
    const val PHONE_KO = "ko-w360dp-h800dp-hdpi"

    /** Medium 폭이 시작되는 600dp. 하단 바 대신 Rail이 나오고, 화면은 아직 한 칸이다. */
    const val MEDIUM_KO = "ko-w600dp-h900dp-hdpi"

    /** Expanded 폭이 시작되는 840dp. 홈과 목록-상세 화면이 두 칸으로 나뉜다. */
    const val EXPANDED_KO = "ko-w840dp-h900dp-hdpi"

    /** 펼친 Pixel 9 Pro Fold 안쪽 화면과 비슷한 크기. Book 자세의 세로 Hinge를 넣어 찍을 때 쓴다. */
    const val FOLD_KO = "ko-w792dp-h820dp-hdpi"

    /** 목록 화면이 세 칸으로 나뉘는 Large 폭(1200dp 이상) 창. */
    const val LARGE_KO = "ko-w1400dp-h900dp-hdpi"
}

/**
 * 기록할 때는 창 전체를 `src/test/screenshots/<name>.png`로 저장하고, 검증할 때는 비교한다.
 * 일반 Unit Test 실행에서는 아무것도 하지 않는다.
 */
@OptIn(ExperimentalRoborazziApi::class)
fun ComposeTestRule.captureScreen(name: String) {
    // 정적인 화면은 한 번만 그려지는데, 같은 Sandbox의 두 번째 테스트부터는 그 그리기가 창에 반영되지 않는다.
    // 캡처 직전에 프레임을 한 번 더 요청한다.
    runOnUiThread { frameView?.rootView?.invalidate() }
    waitForIdle()
    // 창 전체를 캡처하므로 콘텐츠 위의 Dialog와 Sheet도 함께 찍힌다.
    captureScreenRoboImage("src/test/screenshots/$name.png")
}

/**
 * ChageunRoot가 제공하는 테마와 창 Surface. 기본 글자·아이콘 색이 실제 앱과 같아진다.
 * 앱은 창 폭에 따라 여백을 넓히므로, 넓은 창을 찍을 때는 그 폭의 [spacing]을 넘긴다.
 */
@Composable
fun AppFrame(spacing: ChageunSpacing = Gutters.Compact, content: @Composable () -> Unit) {
    val view = LocalView.current
    SideEffect { frameView = view }
    ChageunTheme(spacing = spacing) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
    }
}

private var frameView: View? = null
