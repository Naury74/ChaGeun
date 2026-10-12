package com.naury.chageun.core.ui

import androidx.activity.ComponentDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertAccessibleControls
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = ScreenshotDevices.PHONE)
class ChageunDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var confirmed = 0
    private var cancelled = 0
    private var dismissed = 0

    private fun showSignOut(open: () -> Boolean = { true }) = composeRule.setContent {
        AppFrame {
            if (open()) {
                ChageunDialog(
                    title = "Sign out?",
                    message = "Your records stay on this phone. Backups stop until you sign in again.",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onDismiss = { dismissed++ },
                    actions = listOf(
                        DialogAction("Sign out", DialogActionStyle.Destructive) { confirmed++ },
                        DialogAction("Cancel", DialogActionStyle.Secondary) { cancelled++ },
                    ),
                )
            }
        }
    }

    @Test
    fun buttons_callTheirActions() {
        showSignOut()

        composeRule.onNode(hasText("Sign out") and hasAnyAncestor(isDialog())).performClick()
        composeRule.onNode(hasText("Cancel") and hasAnyAncestor(isDialog())).performClick()

        assertThat(confirmed).isEqualTo(1)
        assertThat(cancelled).isEqualTo(1)
        assertThat(dismissed).isEqualTo(0)
    }

    @Test
    fun title_isHeading_andControlsAreAccessible() {
        showSignOut()

        composeRule.onNodeWithText("Sign out?").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.assertAccessibleControls()
    }

    @Test
    fun backPress_dismissesAfterClosing() {
        var isOpen by mutableStateOf(true)
        showSignOut { isOpen }

        composeRule.runOnUiThread {
            (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        assertThat(dismissed).isEqualTo(1)
        isOpen = false
    }

    @Test
    fun talkBackDismissAction_closesTheDialog() {
        showSignOut()

        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss)
        composeRule.waitForIdle()

        assertThat(dismissed).isEqualTo(1)
    }

    @Test
    fun notDismissible_ignoresBackPress() {
        composeRule.setContent {
            ChageunTheme {
                ChageunDialog(
                    title = "Restoring",
                    onDismiss = { dismissed++ },
                    actions = emptyList(),
                    dismissible = false,
                )
            }
        }

        composeRule.runOnUiThread {
            (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        assertThat(dismissed).isEqualTo(0)
    }

    @Test
    fun optionDialog_picksAnOption() {
        var picked: String? = null
        composeRule.setContent {
            AppFrame {
                ChageunOptionDialog(
                    title = "Add a photo",
                    options = listOf(
                        DialogOption("Take a photo", { picked = "camera" }),
                        DialogOption("Choose from gallery", { picked = "gallery" }),
                    ),
                    cancelLabel = "Cancel",
                    onDismiss = { dismissed++ },
                )
            }
        }

        composeRule.onNodeWithText("Choose from gallery").performClick()

        assertThat(picked).isEqualTo("gallery")
        composeRule.assertAccessibleControls()
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO, fontScale = 2f)
    fun largeFont_keepsEveryLineVisible() {
        composeRule.setContent {
            AppFrame {
                ChageunDialog(
                    title = "모든 데이터를 지울까요?",
                    message = "차량, 정비 기록, 사진이 이 휴대폰에서 모두 지워져요. 되돌릴 수 없어요.",
                    icon = Icons.Filled.DeleteForever,
                    tone = ChageunTheme.colors.critical,
                    onDismiss = {},
                    actions = listOf(
                        DialogAction("모두 지우기", DialogActionStyle.Destructive) {},
                        DialogAction("취소", DialogActionStyle.Secondary) {},
                    ),
                )
            }
        }

        composeRule.assertNoClippedText()
        composeRule.captureScreen("dialog_destructive_phone_large_font")
    }
}
