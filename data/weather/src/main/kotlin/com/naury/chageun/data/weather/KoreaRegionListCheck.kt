package com.naury.chageun.data.weather

import com.naury.chageun.core.domain.weather.KoreaRegionCatalog
import com.naury.chageun.core.domain.weather.KoreaRegionRow
import com.naury.chageun.core.model.KoreaAreaLevel
import java.security.MessageDigest

/**
 * 내려받은 지역 목록을 쓰기 전에 검사한다. 하나라도 어긋나면 [IllegalArgumentException]을 던지고 가진 목록을 그대로 쓴다.
 * 오류 문구에는 지역 코드나 이름을 넣지 않는다(로그에 남을 수 있다).
 */
internal object KoreaRegionListCheck {

    /** 이보다 적으면 잘린 파일로 본다. 지금 목록은 3,836곳이다. */
    const val MIN_ROWS = 3_000

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /** [bytes]가 [manifest]가 말한 파일인지 보고, 읽어서 [validate]까지 마친 줄을 돌려준다. */
    fun verify(bytes: ByteArray, manifest: KoreaRegionManifest): List<KoreaRegionRow> {
        require(sha256(bytes) == manifest.sha256) { "지역 목록 sha256이 안내 파일과 다름" }
        val rows = parse(bytes)
        require(rows.size == manifest.rows) { "지역 목록 줄 수가 안내 파일과 다름: ${rows.size}/${manifest.rows}" }
        validate(rows)
        return rows
    }

    fun parse(bytes: ByteArray): List<KoreaRegionRow> =
        bytes.toString(Charsets.UTF_8).removePrefix(BOM).lineSequence().let(KoreaRegionCsv::parse)

    /** 좌표가 국토 안에 있고, 코드가 겹치지 않고, 모든 시·군·구와 읍·면·동의 윗 지역이 목록에 있는지. */
    fun validate(rows: List<KoreaRegionRow>) {
        require(rows.size >= MIN_ROWS) { "지역 목록이 너무 짧음: ${rows.size}" }
        val codes = rows.mapTo(HashSet(rows.size)) { it.code }
        require(codes.size == rows.size) { "지역 목록에 겹치는 코드가 있음" }
        rows.forEach { row ->
            require(CODE.matches(row.code) && row.name.isNotBlank()) { "지역 목록에 형식이 틀린 줄이 있음" }
            require(row.point.latitude in LATITUDE && row.point.longitude in LONGITUDE) { "지역 목록 좌표가 국토 밖" }
            require(KoreaRegionCatalog.parentCode(row.code)?.let { it in codes } ?: true) { "지역 목록에 윗 지역이 없는 줄" }
            require(KoreaRegionCatalog.levelOf(row.code) != KoreaAreaLevel.Province || !row.shortName.isNullOrBlank()) {
                "지역 목록에 짧은 이름 없는 시·도가 있음"
            }
        }
    }

    private const val BOM = "\uFEFF"
    private val CODE = Regex("\\d{10}")

    // 남쪽 마라도(33.1)부터 북쪽 강원 고성·연천(38.5), 서쪽 백령도(124.7)부터 동쪽 독도(131.9)까지.
    private val LATITUDE = 33.0..38.7
    private val LONGITUDE = 124.5..132.0
}
