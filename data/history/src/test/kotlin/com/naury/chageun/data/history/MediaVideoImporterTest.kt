package com.naury.chageun.data.history

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaVideoImporterTest {

    private val importer = MediaVideoImporter(ApplicationProvider.getApplicationContext())

    @Test
    fun isVideo_followsTheFileExtension_forAppFiles() {
        assertThat(importer.isVideo("file:///data/cache/photo_input/edited.mp4")).isTrue()
        assertThat(importer.isVideo("/data/files/attachments/album_1.MP4")).isTrue()
        assertThat(importer.isVideo("file:///data/cache/photo_input/edited.jpg")).isFalse()
        assertThat(importer.isVideo("content://media/picker/0/unknown")).isFalse()
    }

    @Test
    fun parseVideoDate_readsTheDay_andIgnoresEmptyContainerDates() {
        assertThat(parseVideoDate("20250815T093012.000Z")).isEqualTo(LocalDate.of(2025, 8, 15))
        assertThat(parseVideoDate("19040101T000000.000Z")).isNull()
        assertThat(parseVideoDate("19700101T000000.000Z")).isNull()
        assertThat(parseVideoDate("not a date")).isNull()
    }

    @Test
    fun import_unreadableSource_failsWithoutLeavingFiles() {
        val directory = Files.createTempDirectory("video").toFile()
        val source = File(directory, "broken.mp4").apply { writeText("not a video") }
        val target = File(directory, "album_1.mp4")
        val poster = File(directory, "album_1_thumb.jpg")

        val result = importer.import(source.toURI().toString(), target, poster, Long.MAX_VALUE, Long.MAX_VALUE)

        assertThat(result).isEqualTo(VideoImport.Failed)
        assertThat(target.exists()).isFalse()
        assertThat(poster.exists()).isFalse()
        directory.deleteRecursively()
    }
}
