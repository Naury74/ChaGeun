package com.naury.chageun.data.weather

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.common.logging.LogField
import com.naury.chageun.core.domain.weather.KoreaRegionCheck
import com.naury.chageun.core.domain.weather.KoreaRegionListInfo
import com.naury.chageun.core.testing.FakeWeatherSettingsRepository
import com.naury.chageun.data.weather.RegionListFixtures.bundledBytes
import com.naury.chageun.data.weather.RegionListFixtures.manifest
import com.naury.chageun.data.weather.RegionListFixtures.renamedBytes
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KoreaRegionListUpdaterTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store by lazy { KoreaRegionListStore(File(folder.root, "regions")) }
    private val repository by lazy { AssetKoreaRegionRepository(context, dispatcher, store) }
    private val remote = FakeRemote()
    private val settings = FakeWeatherSettingsRepository()
    private val clock = MovableClock(Instant.parse("2026-10-12T00:00:00Z"))
    private val logged = mutableListOf<String>()
    private val logger = object : AppLogger {
        override fun debug(event: String, vararg fields: LogField) = record(event, fields)

        override fun warn(event: String, vararg fields: LogField, error: Throwable?) = record(event, fields)

        override fun error(event: String, vararg fields: LogField, error: Throwable?) = record(event, fields)

        private fun record(event: String, fields: Array<out LogField>) {
            logged += event
            logged += fields.map { it.value }
        }
    }
    private val updater by lazy {
        KoreaRegionListUpdater(remote, repository, settings, clock, logger, dispatcher)
    }

    @Test
    fun withoutDownload_usesBundledList() = scope.runTest {
        assertThat(repository.listInfo.first()).isEqualTo(KoreaRegionListInfo("2026-07"))
        assertThat(repository.catalog().find("1168064000")?.label).isEqualTo("강남구 역삼1동")
    }

    @Test
    fun newerList_isVerifiedInstalledAndUsedRightAway() = scope.runTest {
        assertThat(repository.catalog().find("1168064100")).isNull()
        remote.publish(manifest(), renamedBytes)

        assertThat(updater.checkNow()).isEqualTo(KoreaRegionCheck.Updated("2026-10"))

        assertThat(repository.catalog().find("1168064100")?.label).isEqualTo("강남구 역삼1동")
        assertThat(repository.listInfo.first()).isEqualTo(KoreaRegionListInfo("2026-10", manifest().source))
        assertThat(settings.appliedRegionListVersions).contains("2026-10")
        // 새 판이 없으면 목록 파일을 다시 받지 않는다.
        assertThat(updater.checkNow()).isEqualTo(KoreaRegionCheck.UpToDate)
        assertThat(remote.fileRequests).isEqualTo(1)
        // 다음 실행(새 저장소)에서도 받은 목록을 쓴다.
        val nextLaunch = AssetKoreaRegionRepository(context, dispatcher, store)
        assertThat(nextLaunch.catalog().find("1168064100")).isNotNull()
    }

    @Test
    fun sameOrOlderVersion_andHigherSchema_areNotDownloaded() = scope.runTest {
        remote.publish(manifest(version = "2026-07"), renamedBytes)
        assertThat(updater.checkNow()).isEqualTo(KoreaRegionCheck.UpToDate)

        remote.manifestText = """{ "schema": 2, "version": "2027-01" }"""
        assertThat(updater.checkNow()).isEqualTo(KoreaRegionCheck.UpToDate)

        assertThat(remote.fileRequests).isEqualTo(0)
        assertThat(repository.listInfo.first().isBundled).isTrue()
    }

    @Test
    fun badDownloads_keepCurrentList_andLogNoRegionCodes() = scope.runTest {
        remote.publish(manifest(), renamedBytes)
        remote.fileBytes = bundledBytes
        assertThat(updater.checkNow()).isEqualTo(KoreaRegionCheck.Failed(isTemporary = false))

        remote.publish(manifest(rows = 10), renamedBytes)
        assertThat(updater.checkNow()).isEqualTo(KoreaRegionCheck.Failed(isTemporary = false))

        remote.manifestText = "<html>"
        assertThat(updater.checkNow()).isEqualTo(KoreaRegionCheck.Failed(isTemporary = false))

        remote.failure = IOException("offline")
        assertThat(updater.checkNow()).isEqualTo(KoreaRegionCheck.Failed(isTemporary = true))

        assertThat(store.installed()).isNull()
        assertThat(repository.catalog().find("1168064000")).isNotNull()
        assertThat(settings.appliedRegionListVersions).isEmpty()
        assertThat(logged.filter { it.matches(Regex("\\d{10}")) }).isEmpty()
    }

    @Test
    fun corruptedDownload_fallsBackToBundledList() = scope.runTest {
        remote.publish(manifest(), renamedBytes)
        updater.checkNow()
        File(folder.root, "regions/regions-2026-10.csv").writeText("broken")

        val nextLaunch = AssetKoreaRegionRepository(context, dispatcher, store)

        assertThat(nextLaunch.listInfo.first().isBundled).isTrue()
        assertThat(nextLaunch.catalog().find("1168064000")).isNotNull()
    }

    @Test
    fun checkIfStale_asksAtMostOnceAWeek() = scope.runTest {
        remote.publish(manifest(version = "2026-07"), renamedBytes)

        updater.checkIfStale()
        updater.checkIfStale()
        assertThat(remote.manifestRequests).isEqualTo(1)

        clock.advance(Duration.ofDays(6))
        updater.checkIfStale()
        assertThat(remote.manifestRequests).isEqualTo(1)

        clock.advance(Duration.ofDays(2))
        updater.checkIfStale()
        assertThat(remote.manifestRequests).isEqualTo(2)

        // 시계를 되돌려 마지막 확인이 미래가 되면 다시 확인한다.
        clock.advance(Duration.ofDays(-30))
        updater.checkIfStale()
        assertThat(remote.manifestRequests).isEqualTo(3)
    }

    private class FakeRemote : KoreaRegionRemote {
        var manifestText: String = ""
        var fileBytes: ByteArray = ByteArray(0)
        var failure: IOException? = null
        var manifestRequests = 0
        var fileRequests = 0

        fun publish(manifest: KoreaRegionManifest, bytes: ByteArray) {
            manifestText = manifest.toJson()
            fileBytes = bytes
            failure = null
        }

        override suspend fun manifest(): String {
            manifestRequests++
            failure?.let { throw it }
            return manifestText
        }

        override suspend fun file(name: String): ByteArray {
            fileRequests++
            failure?.let { throw it }
            return fileBytes
        }
    }

    private class MovableClock(private var now: Instant) : Clock() {
        fun advance(by: Duration) {
            now = now.plus(by)
        }

        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this
    }
}
