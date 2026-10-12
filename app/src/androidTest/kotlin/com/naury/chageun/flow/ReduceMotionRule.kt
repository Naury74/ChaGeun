package com.naury.chageun.flow

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource

/**
 * 테스트 동안 시스템 '애니메이션 삭제'(animator_duration_scale 0)를 켜고, 끝나면 원래 값으로 돌린다.
 *
 * 앱은 이 값을 rememberReduceMotion으로 읽어 날씨 하늘처럼 끝없이 프레임을 요청하는 움직임을 멈춘 한 장면으로 그린다.
 * 하늘이 계속 움직이면 Compose 테스트가 화면이 쉬지 않는다고 보고 기다리다 실패한다. 앱은 화면을 처음 그릴 때 값을
 * 읽으므로 Activity를 띄우는 규칙보다 먼저 적용해야 한다. 같은 기기에서 다음 테스트나 사람이 쓸 때 영향이 남지 않게
 * 원래 값으로 되돌린다.
 */
class ReduceMotionRule : ExternalResource() {

    private var previous: String? = null

    override fun before() {
        previous = shell("settings get global $KEY")
        shell("settings put global $KEY 0")
    }

    override fun after() {
        val value = previous
        if (value == null || value == UNSET) {
            shell("settings delete global $KEY")
        } else {
            shell("settings put global $KEY $value")
        }
    }

    /** 셸 권한으로 실행하고 끝날 때까지 출력을 읽는다. 출력을 읽지 않으면 명령이 끝나기 전에 다음으로 넘어간다. */
    private fun shell(command: String): String {
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(output).bufferedReader().use { it.readText() }.trim()
    }

    private companion object {
        const val KEY = "animator_duration_scale"

        // 한 번도 바꾸지 않은 기기는 값이 없어 "null"을 돌려준다.
        const val UNSET = "null"
    }
}
