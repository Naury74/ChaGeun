package com.naury.chageun.feature.account

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.auth.AuthError
import com.naury.chageun.core.model.AuthMethod
import com.naury.chageun.core.model.AuthUser
import com.naury.chageun.core.testing.FakeAnalyticsTracker
import com.naury.chageun.core.testing.FakeAuthRepository
import com.naury.chageun.core.testing.FakeProfileNameRepository
import com.naury.chageun.core.testing.FakeProfilePhotoRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountNameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val names = FakeProfileNameRepository(auth)

    private val googleUser = AuthUser(
        uid = "uid",
        email = "naury@gmail.com",
        displayName = null,
        isEmailVerified = true,
        methods = setOf(AuthMethod.Google),
        googleName = "나우리",
    )

    private fun TestScope.viewModel(user: AuthUser? = googleUser): AccountViewModel {
        auth.signedInUser = user
        val viewModel =
            AccountViewModel(auth, FakeGoogleIdTokens(), FakeAnalyticsTracker(), FakeProfilePhotoRepository(), names)
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    private suspend fun AccountViewModel.openEditor(): NameEditorState {
        uiState.first { it.user != null }
        editName()
        return checkNotNull(uiState.first { it.nameEditor != null }.nameEditor)
    }

    @Test
    fun edit_prefillsCurrentName() = runTest {
        val editor = viewModel().openEditor()

        assertThat(editor.draft).isEqualTo("나우리")
        assertThat(editor.hasCustomName).isFalse()
        assertThat(editor.usesGoogleName).isTrue()
    }

    @Test
    fun save_trimsName_andShowsItRightAway() = runTest {
        val viewModel = viewModel()
        viewModel.openEditor()

        viewModel.changeNameDraft("  차근이 ")
        viewModel.saveName()

        val state = viewModel.uiState.first { it.nameEditor == null }
        assertThat(state.user?.profileName).isEqualTo("차근이")
        assertThat(names.savedNames).containsExactly("차근이")
    }

    @Test
    fun saveFailure_keepsDialogAndDraft_withReason() = runTest {
        val viewModel = viewModel()
        viewModel.openEditor()
        names.nextError = AuthError.Network

        viewModel.changeNameDraft("차근이")
        viewModel.saveName()

        val editor = checkNotNull(viewModel.uiState.first { it.nameEditor?.error != null }.nameEditor)
        assertThat(editor.draft).isEqualTo("차근이")
        assertThat(editor.error).isEqualTo(AuthError.Network)
        assertThat(editor.isSaving).isFalse()
        assertThat(viewModel.uiState.value.user?.profileName).isEqualTo("나우리")

        // 다시 고치면 실패 안내는 사라진다.
        viewModel.changeNameDraft("차근이!")
        assertThat(viewModel.uiState.first { it.nameEditor?.draft == "차근이!" }.nameEditor?.error).isNull()
    }

    @Test
    fun invalidName_isNotSaved() = runTest {
        val viewModel = viewModel()
        viewModel.openEditor()

        viewModel.changeNameDraft("가".repeat(21))
        viewModel.saveName()
        viewModel.changeNameDraft("나우\n리")
        viewModel.saveName()

        assertThat(viewModel.uiState.first { it.nameEditor?.draft == "나우\n리" }.nameEditor).isNotNull()
        assertThat(names.savedNames).isEmpty()
    }

    @Test
    fun unchangedName_closesWithoutSaving() = runTest {
        val viewModel = viewModel()
        viewModel.openEditor()

        viewModel.saveName()

        assertThat(viewModel.uiState.first { it.nameEditor == null }.user?.displayName).isNull()
        // Google 이름을 그대로 저장하면 이름이 고정돼 Google에서 바꿔도 따라가지 않는다. 그래서 묻지 않는다.
        assertThat(names.savedNames).isEmpty()
    }

    @Test
    fun useDefaultName_clearsUserSetName_backToGoogleName() = runTest {
        val viewModel = viewModel(googleUser.copy(displayName = "차근이"))
        val editor = viewModel.openEditor()
        assertThat(editor.draft).isEqualTo("차근이")
        assertThat(editor.hasCustomName).isTrue()

        viewModel.saveName(useDefaultName = true)

        val state = viewModel.uiState.first { it.nameEditor == null }
        assertThat(state.user?.profileName).isEqualTo("나우리")
        assertThat(names.savedNames).containsExactly(null)
    }

    @Test
    fun emptyName_resetsToEmailPart_forEmailAccount() = runTest {
        val emailUser = AuthUser("uid", "driver.kim@example.com", "운전왕", true, setOf(AuthMethod.Email))
        val viewModel = viewModel(emailUser)
        assertThat(viewModel.openEditor().usesGoogleName).isFalse()

        viewModel.changeNameDraft("   ")
        viewModel.saveName()

        assertThat(viewModel.uiState.first { it.nameEditor == null }.user?.profileName).isEqualTo("driver.kim")
        assertThat(names.savedNames).containsExactly(null)
    }

    @Test
    fun signOut_closesEditor() = runTest {
        val viewModel = viewModel()
        viewModel.openEditor()

        viewModel.signOut()

        val state = viewModel.uiState.first { it.user == null }
        assertThat(state.nameEditor).isNull()
    }
}
