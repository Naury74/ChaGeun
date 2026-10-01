package com.naury.chageun.data.backup

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal sealed interface ArchiveContent {
    data class Valid(val document: BackupDocument) : ArchiveContent

    data class UnsupportedVersion(val version: Int) : ArchiveContent

    data object Invalid : ArchiveContent
}

internal object BackupArchiveReader {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Reads `data.json` and, when [extractTo] is given, copies `attachments/` images into it.
     * Entry names are reduced to a bare file name so a crafted archive cannot write outside the target.
     */
    fun read(input: InputStream, extractTo: File? = null): ArchiveContent {
        var content: ArchiveContent = ArchiveContent.Invalid
        ZipInputStream(input.buffered()).use { zip ->
            generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
                when {
                    entry.name == BackupArchiveWriter.DATA_ENTRY ->
                        content =
                            parse(zip.readBytes().toString(Charsets.UTF_8))
                    extractTo != null && entry.name.startsWith(BackupArchiveWriter.ATTACHMENT_PREFIX) ->
                        extract(zip, File(entry.name).name, extractTo)
                }
            }
        }
        return content
    }

    private fun parse(text: String): ArchiveContent {
        val version = runCatching { json.decodeFromString(VersionProbe.serializer(), text) }.getOrNull()?.schemaVersion
        return when (version) {
            null -> ArchiveContent.Invalid
            BackupDocument.SCHEMA_VERSION ->
                runCatching { json.decodeFromString(BackupDocument.serializer(), text) }
                    .fold(onSuccess = ArchiveContent::Valid, onFailure = { ArchiveContent.Invalid })
            else -> ArchiveContent.UnsupportedVersion(version)
        }
    }

    private fun extract(zip: ZipInputStream, name: String, directory: File) {
        if (name.isBlank() || name == "." || name == "..") return
        directory.mkdirs()
        File(directory, name).outputStream().use { zip.copyTo(it) }
    }
}

@Serializable
private data class VersionProbe(@SerialName("schema_version") val schemaVersion: Int)
