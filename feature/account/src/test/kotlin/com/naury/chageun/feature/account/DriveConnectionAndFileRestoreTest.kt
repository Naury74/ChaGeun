package com.naury.chageun.feature.account

import android.content.Intent
import android.content.IntentSender
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.auth.DriveConnectRequest
import com.naury.chageun.core.domain.backup.ImportPreview
import com.naury.chageun.core.domain.backup.LocalDataSummary
import com.naury.chageun.core.testing.FakeBackupRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DriveConnectionAndFileRestoreTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun connect_showsConsent_thenConnectsOnGrant() = runTest {
        val drive = FakeDriveAccess()
        val sender = IntentSender::class.java.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
        drive.nextRequest = DriveConnectRequest.NeedsConsent(sender)
        val viewModel = DriveConnectionViewModel(drive)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.connect()
        assertThat(viewModel.uiState.first { it.consent != null }.isConnecting).isTrue()
        viewModel.onConsentShown()

        viewModel.onConsentResult(Intent())
        assertThat(viewModel.uiState.first { it.isConnected }.isConnecting).isFalse()

        viewModel.disconnect()
        assertThat(viewModel.uiState.first { !it.isConnected }.hasFailed).isFalse()
    }

    @Test
    fun closingConsent_isSilent_andDeniedIsReported() = runTest {
        val drive = FakeDriveAccess()
        val viewModel = DriveConnectionViewModel(drive)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.onConsentResult(null)
        assertThat(viewModel.uiState.first { !it.isConnecting }.hasFailed).isFalse()

        drive.grantConsent = false
        viewModel.onConsentResult(Intent())
        assertThat(viewModel.uiState.first { it.hasFailed }.isConnected).isFalse()
    }

    @Test
    fun fileRestore_previewsThenReplaces() = runTest {
        val local = FakeBackupRepository().apply {
            importPreview = ImportPreview.Ready(LocalDataSummary(1, 10, 2), LocalDataSummary(0, 0, 0))
        }
        val viewModel = FileRestoreViewModel(local)

        viewModel.preview("content://backup.zip")
        assertThat(viewModel.uiState.first { it.preview != null }.preview?.incoming?.records).isEqualTo(10)

        viewModel.confirm()
        assertThat(viewModel.uiState.first { it.notice != null }.notice).isEqualTo(BackupNotice.Restored)
        assertThat(local.importedFrom).containsExactly("content://backup.zip")
    }

    @Test
    fun fileRestore_rejectsOtherFiles() = runTest {
        val viewModel = FileRestoreViewModel(FakeBackupRepository())

        viewModel.preview("content://photo.jpg")

        assertThat(viewModel.uiState.first { it.notice != null }.notice).isEqualTo(BackupNotice.RestoreFailed)
    }
}
