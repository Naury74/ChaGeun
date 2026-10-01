package com.naury.chageun.core.model

import java.time.Instant

/** A photo or receipt stored in app-private storage. Paths are absolute and never leave the device. */
data class Attachment(
    val id: String,
    val owner: RecordRef,
    val filePath: String,
    val thumbnailPath: String,
    val createdAt: Instant,
)
