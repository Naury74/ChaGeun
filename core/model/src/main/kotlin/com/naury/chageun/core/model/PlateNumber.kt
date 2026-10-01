package com.naury.chageun.core.model

sealed interface PlateParseResult {
    data class Valid(val plate: PlateNumber) : PlateParseResult

    data object Empty : PlateParseResult

    /** Input still contains standalone jamo, i.e. the user is mid-composition. Callers should not show an error. */
    data object Composing : PlateParseResult

    data object InvalidFormat : PlateParseResult

    data object UnsupportedUsage : PlateParseResult
}

/**
 * A validated Korean registration plate, created only through [parse].
 *
 * [toString] is masked on purpose so an accidental string interpolation never exposes the full plate.
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

        // Private, commercial (아바사자배) and rental (하허호) letters. Military and diplomatic plates are out of scope.
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
