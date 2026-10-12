package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.backup.BackupRepository
import com.naury.chageun.core.domain.backup.ImportPreview
import com.naury.chageun.core.domain.backup.LocalDataSummary

class FakeBackupRepository : BackupRepository {
    var summary = LocalDataSummary(vehicles = 1, records = 0, photos = 0)
    var exportSucceeds = true
    val exportedTo = mutableListOf<String>()
    var deleteCount = 0

    override suspend fun summary() = summary

    /** 마지막 내보내기에 동영상을 넣었는지. */
    var exportedVideos: Boolean? = null

    override suspend fun export(destinationUri: String, includeVideos: Boolean): Boolean {
        exportedTo += destinationUri
        exportedVideos = includeVideos
        return exportSucceeds
    }

    var importPreview: ImportPreview = ImportPreview.Invalid
    val importedFrom = mutableListOf<String>()

    override suspend fun previewImport(sourceUri: String) = importPreview

    override suspend fun import(sourceUri: String): Boolean {
        importedFrom += sourceUri
        return importPreview is ImportPreview.Ready
    }

    override suspend fun deleteAll() {
        deleteCount++
        summary = LocalDataSummary(0, 0, 0)
    }
}
