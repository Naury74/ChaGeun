package com.naury.chageun.data.weather

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.data.weather.RegionListFixtures.bundledBytes
import com.naury.chageun.data.weather.RegionListFixtures.bundledRowCount
import com.naury.chageun.data.weather.RegionListFixtures.manifest
import com.naury.chageun.data.weather.RegionListFixtures.renamedBytes
import org.junit.Assert.assertThrows
import org.junit.Test

class KoreaRegionListCheckTest {

    @Test
    fun acceptsWellFormedList() {
        val rows = KoreaRegionListCheck.verify(renamedBytes, manifest())

        assertThat(rows).hasSize(bundledRowCount)
        assertThat(rows.map { it.code }).contains("1168064100")
    }

    @Test
    fun acceptsByteOrderMark() {
        val withBom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + bundledBytes

        assertThat(KoreaRegionListCheck.verify(withBom, manifest(bytes = withBom))).hasSize(bundledRowCount)
    }

    @Test
    fun rejectsShaOrRowCountMismatch() {
        assertThrows(IllegalArgumentException::class.java) {
            KoreaRegionListCheck.verify(renamedBytes, manifest(sha256 = KoreaRegionListCheck.sha256(bundledBytes)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            KoreaRegionListCheck.verify(renamedBytes, manifest(rows = bundledRowCount + 1))
        }
    }

    @Test
    fun rejectsInvalidLists() {
        val text = bundledBytes.toString(Charsets.UTF_8)
        listOf(
            // 국토 밖 좌표
            text.replace("1168064000,역삼1동,37.4926,127.0351", "1168064000,역삼1동,0.0,0.0"),
            // 겹치는 코드
            text.replace("1168065000,역삼2동", "1168064000,역삼2동"),
            // 윗 지역이 없는 동
            text.replace("1168064000,역삼1동", "9968064000,역삼1동"),
            // 숫자가 아닌 좌표
            text.replace("1168064000,역삼1동,37.4926", "1168064000,역삼1동,abc"),
            // 칸 수가 틀린 줄
            text + "\n1168099999,새동\n",
            // 잘린 파일
            text.lines().take(1_000).joinToString("\n"),
            // HTML 오류 페이지
            "<html><body>404</body></html>",
        ).forEach { broken ->
            val bytes = broken.toByteArray()
            val rows = runCatching { KoreaRegionListCheck.parse(bytes).size }.getOrDefault(bundledRowCount)
            assertThrows(IllegalArgumentException::class.java) {
                KoreaRegionListCheck.verify(bytes, manifest(bytes = bytes, rows = rows))
            }
        }
    }
}
