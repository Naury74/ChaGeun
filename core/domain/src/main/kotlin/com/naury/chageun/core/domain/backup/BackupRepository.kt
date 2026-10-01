package com.naury.chageun.core.domain.backup

data class LocalDataSummary(val vehicles: Int, val records: Int, val photos: Int)

sealed interface ImportPreview {
    /** [current] is what will be replaced on this device. */
    data class Ready(val incoming: LocalDataSummary, val current: LocalDataSummary) : ImportPreview

    data class UnsupportedVersion(val version: Int) : ImportPreview

    data object Invalid : ImportPreview
}

interface BackupRepository {
    suspend fun summary(): LocalDataSummary

    /**
     * Writes a ZIP with `data.json` and attachment images to [destinationUri], a document the user picked.
     * @return false when the destination could not be written.
     */
    suspend fun export(destinationUri: String): Boolean

    suspend fun previewImport(sourceUri: String): ImportPreview

    /**
     * Replaces all local data with the archive at [sourceUri]. Database rows are swapped in one transaction and
     * attachment files only after it commits, so a failure leaves the existing data untouched.
     * @return false when nothing was changed.
     */
    suspend fun import(sourceUri: String): Boolean

    /** Removes every vehicle, record, reminder state and attachment file; user settings are kept. */
    suspend fun deleteAll()
}
