package com.naury.chageun.feature.manage

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 탭 화면 큰 제목이 펼친 상태와, 스크롤해 가운데 작은 제목으로 접힌 상태. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = ScreenshotDevices.PHONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LargeTitleScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show() = composeRule.setContent {
        AppFrame {
            LargeTitleScaffold(title = "Care") { padding ->
                LazyColumn(Modifier.padding(padding).testTag("list")) {
                    items(40) { Text("Row $it", Modifier.fillMaxWidth().padding(16.dp)) }
                }
            }
        }
    }

    @Test
    fun expanded() {
        show()
        composeRule.captureScreen("large_title_expanded")
    }

    @Test
    fun collapsed() {
        show()
        composeRule.onNodeWithTag("list").performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        composeRule.captureScreen("large_title_collapsed")
    }
}
