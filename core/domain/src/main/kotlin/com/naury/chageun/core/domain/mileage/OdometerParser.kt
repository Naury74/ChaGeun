package com.naury.chageun.core.domain.mileage

import com.naury.chageun.core.model.Kilometers
import com.naury.chageun.core.model.MileageReading
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 계기판 사진에서 읽은 글자로 고른 주행거리 후보.
 *
 * @property best 가장 그럴듯한 누적 주행거리. 입력칸에 미리 채운다.
 * @property others 다른 후보. 사용자가 바꿔 고를 수 있게 보여 준다.
 * @property range 주행 가능 거리(DTE)로 보이는 값. 참고로만 보여 주고 저장하지 않는다.
 */
data class OdometerCandidates(val best: Kilometers?, val others: List<Kilometers>, val range: Kilometers?)

/**
 * 계기판 사진에서 읽은 줄들로 누적 주행거리를 고른다. 인식 결과가 틀릴 수 있으므로 값을 고르기만 하고
 * 저장은 사용자가 확인한 뒤에 한다.
 *
 * 걸러 내는 숫자: 시계(12:34), 온도(°), 연비·퍼센트, 소수점이 있는 트립 거리, 마지막 기록보다 작거나
 * 지난 기간 동안 달릴 수 없을 만큼 큰 값.
 */
object OdometerParser {

    /** 하루에 달릴 수 있는 넉넉한 최대 거리. 장거리 운행도 걸러지지 않게 크게 잡는다. */
    private const val MAX_DAILY_KM = 1_500L

    /** 같은 날 다시 기록해도 걸러지지 않게 기간과 상관없이 허용하는 거리. */
    private const val MIN_ALLOWANCE_KM = 1_000L

    private const val MAX_ODOMETER_KM = 1_999_999L
    private const val MIN_ODOMETER_DIGITS = 3
    private const val ODO_KEYWORD_SCORE = 3
    private const val KM_UNIT_SCORE = 1
    private const val LONG_NUMBER_SCORE = 1
    private const val LONG_NUMBER_DIGITS = 4

    private val TIME = Regex("""\b\d{1,2}:\d{2}\b""")
    private val SKIP_UNITS = Regex("""°|%|km/h|㎞/h|km/l|l/100|kml""", RegexOption.IGNORE_CASE)
    private val ODO_KEYWORDS = Regex("""odo|적산|총\s*주행|total""", RegexOption.IGNORE_CASE)
    private val RANGE_KEYWORDS = Regex("""dte|range|주행\s*가능|가능\s*거리|남은\s*거리""", RegexOption.IGNORE_CASE)
    private val KM_UNIT = Regex("""km|㎞""", RegexOption.IGNORE_CASE)

    /** 1,234 / 1 234 같은 천 단위 구분과, 소수점이 한 자리인 트립 거리(123.4)를 구분한다. */
    private val NUMBER = Regex("""\d{1,3}(?:[ ,]\d{3})+(?!\d)|\d+(?:\.\d+)?""")

    fun parse(lines: List<String>, previous: MileageReading?, today: LocalDate): OdometerCandidates {
        val readings = lines.mapNotNull(::readLine)
        val range = readings.firstOrNull { it.isRange && it.values.isNotEmpty() }?.values?.first()
        val scores = readings.filterNot { it.isRange }
            .flatMap { line -> line.values.filter { isPlausible(it, previous, today) }.map { it to line.score(it) } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.max() }
        val expected = previous?.mileage?.value ?: 0L
        // 점수가 같으면 마지막 기록에 가까운 값을 먼저 둔다. 누적 주행거리는 조금씩만 늘어나기 때문이다.
        val ordered = scores.entries
            .sortedWith(compareByDescending<Map.Entry<Long, Int>> { it.value }.thenBy { it.key - expected })
            .map { Kilometers(it.key) }
        return OdometerCandidates(
            best = ordered.firstOrNull(),
            others = ordered.drop(1),
            range = range?.let(::Kilometers),
        )
    }

    private class LineNumbers(val values: List<Long>, val isRange: Boolean, val isOdo: Boolean, val hasKm: Boolean) {
        fun score(value: Long): Int = (if (isOdo) ODO_KEYWORD_SCORE else 0) +
            (if (hasKm) KM_UNIT_SCORE else 0) +
            (if (value.toString().length >= LONG_NUMBER_DIGITS) LONG_NUMBER_SCORE else 0)
    }

    /** 시계를 지우고 연비·온도 같은 줄은 건너뛴 뒤, 소수가 아닌 숫자를 모은다. */
    private fun readLine(raw: String): LineNumbers? {
        val line = TIME.replace(raw, " ")
        if (SKIP_UNITS.containsMatchIn(line)) return null
        val values = NUMBER.findAll(line)
            .filterNot { '.' in it.value }
            .mapNotNull { match -> match.value.filter(Char::isDigit).toLongOrNull() }
            .toList()
        return LineNumbers(
            values = values,
            isRange = RANGE_KEYWORDS.containsMatchIn(line),
            isOdo = ODO_KEYWORDS.containsMatchIn(line),
            hasKm = KM_UNIT.containsMatchIn(line),
        )
    }

    private fun isPlausible(value: Long, previous: MileageReading?, today: LocalDate): Boolean {
        if (value.toString().length < MIN_ODOMETER_DIGITS || value > MAX_ODOMETER_KM) return false
        previous ?: return true
        val days = ChronoUnit.DAYS.between(previous.date, today).coerceAtLeast(0)
        val allowance = maxOf(MIN_ALLOWANCE_KM, days * MAX_DAILY_KM)
        return value >= previous.mileage.value && value <= previous.mileage.value + allowance
    }
}
