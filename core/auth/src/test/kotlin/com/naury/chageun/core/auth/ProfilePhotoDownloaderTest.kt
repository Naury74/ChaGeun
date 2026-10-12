package com.naury.chageun.core.auth

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.common.logging.formatLogLine
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class ProfilePhotoDownloaderTest {

    @Test
    fun sizedUrl_asksGoogleForTwiceTheAvatarSize() {
        assertThat(sizedPhotoUrl("https://lh3.googleusercontent.com/a/abc=s96-c"))
            .isEqualTo("https://lh3.googleusercontent.com/a/abc=s192-c")
        assertThat(sizedPhotoUrl("https://lh3.googleusercontent.com/a/abc=s96"))
            .isEqualTo("https://lh3.googleusercontent.com/a/abc=s192-c")
        assertThat(sizedPhotoUrl("https://lh3.googleusercontent.com/a/abc"))
            .isEqualTo("https://lh3.googleusercontent.com/a/abc=s192-c")
    }

    @Test
    fun sizedUrl_leavesOtherHostsAlone_andRejectsPlainHttp() {
        assertThat(sizedPhotoUrl("https://example.com/me.jpg")).isEqualTo("https://example.com/me.jpg")
        assertThat(sizedPhotoUrl("http://lh3.googleusercontent.com/a/abc=s96-c")).isNull()
        assertThat(sizedPhotoUrl("not a url")).isNull()
    }

    @Test
    fun readAtMost_rejectsOversizedResponses() {
        assertThat(ByteArrayInputStream(ByteArray(10)).readAtMost(10)).hasLength(10)
        assertThrows(IOException::class.java) { ByteArrayInputStream(ByteArray(11)).readAtMost(10) }
    }

    @Test
    fun failedDownload_returnsNull_andLogsNoUrl() = runTest {
        val lines = mutableListOf<String>()
        val logger = object : AppLogger {
            override fun debug(event: String, vararg fields: LogField) {
                lines += formatLogLine(event, fields, null)
            }

            override fun warn(event: String, vararg fields: LogField, error: Throwable?) {
                lines += formatLogLine(event, fields, error)
            }

            override fun error(event: String, vararg fields: LogField, error: Throwable?) {
                lines += formatLogLine(event, fields, error)
            }
        }
        val downloader = HttpProfilePhotoDownloader(logger, StandardTestDispatcher(testScheduler))

        // 1번 포트는 열려 있지 않아 바로 연결이 거부된다. 바깥 네트워크는 쓰지 않는다.
        val result = downloader.download("https://localhost:1/naury-secret-photo=s96-c")

        assertThat(result).isNull()
        assertThat(lines).isNotEmpty()
        lines.forEach {
            assertThat(it).doesNotContain("naury")
            assertThat(it).doesNotContain("localhost")
        }
    }
}
