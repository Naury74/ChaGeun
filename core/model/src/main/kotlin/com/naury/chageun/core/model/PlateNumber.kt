package com.naury.chageun.core.model

sealed interface PlateParseResult {
    data class Valid(val plate: PlateNumber) : PlateParseResult

    data object Empty : PlateParseResult

    /** 입력에 아직 낱자모가 남아 있다. 즉 사용자가 글자를 조합하는 중이므로 호출부에서 오류를 표시하지 않아야 한다. */
    data object Composing : PlateParseResult

    data object InvalidFormat : PlateParseResult

    data object UnsupportedUsage : PlateParseResult
}

/**
 * 검증을 거친 한국 차량번호다. [parse]로만 만들 수 있다.
 *
 * [toString]은 일부러 마스킹한다. 실수로 문자열 보간에 쓰여도 전체 번호가 드러나지 않게 하기 위해서다.
 */
class PlateNumber private constructor(
    val region: String?,
    val classNumber: String,
    val usage: Char,
    val serial: String,
) {
    val normalized: String get() = "${region.orEmpty()}$classNumber$usage$serial"

    val masked: String get() = "${region.orEmpty()}$classNumber$usage **${serial.takeLast(2)}"

    override fun toString(): String = masked

    override fun equals(other: Any?): Boolean = other is PlateNumber && other.normalized == normalized

    override fun hashCode(): Int = normalized.hashCode()

    companion object {
        private fun MatchGroupCollection.getValue(name: String) = checkNotNull(get(name)).value

        private val SEPARATORS = Regex("[\\s\\-·.]")
        private val JAMO = Regex("[\\u3131-\\u318E]")
        private val PATTERN = Regex("^(?<region>[가-힣]{2})?(?<class>\\d{2,3})(?<usage>[가-힣])(?<serial>\\d{4})$")

        private val REGIONS = setOf(
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주",
        )

        // 자가용, 영업용(아바사자배), 렌터카(하허호) 용도 문자다. 군용과 외교용 번호판은 범위에서 제외한다.
        private val USAGE_LETTERS =
            "가나다라마거너더러머버서어저고노도로모보소오조구누두루무부수우주아바사자배하허호".toSet()

        fun parse(input: String): PlateParseResult {
            val compact = input.replace(SEPARATORS, "")
            val match = PATTERN.matchEntire(compact)
            return when {
                compact.isEmpty() -> PlateParseResult.Empty
                JAMO.containsMatchIn(compact) -> PlateParseResult.Composing
                match == null -> PlateParseResult.InvalidFormat
                else -> fromMatch(match)
            }
        }

        private fun fromMatch(match: MatchResult): PlateParseResult {
            val groups = match.groups
            val region = groups["region"]?.value
            val usage = groups.getValue("usage").single()
            return when {
                region != null && region !in REGIONS -> PlateParseResult.InvalidFormat
                usage !in USAGE_LETTERS -> PlateParseResult.UnsupportedUsage
                else -> PlateParseResult.Valid(
                    PlateNumber(region, groups.getValue("class"), usage, groups.getValue("serial")),
                )
            }
        }
    }
}
