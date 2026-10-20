package com.naury.chageun.feature.ai

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * 모델이 마크다운을 섞어 답해도 기호가 그대로 보이지 않게 한다. 굵게(**), 목록(*, -), 제목(#)만 다룬다.
 * 답을 쓰는 중에 닫히지 않은 **는 지우고 나머지만 보여 준다.
 */
internal fun answerText(text: String): AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { index, raw ->
        if (index > 0) append('\n')
        val heading = HEADING.matchAt(raw, 0)
        val bullet = BULLET.matchAt(raw, 0)
        val line = when {
            heading != null -> raw.substring(heading.range.last + 1)
            bullet != null -> "• " + raw.substring(bullet.range.last + 1)
            else -> raw
        }
        if (heading != null) {
            withStyle(BOLD) { append(line.replace(STARS, "")) }
        } else {
            appendBold(line)
        }
    }
}

private fun AnnotatedString.Builder.appendBold(line: String) {
    val parts = line.split(STARS)
    parts.forEachIndexed { i, part ->
        // 홀수 조각이 ** 사이다. 마지막 조각이 홀수면 아직 닫히지 않은 것이라 굵게 하지 않는다.
        if (i % 2 == 1 && i < parts.lastIndex) withStyle(BOLD) { append(part) } else append(part)
    }
}

private val BOLD = SpanStyle(fontWeight = FontWeight.SemiBold)
private const val STARS = "**"
private val HEADING = Regex("""\s*#{1,6}\s+""")
private val BULLET = Regex("""\s*[*-]\s+""")
