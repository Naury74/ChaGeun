package com.naury.chageun.data.backup

import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.Json

/** Writes the export ZIP: `data.json` at the root and images under `attachments/`. Missing image files are skipped. */
internal object BackupArchiveWriter {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun write(output: OutputStream, document: BackupDocument, attachmentDirectory: File) {
        ZipOutputStream(output.buffered()).use { zip ->
            zip.writeEntry(DATA_ENTRY) {
                write(json.encodeToString(BackupDocument.serializer(), document).toByteArray(Charsets.UTF_8))
            }
            document.attachments
                .map { File(attachmentDirectory, it.file) }
                .filter(File::isFile)
                .forEach { file ->
                    zip.writeEntry("$ATTACHMENT_PREFIX${file.name}") { file.inputStream().use { it.copyTo(this) } }
                }
        }
    }

    private fun ZipOutputStream.writeEntry(name: String, body: ZipOutputStream.() -> Unit) {
        putNextEntry(ZipEntry(name))
        body()
        closeEntry()
    }

    const val DATA_ENTRY = "data.json"
    const val ATTACHMENT_PREFIX = "attachments/"
}
