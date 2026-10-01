package com.naury.chageun.core.domain.backup

data class LocalDataSummary(val vehicles: Int, val records: Int, val photos: Int)

interface BackupRepository {
    suspend fun summary(): LocalDataSummary

    /**
     * Writes a ZIP with `data.json` and attachment images to [destinationUri], a document the user picked.
     * @return false when the destination could not be written.
     */
    suspend fun export(destinationUri: String): Boolean

    /** Removes every vehicle, record, reminder state and attachment file; user settings are kept. */
    suspend fun deleteAll()
}
