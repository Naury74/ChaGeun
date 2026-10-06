package com.naury.chageun.data.cloudbackup

import android.content.Intent
import com.naury.chageun.core.auth.DriveConnectRequest
import com.naury.chageun.core.auth.GoogleDriveAccess
import com.naury.chageun.core.domain.cloudbackup.CloudBackup
import com.naury.chageun.core.domain.cloudbackup.CloudBackupError
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeDriveAccess(connected: Boolean = true) : GoogleDriveAccess {
    override val isConnected = MutableStateFlow(connected)

    override suspend fun connect() = DriveConnectRequest.Connected.also { isConnected.value = true }

    override suspend fun completeConsent(data: Intent?) = true.also { isConnected.value = true }

    override suspend fun accessToken(): String? = if (isConnected.value) "token" else null

    override fun disconnect() {
        isConnected.value = false
    }
}

/** 드라이브처럼 올릴 때 새 ID를 붙이고, [failOn]으로 고른 요청을 네트워크 오류로 실패시킨다. */
internal class FakeRemote : CloudBackupRemote {
    val files = linkedMapOf<String, Pair<CloudBackup, String>>()
    val calls = mutableListOf<String>()
    var failOn: String? = null
    var failWith = CloudBackupError.Network
    private var nextId = 0

    private fun record(call: String) {
        calls += call
        if (failOn != null && call.startsWith(failOn!!)) throw CloudBackupException(failWith)
    }

    override suspend fun upload(token: String, backup: CloudBackup, file: File): CloudBackup {
        record("upload ${backup.id}")
        val stored = backup.copy(id = "drive-${nextId++}")
        files[stored.id] = stored to file.readText()
        return stored
    }

    override suspend fun list(token: String): List<CloudBackup> {
        record("list")
        return files.values.map { it.first }.sortedByDescending { it.createdAt }
    }

    override suspend fun download(token: String, backupId: String, target: File) {
        record("download $backupId")
        target.writeText(files.getValue(backupId).second)
    }

    override suspend fun delete(token: String, backupId: String) {
        record("delete $backupId")
        files -= backupId
    }
}
