package com.naury.chageun.data.weather

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 웹(naurylab.com)에 올린 지역 목록 안내 파일. 새 목록 파일 이름과 sha256, 줄 수, 출처, 바뀐 코드 표를 담는다.
 * 내려받아 설치한 뒤에는 같은 내용을 앱 안에 그대로 두고, 지금 쓰는 목록의 판과 출처로 쓴다.
 */
@Serializable
internal data class KoreaRegionManifest(
    val schema: Int,
    val version: String,
    val file: String,
    val sha256: String,
    val rows: Int,
    val source: String,
    val renamedCodes: Map<String, String> = emptyMap(),
) {
    /** 판은 "2026-07"처럼 연-월이라 글자 순서로 비교하면 날짜 순서와 같다. */
    fun isNewerThan(version: String): Boolean = this.version > version

    private fun isWellFormed(): Boolean = schema == SCHEMA &&
        VERSION.matches(version) &&
        FILE.matches(file) &&
        SHA256.matches(sha256) &&
        rows > 0 &&
        source.isNotBlank() &&
        source.length <= MAX_SOURCE_LENGTH &&
        renamedCodes.all { (old, new) -> CODE.matches(old) && CODE.matches(new) }

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        const val SCHEMA = 1
        private const val MAX_SOURCE_LENGTH = 300
        private val VERSION = Regex("\\d{4}-\\d{2}")

        // 파일 이름에 경로가 섞이면 다른 곳을 받거나 앱 밖에 쓰게 된다. 이름만 허락한다.
        private val FILE = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}\\.csv")
        private val SHA256 = Regex("[0-9a-f]{64}")
        private val CODE = Regex("\\d{10}")

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * 모르는(더 높은) [schema]면 칸이 달라졌을 수 있어 나머지를 읽지 않고 null을 돌려준다. 그런 목록은 앱을 새로
         * 받은 뒤에 쓴다. 읽을 수 없거나 형식이 맞지 않으면 [IllegalArgumentException]을 던진다
         * (직렬화 오류 SerializationException도 IllegalArgumentException이다).
         */
        fun parse(text: String): KoreaRegionManifest? {
            val root = json.parseToJsonElement(text).jsonObject
            val schema = requireNotNull(root["schema"]?.jsonPrimitive?.intOrNull) { "지역 목록 안내 파일에 schema가 없음" }
            if (schema > SCHEMA) return null
            return json.decodeFromJsonElement(serializer(), root).also {
                require(it.isWellFormed()) { "지역 목록 안내 파일 형식이 맞지 않음" }
            }
        }
    }
}
