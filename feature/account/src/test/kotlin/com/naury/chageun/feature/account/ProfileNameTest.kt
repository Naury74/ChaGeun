package com.naury.chageun.feature.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.model.AuthUser
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.assertAccessibleControls
import com.naury.chageun.core.uitesting.assertNoClippedText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-hdpi")
class ProfileNameTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val googleUser = AuthUser(
        uid = "uid",
        email = "naury@gmail.com",
        displayName = "Chageun fan",
        isEmailVerified = true,
        methods = setOf(AuthMethod.Google),
        googleName = "Naury",
    )

    private val editName = SemanticsMatcher("can change the name") {
        it.config.getOrNull(SemanticsActions.OnClick)?.label == "Change your name"
    }

    private var editRequests = 0
    private var saves = 0
    private var defaultRequests = 0

    private fun account(user: AuthUser?) = composeRule.setContent {
        AppFrame {
            AccountScreen(
                uiState = AccountUiState(isLoading = false, user = user),
                onBack = {},
                onContinueWithGoogle = {},
                onContinueWithEmail = {},
                onOpenPrivacyPolicy = {},
                onCheckVerification = {},
                onResendVerification = {},
                onSignOut = {},
                onEditName = { editRequests++ },
            )
        }
    }

    private fun dialog(initial: NameEditorState) {
        var state by mutableStateOf(initial)
        composeRule.setContent {
            AppFrame {
                ProfileNameDialog(
                    state = state,
                    onDraftChange = { state = state.copy(draft = it, error = null) },
                    onSave = { saves++ },
                    onUseDefaultName = { defaultRequests++ },
                    onDismiss = {},
                )
            }
        }
    }

    private fun editor(draft: String = "Chageun fan", error: AuthError? = null) = NameEditorState(
        draft = draft,
        initial = "Chageun fan",
        hasCustomName = true,
        usesGoogleName = true,
        error = error,
    )

    @Test
    fun signedIn_showsEditableName() {
        account(googleUser)

        composeRule.onNode(editName).assertExists().performClick()
        assertThat(editRequests).isEqualTo(1)
        composeRule.assertAccessibleControls()
    }

    @Test
    fun signedOut_orUnverified_hasNoNameToEdit() {
        var user by mutableStateOf<AuthUser?>(null)
        composeRule.setContent {
            AppFrame {
                AccountScreen(
                    uiState = AccountUiState(isLoading = false, user = user),
                    onBack = {},
                    onContinueWithGoogle = {},
                    onContinueWithEmail = {},
                    onOpenPrivacyPolicy = {},
                    onCheckVerification = {},
                    onResendVerification = {},
                    onSignOut = {},
                )
            }
        }
        composeRule.onNode(editName).assertDoesNotExist()

        user = AuthUser("uid", "driver@example.com", null, false, setOf(AuthMethod.Email))
        composeRule.onNodeWithText("Check your inbox to finish signing up").assertExists()
        composeRule.onNode(editName).assertDoesNotExist()
    }

    @Test
    fun dialog_validatesLength_andLineBreaks() {
        dialog(editor())
        composeRule.onNodeWithText("11/20").assertExists()
        composeRule.onNodeWithText("Save").assertIsEnabled()

        composeRule.onNode(hasSetTextAction()).performTextReplacement("a".repeat(21))
        composeRule.onNodeWithText("Up to 20 characters. You have 21.").assertExists()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        composeRule.onNode(hasSetTextAction()).performTextReplacement("Nau\nry")
        composeRule.onNodeWithText("Keep it on one line.").assertExists()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
        assertThat(saves).isEqualTo(0)
    }

    @Test
    fun dialog_emptyName_offersGoogleName() {
        dialog(editor())
        composeRule.onNodeWithText("Use Google name").performClick()
        assertThat(defaultRequests).isEqualTo(1)

        composeRule.onNode(hasSetTextAction()).performTextReplacement("  ")
        composeRule.onNodeWithText("Leave it empty to use your Google account name.").assertExists()
        // 비우면 저장 버튼이 곧 "Google 이름 쓰기"가 된다. 같은 버튼을 두 번 두지 않는다.
        composeRule.onNodeWithText("Save").assertDoesNotExist()
        composeRule.onNodeWithText("Use Google name").assertIsEnabled().performClick()
        assertThat(saves).isEqualTo(1)
    }

    @Test
    fun dialog_failure_keepsDraft_andExplains() {
        dialog(editor(draft = "Nau", error = AuthError.Network))

        composeRule.onNodeWithText("Nau").assertExists()
        composeRule.onNodeWithText("Check your internet connection and try again.").assertExists()
        composeRule.assertAccessibleControls()
    }

    @Test
    @Config(fontScale = 2f)
    fun header_atDoubleFont_isNotClipped() {
        account(googleUser.copy(displayName = "A rather long name 20"))

        composeRule.onNode(editName).assertExists()
        composeRule.assertNoClippedText()
        composeRule.assertAccessibleControls()
    }

    @Test
    @Config(fontScale = 2f)
    fun dialog_atDoubleFont_isNotClipped() {
        dialog(editor(error = AuthError.Network))

        composeRule.onNodeWithText("Use Google name").assertExists()
        composeRule.assertNoClippedText()
        composeRule.assertAccessibleControls()
    }
}
