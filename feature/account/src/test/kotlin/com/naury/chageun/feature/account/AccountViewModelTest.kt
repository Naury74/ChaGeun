package com.naury.chageun.feature.account

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.auth.GoogleIdTokenSource
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.domain.auth.GoogleIdTokenResult
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.testing.FakeAnalyticsTracker
import com.naury.chageun.core.testing.FakeAuthRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val analytics = FakeAnalyticsTracker()
    private val google = FakeGoogleIdTokens()
    private val context: Context = RuntimeEnvironment.getApplication()

    private fun viewModel() = AccountViewModel(auth, google, analytics)

    @Test
    fun newGoogleAccount_signsIn_andTracksSignUp() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        google.next = GoogleIdTokenResult.Token("new-token")

        viewModel.signInWithGoogle(context)

        val state = viewModel.uiState.first { it.user != null }
        assertThat(state.user?.methods).containsExactly(AuthMethod.Google)
        assertThat(state.user?.needsEmailVerification).isFalse()
        assertThat(analytics.events).containsExactly(AnalyticsEvent.SignUp(AuthMethod.Google))
    }

    @Test
    fun cancelledPicker_isSilent_butMissingAccountIsExplained() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        google.next = GoogleIdTokenResult.Cancelled
        viewModel.signInWithGoogle(context)
        assertThat(viewModel.uiState.first { !it.isBusy && !it.isLoading }.notice).isNull()

        google.next = GoogleIdTokenResult.NoAccount
        viewModel.signInWithGoogle(context)
        assertThat(viewModel.uiState.first { it.notice != null }.notice)
            .isEqualTo(AccountNotice.Failed(AuthError.NoGoogleAccount))
        assertThat(analytics.events).isEmpty()
    }

    @Test
    fun verification_checksServer_andSignOutKeepsNothingSignedIn() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        auth.signUpWithEmail("driver@example.com", "chageun1")

        viewModel.checkVerification()
        assertThat(viewModel.uiState.first { it.notice != null }.notice).isEqualTo(AccountNotice.NotVerifiedYet)
        viewModel.dismissNotice()

        auth.verifyEmailOutside("driver@example.com")
        viewModel.checkVerification()
        val verified = viewModel.uiState.first { it.notice == AccountNotice.Verified }
        assertThat(verified.user?.needsEmailVerification).isFalse()
        assertThat(analytics.events).containsExactly(AnalyticsEvent.SignUp(AuthMethod.Email))

        viewModel.signOut()
        assertThat(viewModel.uiState.first { it.user == null && !it.isBusy }.user).isNull()
    }

    @Test
    fun resend_reportsSentOrError() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        auth.signUpWithEmail("driver@example.com", "chageun1")

        auth.nextError = AuthError.TooManyRequests
        viewModel.resendVerification()
        assertThat(viewModel.uiState.first { it.notice != null }.notice)
            .isEqualTo(AccountNotice.Failed(AuthError.TooManyRequests))
        viewModel.dismissNotice()

        viewModel.resendVerification()
        assertThat(viewModel.uiState.first { it.notice != null }.notice).isEqualTo(AccountNotice.VerificationSent)
        assertThat(auth.verificationMails).isEqualTo(2)
    }
}

@RunWith(RobolectricTestRunner::class)
class AccountStartOverTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun startOver_deletesUnverifiedAccount() = runTest {
        val auth = FakeAuthRepository()
        auth.signUpWithEmail("typo@example.com", "chageun1")
        val viewModel = AccountViewModel(auth, FakeGoogleIdTokens(), FakeAnalyticsTracker())
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.startOver()

        assertThat(viewModel.uiState.first { it.user == null && !it.isLoading }.user).isNull()
        assertThat(auth.deletedAccounts).isEqualTo(1)
    }
}

internal class FakeGoogleIdTokens(override val isConfigured: Boolean = true) : GoogleIdTokenSource {
    var next: GoogleIdTokenResult = GoogleIdTokenResult.Cancelled

    override suspend fun request(activityContext: Context): GoogleIdTokenResult = next
}
