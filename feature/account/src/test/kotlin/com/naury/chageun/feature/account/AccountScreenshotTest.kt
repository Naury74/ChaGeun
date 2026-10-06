package com.naury.chageun.feature.account

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.model.AuthUser
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccountScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val emailUser = AuthUser("uid", "driver@example.com", null, false, setOf(AuthMethod.Email))

    private fun account(state: AccountUiState) = composeRule.setContent {
        AppFrame {
            AccountScreen(
                uiState = state,
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

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun start_phone() {
        account(AccountUiState(isLoading = false, isGoogleConfigured = true))

        composeRule.onNodeWithText("이메일로 계속하기").assertExists()
        composeRule.assertNoClippedText()
        composeRule.captureScreen("account_start_phone")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.TABLET)
    fun start_tablet_withoutGoogleClient() {
        account(AccountUiState(isLoading = false, isGoogleConfigured = false))

        composeRule.onNodeWithText("Google sign-in is coming soon. You can use email for now.").assertExists()
        composeRule.captureScreen("account_start_tablet")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun home_unverifiedEmail_phone() {
        account(AccountUiState(isLoading = false, user = emailUser))

        composeRule.onNodeWithText("이메일 인증이 필요해요").assertExists()
        composeRule.assertNoClippedText()
        composeRule.captureScreen("account_home_phone")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_DARK)
    fun home_google_dark() {
        account(
            AccountUiState(
                isLoading = false,
                user = AuthUser("uid", "driver@gmail.com", "Driver", true, setOf(AuthMethod.Google)),
            ),
        )

        composeRule.onNodeWithText("Signed in with Google").assertExists()
        composeRule.captureScreen("account_home_dark")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun emailSignUp_withErrors_phone() {
        composeRule.setContent {
            AppFrame {
                EmailAuthScreen(
                    uiState = EmailAuthUiState(
                        mode = EmailAuthMode.SignUp,
                        email = "driver@example",
                        password = "chageun1",
                        passwordConfirm = "chageun2",
                        showFieldErrors = true,
                        error = AuthError.EmailInUse,
                    ),
                    actions = EmailAuthActions(),
                )
            }
        }

        composeRule.onNodeWithText("비밀번호가 서로 달라요").assertExists()
        composeRule.assertNoClippedText()
        composeRule.captureScreen("account_email_sign_up_phone")
    }
}
