package com.naury.chageun.feature.account

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.backup.ImportPreview
import com.naury.chageun.core.domain.backup.LocalDataSummary
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import com.naury.chageun.core.testing.FakeAnalyticsTracker
import com.naury.chageun.core.testing.FakeAuthRepository
import com.naury.chageun.core.testing.FakeBackupRepository
import com.naury.chageun.core.testing.FakeCloudBackupRepository
import com.naury.chageun.core.testing.FakeSettingsRepository
import com.naury.chageun.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CloudBackupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val cloud = FakeCloudBackupRepository()
    private val local = FakeBackupRepository()
    private val analytics = FakeAnalyticsTracker()
    private val settings = FakeSettingsRepository()

    private fun viewModel() = CloudBackupViewModel(auth, cloud, local, settings, analytics)

    @Test
    fun autoBackupSwitch_isStoredInSettings() = runTest {
        val viewModel = viewModel()

        viewModel.setAutoBackupEnabled(true)

        assertThat(viewModel.uiState.first { it.isAutoBackupEnabled }.isAutoBackupEnabled).isTrue()
        assertThat(settings.settings.value.isCloudAutoBackupEnabled).isTrue()
    }

    @Test
    fun loadsAfterSignIn_backsUp_andClearsOnSignOut() = runTest {
        val viewModel = viewModel()
        assertThat(viewModel.uiState.value.isLoaded).isFalse()

        auth.signInWithGoogle("token")
        viewModel.uiState.first { it.isLoaded }
        viewModel.backUpNow()

        val backedUp = viewModel.uiState.first { it.backups.size == 1 }
        assertThat(backedUp.notice).isEqualTo(BackupNotice.BackedUp)
        assertThat(backedUp.lastBackup?.id).isEqualTo("b0")
        assertThat(analytics.events).containsExactly(AnalyticsEvent.CloudBackupCreated)

        auth.signOut()
        assertThat(viewModel.uiState.first { !it.isLoaded }.backups).isEmpty()
    }

    @Test
    fun backupFailure_isReported() = runTest {
        auth.signInWithGoogle("token")
        val viewModel = viewModel()
        viewModel.uiState.first { it.isLoaded }

        cloud.nextError = CloudBackupError.Network
        viewModel.backUpNow()

        assertThat(viewModel.uiState.first { it.notice != null }.notice)
            .isEqualTo(BackupNotice.Failed(CloudBackupError.Network))
        assertThat(viewModel.uiState.value.isBackingUp).isFalse()
    }

    @Test
    fun restore_downloadsPreviewsThenReplaces() = runTest {
        auth.signInWithGoogle("token")
        cloud.backups += FakeCloudBackupRepository.backup("b9", java.time.Instant.parse("2026-10-01T00:00:00Z"))
        local.importPreview = ImportPreview.Ready(LocalDataSummary(1, 10, 2), LocalDataSummary(1, 12, 3))
        val viewModel = viewModel()
        val backup = viewModel.uiState.first { it.backups.isNotEmpty() }.backups.single()

        viewModel.select(backup)
        viewModel.startRestore(backup)
        val confirming = viewModel.uiState.first { it.restore is RestoreStep.Confirming }
        assertThat(confirming.selected).isNull()
        assertThat((confirming.restore as RestoreStep.Confirming).preview.incoming.records).isEqualTo(10)

        viewModel.confirmRestore()
        assertThat(viewModel.uiState.first { it.restore == null }.notice).isEqualTo(BackupNotice.Restored)
        assertThat(local.importedFrom).containsExactly("file:///cache/b9.zip")
        assertThat(analytics.events).containsExactly(AnalyticsEvent.CloudBackupRestored)
    }

    @Test
    fun newerBackup_asksForUpdate_withoutReplacing() = runTest {
        auth.signInWithGoogle("token")
        cloud.backups += FakeCloudBackupRepository.backup("b9", java.time.Instant.parse("2026-10-01T00:00:00Z"))
        local.importPreview = ImportPreview.UnsupportedVersion(2)
        val viewModel = viewModel()
        val backup = viewModel.uiState.first { it.backups.isNotEmpty() }.backups.single()

        viewModel.startRestore(backup)

        assertThat(viewModel.uiState.first { it.notice != null }.notice).isEqualTo(BackupNotice.NeedsUpdate)
        assertThat(local.importedFrom).isEmpty()
    }

    @Test
    fun delete_confirmsFirst_thenRemovesFromList() = runTest {
        auth.signInWithGoogle("token")
        cloud.backups += FakeCloudBackupRepository.backup("b9", java.time.Instant.parse("2026-10-01T00:00:00Z"))
        val viewModel = viewModel()
        val backup = viewModel.uiState.first { it.backups.isNotEmpty() }.backups.single()

        viewModel.requestDelete(backup)
        assertThat(viewModel.uiState.value.pendingDelete).isEqualTo(backup)
        viewModel.confirmDelete()

        val deleted = viewModel.uiState.first { it.notice == BackupNotice.Deleted }
        assertThat(deleted.backups).isEmpty()
        assertThat(cloud.backups).isEmpty()
    }
}
