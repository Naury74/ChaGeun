package com.naury.chageun.feature.account

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.testing.FakeAnalyticsTracker
import com.naury.chageun.core.testing.FakeAuthRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class EmailAuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val analytics = FakeAnalyticsTracker()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = EmailAuthViewModel(handle, auth, analytics)

    @Test
    fun signUp_needsStrongMatchingPasswordAndAgreement() = runTest {
        val viewModel = viewModel()
        viewModel.setMode(EmailAuthMode.SignUp)
        viewModel.setEmail("driver@example.com")
        viewModel.setPassword("short")
        viewModel.setPasswordConfirm("short")
        assertThat(viewModel.uiState.value.canSubmit).isFalse()

        viewModel.setPrivacyAgreed(true)
        viewModel.submit()
        assertThat(viewModel.uiState.value.showFieldErrors).isTrue()
        assertThat(viewModel.uiState.value.isPasswordWeak).isTrue()
        assertThat(auth.currentUser.value).isNull()

        viewModel.setPassword("chageun1")
        viewModel.setPasswordConfirm("chageun2")
        viewModel.submit()
        assertThat(viewModel.uiState.value.isConfirmMismatch).isTrue()

        viewModel.setPasswordConfirm("chageun1")
        viewModel.submit()
        val done = viewModel.uiState.first { it.isCompleted }
        assertThat(done.password).isEmpty()
        assertThat(auth.currentUser.value?.needsEmailVerification).isTrue()
        assertThat(auth.verificationMails).isEqualTo(1)
        // 가입 이벤트는 인증을 마칠 때 센다.
        assertThat(analytics.events).isEmpty()
    }

    @Test
    fun signIn_showsServerError_andClearsItWhenTyping() = runTest {
        auth.signUpWithEmail("driver@example.com", "chageun1")
        auth.verifyEmailOutside("driver@example.com")
        auth.signOut()
        val viewModel = viewModel()
        viewModel.setEmail("driver@example.com")
        viewModel.setPassword("wrong-password1")

        viewModel.submit()
        assertThat(viewModel.uiState.first { !it.isBusy }.error).isEqualTo(AuthError.InvalidCredentials)
        viewModel.setPassword("chageun1")
        assertThat(viewModel.uiState.value.error).isNull()

        viewModel.submit()
        assertThat(viewModel.uiState.first { it.isCompleted }.isCompleted).isTrue()
        assertThat(analytics.events).containsExactly(AnalyticsEvent.Login(AuthMethod.Email))
    }

    @Test
    fun modeAndEmail_surviveRecreation_butPasswordDoesNot() {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            setMode(EmailAuthMode.SignUp)
            setEmail("driver@example.com")
            setPassword("chageun1")
        }

        val restored = viewModel(handle).uiState.value

        assertThat(restored.mode).isEqualTo(EmailAuthMode.SignUp)
        assertThat(restored.email).isEqualTo("driver@example.com")
        assertThat(restored.password).isEmpty()
    }

    @Test
    fun passwordReset_prefillsEmail_andConfirmsSending() = runTest {
        val viewModel = viewModel()
        viewModel.setEmail(" driver@example.com ")

        viewModel.openPasswordReset()
        assertThat(viewModel.uiState.value.reset?.email).isEqualTo("driver@example.com")
        viewModel.sendPasswordReset()

        assertThat(viewModel.uiState.first { it.reset?.isSent == true }.reset?.error).isNull()
        assertThat(auth.passwordResets).containsExactly("driver@example.com")
        viewModel.closePasswordReset()
        assertThat(viewModel.uiState.value.reset).isNull()
    }
}
