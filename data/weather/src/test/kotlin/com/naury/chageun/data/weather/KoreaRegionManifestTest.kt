package com.naury.chageun.data.weather

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class KoreaRegionManifestTest {

    private val sha = "a".repeat(64)

    private fun json(
        schema: String = "1",
        version: String = "2026-10",
        file: String = "regions-2026-10.csv",
        extra: String = "",
    ) = """
        { "schema": $schema, "version": "$version", "file": "$file", "sha256": "$sha", "rows": 3836,
          "source": "기상청 단기예보 조회서비스 격자·위경도 (공공데이터포털 15084084), 2026-07-01, 저작자표시",
          "renamedCodes": { "1168064000": "1168064100" }$extra }
    """.trimIndent()

    @Test
    fun parsesSchemaOne_andIgnoresUnknownFields() {
        val manifest = KoreaRegionManifest.parse(json(extra = """, "notes": "새 칸""""))

        assertThat(manifest?.version).isEqualTo("2026-10")
        assertThat(manifest?.file).isEqualTo("regions-2026-10.csv")
        assertThat(manifest?.rows).isEqualTo(3836)
        assertThat(manifest?.renamedCodes).containsExactly("1168064000", "1168064100")
    }

    @Test
    fun higherSchema_isIgnored() {
        assertThat(KoreaRegionManifest.parse("""{ "schema": 2, "whatever": [] }""")).isNull()
    }

    @Test
    fun brokenOrUnsafeManifests_areRejected() {
        listOf(
            "not json",
            """{ "version": "2026-10" }""",
            json(schema = "0"),
            json(version = "2026-1"),
            json(version = "latest"),
            json(file = "../../shared_prefs/x.csv"),
            json(file = "regions.json"),
            json().replace("\"rows\": 3836", "\"rows\": -1"),
            json().replace(sha, "XYZ"),
            json().replace("\"1168064100\"", "\"abc\""),
        ).forEach { text ->
            assertThrows(text, IllegalArgumentException::class.java) { KoreaRegionManifest.parse(text) }
        }
    }

    @Test
    fun versionsCompareByYearAndMonth() {
        val manifest = checkNotNull(KoreaRegionManifest.parse(json()))

        assertThat(manifest.isNewerThan("2026-07")).isTrue()
        assertThat(manifest.isNewerThan("2026-10")).isFalse()
        assertThat(manifest.isNewerThan("2027-01")).isFalse()
        assertThat(manifest.copy(version = "2027-01").isNewerThan("2026-12")).isTrue()
    }

    @Test
    fun roundTripsThroughJson() {
        val manifest = checkNotNull(KoreaRegionManifest.parse(json()))

        assertThat(KoreaRegionManifest.parse(manifest.toJson())).isEqualTo(manifest)
    }
}
