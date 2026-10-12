package com.naury.chageun.data.weather

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.data.weather.RegionListFixtures.bundledBytes
import com.naury.chageun.data.weather.RegionListFixtures.manifest
import com.naury.chageun.data.weather.RegionListFixtures.renamedBytes
import java.io.File
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class KoreaRegionListStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dir: File get() = File(folder.root, "regions")
    private val store by lazy { KoreaRegionListStore(dir) }

    @Test
    fun nothingInstalled_atFirst() {
        assertThat(store.installed()).isNull()
        assertThat(store.lastCheckedAt()).isNull()
    }

    @Test
    fun installsListAndManifest_andReplacesOlderList() {
        val october = manifest()
        store.install(october, renamedBytes)
        assertThat(store.installed()?.manifest).isEqualTo(october)
        assertThat(store.installed()?.bytes).isEqualTo(renamedBytes)

        val january = manifest(bytes = bundledBytes, version = "2027-01", renamedCodes = emptyMap())
        store.install(january, bundledBytes)

        assertThat(store.installed()?.manifest).isEqualTo(january)
        assertThat(dir.list()?.toList()).containsExactly("regions.json", "regions-2027-01.csv")
    }

    @Test
    fun tamperedOrHalfWrittenFiles_areNotUsed() {
        store.install(manifest(), renamedBytes)
        File(dir, "regions-2026-10.csv").writeText("1100000000,서울특별시,37.5,126.9,서울")
        assertThat(store.installed()).isNull()

        // 안내 파일을 바꾸기 전에 앱이 꺼진 경우: 새 목록 파일과 임시 파일만 남고 안내 파일은 예전 그대로다.
        store.install(manifest(), renamedBytes)
        File(dir, "regions-2027-01.csv").writeBytes(bundledBytes)
        File(dir, "regions.json.tmp").writeText("{")
        assertThat(store.installed()?.manifest?.version).isEqualTo("2026-10")

        File(dir, "regions.json").writeText("{ broken")
        assertThat(store.installed()).isNull()
    }

    @Test
    fun clear_keepsLastCheck() {
        val at = Instant.parse("2026-10-12T00:00:00Z")
        store.install(manifest(), renamedBytes)
        store.markChecked(at)

        store.clear()

        assertThat(store.installed()).isNull()
        assertThat(store.lastCheckedAt()).isEqualTo(at)
    }
}
