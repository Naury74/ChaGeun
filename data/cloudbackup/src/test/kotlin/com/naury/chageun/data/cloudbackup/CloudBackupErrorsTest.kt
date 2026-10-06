package com.naury.chageun.data.cloudbackup

import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import org.junit.Test

class CloudBackupErrorsTest {

    @Test
    fun separatesNetworkFromConsoleSetup() {
        assertThat(FirebaseNetworkException("offline").toCloudBackupError()).isEqualTo(CloudBackupError.Network)
        assertThat(firestore(FirebaseFirestoreException.Code.UNAVAILABLE)).isEqualTo(CloudBackupError.Network)
        assertThat(firestore(FirebaseFirestoreException.Code.PERMISSION_DENIED)).isEqualTo(CloudBackupError.Unavailable)
        assertThat(firestore(FirebaseFirestoreException.Code.NOT_FOUND)).isEqualTo(CloudBackupError.Unavailable)
        assertThat(firestore(FirebaseFirestoreException.Code.INTERNAL)).isEqualTo(CloudBackupError.Unknown)
        assertThat(IllegalStateException().toCloudBackupError()).isEqualTo(CloudBackupError.Unknown)
    }

    private fun firestore(code: FirebaseFirestoreException.Code) =
        FirebaseFirestoreException("failed", code).toCloudBackupError()
}
