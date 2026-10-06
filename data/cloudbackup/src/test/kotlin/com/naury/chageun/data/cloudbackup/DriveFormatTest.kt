package com.naury.chageun.data.cloudbackup

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.domain.cloudbackup.CloudBackup
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import java.time.Instant
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DriveFormatTest {

    private val backup = CloudBackup(
        id = "b0",
        createdAt = Instant.parse("2026-10-06T05:30:00Z"),
        sizeBytes = 2_400_000,
        schemaVersion = 1,
        appVersion = "1.0.0",
        deviceModel = "Pixel 9 Pro Fold",
        vehicleLabel = "현대 " + "아반떼".repeat(30),
        recordCount = 12,
        photoCount = 3,
    )

    @Test
    fun appProperties_fitDriveLimit_andRoundTrip() {
        val properties = backup.toAppProperties()
        properties.keys().forEach { key ->
            assertThat((key + properties.getString(key)).toByteArray().size).isAtMost(124)
        }

        val file = JSONObject()
            .put("id", "drive-1")
            .put("size", "2400000")
            .put("createdTime", "2026-10-06T05:30:00.000Z")
            .put("appProperties", properties)
        val parsed = checkNotNull(file.toCloudBackup())

        assertThat(parsed.id).isEqualTo("drive-1")
        assertThat(parsed.createdAt).isEqualTo(backup.createdAt)
        assertThat(parsed.recordCount).isEqualTo(12)
        assertThat(parsed.vehicleLabel).startsWith("현대 아반떼")
        assertThat(JSONObject().put("id", "x").toCloudBackup()).isNull()
    }

    @Test
    fun errors_separateConnectionStorageAndNetwork() {
        val quota = """{"error":{"errors":[{"reason":"storageQuotaExceeded"}],"code":403}}"""

        assertThat(driveError(401, null)).isEqualTo(CloudBackupError.NotConnected)
        assertThat(driveError(403, driveErrorReason(quota))).isEqualTo(CloudBackupError.StorageFull)
        assertThat(driveError(403, "accessNotConfigured")).isEqualTo(CloudBackupError.Unavailable)
        assertThat(driveError(429, null)).isEqualTo(CloudBackupError.Network)
        assertThat(driveError(503, null)).isEqualTo(CloudBackupError.Network)
        assertThat(driveError(400, null)).isEqualTo(CloudBackupError.Unknown)
    }
}
