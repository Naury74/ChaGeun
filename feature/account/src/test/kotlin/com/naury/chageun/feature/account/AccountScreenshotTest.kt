package com.naury.chageun.feature.account

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.domain.backup.ImportPreview
import com.naury.chageun.core.domain.backup.LocalDataSummary
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.model.AuthUser
import com.naury.chageun.core.testing.FakeCloudBackupRepository
import com.naury.chageun.core.uitesting.AppFrame
import com.naury.chageun.core.uitesting.ScreenshotDevices
import com.naury.chageun.core.uitesting.assertNoClippedText
import com.naury.chageun.core.uitesting.captureScreen
import java.time.Instant
import java.util.TimeZone
import org.junit.After
import org.junit.Before
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

    // 백업 시각은 기기 시간대로 보여 준다. CI(UTC)와 개발 PC에서 같은 그림이 나오게 고정한다.
    private val originalTimeZone = TimeZone.getDefault()

    @Before
    fun fixTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

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
    fun home_withBackups_phone() {
        composeRule.setContent {
            AppFrame {
                AccountScreen(
                    uiState = AccountUiState(isLoading = false, user = emailUser.copy(isEmailVerified = true)),
                    onBack = {},
                    onContinueWithGoogle = {},
                    onContinueWithEmail = {},
                    onOpenPrivacyPolicy = {},
                    onCheckVerification = {},
                    onResendVerification = {},
                    onSignOut = {},
                    backup = CloudBackupUiState(
                        isLoaded = true,
                        backups = listOf(
                            FakeCloudBackupRepository.backup("b1", Instant.parse("2026-10-06T05:30:00Z")),
                            FakeCloudBackupRepository.backup("b0", Instant.parse("2026-09-29T11:00:00Z"), 10, 2),
                        ),
                    ),
                )
            }
        }

        composeRule.onNodeWithText("백업 목록 · 최근 5개 보관").assertExists()
        composeRule.assertNoClippedText()
        composeRule.captureScreen("account_home_backups_phone")
    }

    @Test
    @Config(qualifiers = ScreenshotDevices.PHONE_KO)
    fun restoreConfirm_phone() {
        val backup = FakeCloudBackupRepository.backup("b1", Instant.parse("2026-10-06T05:30:00Z"))
        composeRule.setContent {
            AppFrame {
                CloudBackupDialogs(
                    CloudBackupUiState(
                        restore = RestoreStep.Confirming(
                            backup,
                            "file:///b1.zip",
                            ImportPreview.Ready(LocalDataSummary(1, 12, 3), LocalDataSummary(1, 4, 0)),
                        ),
                    ),
                    CloudBackupActions(),
                )
            }
        }

        composeRule.onNodeWithText("이 백업으로 바꿀까요?").assertExists()
        composeRule.captureScreen("account_restore_confirm_phone")
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
