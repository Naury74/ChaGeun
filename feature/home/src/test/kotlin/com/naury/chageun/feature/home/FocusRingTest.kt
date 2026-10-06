package com.naury.chageun.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 키보드로 이동할 때 iOS풍 누름 효과를 쓴 카드와 목록 행에도 초점이 보이는지(기획 §28). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = ScreenshotDevices.PHONE)
class FocusRingTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var opened = ""
    private lateinit var focusManager: FocusManager
    private lateinit var inputModeManager: InputModeManager

    private fun show() {
        composeRule.setContent {
            focusManager = LocalFocusManager.current
            inputModeManager = LocalInputModeManager.current
            AppFrame {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressable(onClick = { opened = "card" }),
                    ) { Text("Mileage card", Modifier.padding(24.dp)) }
                    CardGroup(null) {
                        ListRow(Icons.Filled.Build, "Engine oil", onClick = { opened = "oil" })
                        ListRow(Icons.Filled.LocalGasStation, "Fuel", onClick = { opened = "fuel" })
                    }
                }
            }
        }
        // 터치 모드에서는 누를 수 있는 항목이 초점을 받지 않으므로 키보드로 조작하는 상태로 바꾼다.
        composeRule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tabMovesFocusThroughCardsAndRows_andEnterOpens() {
        show()
        // 테스트 창은 처음에 초점이 없으므로 첫 카드에서 시작한다.
        composeRule.onNodeWithText("Mileage card").requestFocus()

        // Tab 키는 다음 초점으로 옮기는 이 동작을 부른다. 테스트에서는 키 이벤트 대신 같은 동작을 직접 부른다.
        composeRule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
        composeRule.onNodeWithText("Engine oil").assertIsFocused()
        composeRule.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
        composeRule.onNodeWithText("Fuel").assertIsFocused()
        composeRule.onNodeWithText("Fuel").performKeyInput { pressKey(Key.Enter) }

        assertThat(opened).isEqualTo("fuel")
    }

    @Test
    fun focusedRow_showsRing() {
        show()
        composeRule.onNodeWithText("Engine oil").requestFocus()
        composeRule.captureScreen("focus_ring_row")
    }

    @Test
    fun focusedCard_showsRing() {
        show()
        composeRule.onNodeWithText("Mileage card").requestFocus()
        composeRule.captureScreen("focus_ring_card")
    }
}
