package com.naury.chageun.feature.account

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.domain.auth.GoogleIdTokenResult
import com.naury.chageun.core.testing.FakeAnalyticsTracker
import com.naury.chageun.core.testing.FakeAuthRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DeleteAccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val google = FakeGoogleIdTokens()
    private val context: Context = RuntimeEnvironment.getApplication()

    private val analytics = FakeAnalyticsTracker()

    private fun viewModel() = DeleteAccountViewModel(auth, google, analytics)

    @Test
    fun emailAccount_needsCorrectPassword_thenDeletesAccount() = runTest {
        auth.signUpWithEmail("driver@example.com", "chageun1")
        val viewModel = viewModel()
        val ready = viewModel.uiState.first { it.user != null }
        assertThat(ready.confirmsWithPassword).isTrue()
        assertThat(ready.canDelete).isFalse()

        viewModel.setPassword("wrong-pass1")
        viewModel.delete(context)
        assertThat(viewModel.uiState.first { it.error != null }.error)
            .isEqualTo(AuthError.InvalidCredentials)
        assertThat(auth.deletedAccounts).isEqualTo(0)

        viewModel.setPassword("chageun1")
        viewModel.delete(context)
        assertThat(viewModel.uiState.first { it.isDeleted }.error).isNull()
        assertThat(auth.deletedAccounts).isEqualTo(1)
        assertThat(auth.currentUser.value).isNull()
        assertThat(analytics.events).containsExactly(AnalyticsEvent.AccountDeleted)
    }

    @Test
    fun googleAccount_cancelledPicker_keepsAccountSilently() = runTest {
        auth.signInWithGoogle("token")
        val viewModel = viewModel()
        viewModel.uiState.first { it.user != null }

        google.next = GoogleIdTokenResult.Cancelled
        viewModel.delete(context)

        val state = viewModel.uiState.first { !it.isBusy }
        assertThat(state.error).isNull()
        assertThat(state.isDeleted).isFalse()
        assertThat(auth.deletedAccounts).isEqualTo(0)
    }

    @Test
    fun recentLoginRequired_isShown_andAccountKept() = runTest {
        auth.signInWithGoogle("token")
        val viewModel = viewModel()
        viewModel.uiState.first { it.user != null }
        google.next = GoogleIdTokenResult.Token("token")
        auth.nextError = null

        auth.failDeletionWith = AuthError.RecentLoginRequired
        viewModel.delete(context)

        assertThat(viewModel.uiState.first { it.error != null }.error).isEqualTo(AuthError.RecentLoginRequired)
        assertThat(auth.deletedAccounts).isEqualTo(0)
    }
}
