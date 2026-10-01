package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.backup.BackupRepository
import com.naury.chageun.core.domain.backup.LocalDataSummary

class FakeBackupRepository : BackupRepository {
    var summary = LocalDataSummary(vehicles = 1, records = 0, photos = 0)
    var exportSucceeds = true
    val exportedTo = mutableListOf<String>()
    var deleteCount = 0

    override suspend fun summary() = summary

    override suspend fun export(destinationUri: String): Boolean {
        exportedTo += destinationUri
        return exportSucceeds
    }

    override suspend fun deleteAll() {
        deleteCount++
        summary = LocalDataSummary(0, 0, 0)
    }
}
