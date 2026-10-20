package com.naury.chageun.feature.ai.ondevice

/**
 * 작은 기기 안 모델은 같은 문단을 끝없이 되풀이하곤 한다. 끝난 줄이 앞에 나온 줄과 같으면 그 줄부터 잘라
 * 돌려주고, 아직 반복이 없으면 null을 돌려준다. 마지막 줄은 아직 쓰는 중일 수 있어 보지 않는다.
 */
internal fun cutRepetition(text: String): String? {
    val seen = HashSet<String>()
    var start = 0
    while (true) {
        val end = text.indexOf('\n', start)
        if (end < 0) return null
        val line = normalize(text.substring(start, end))
        if (line.length >= MIN_LINE_LENGTH && !seen.add(line)) return text.substring(0, start).trimEnd()
        start = end + 1
    }
}

private fun normalize(line: String): String = line.replace(MARKUP, "").trim()

private val MARKUP = Regex("""[*#_`>-]""")

// 짧은 줄("네.", 목록 기호 등)은 정상 답에서도 겹칠 수 있어 비교하지 않는다.
private const val MIN_LINE_LENGTH = 12

/**
 * 길이 제한에 걸려 문장 중간에서 끝난 답은 마지막으로 끝난 문장까지만 남긴다. 끝난 문장이 너무 앞에 있으면
 * 많이 잘리므로 그대로 둔다.
 */
internal fun dropUnfinishedSentence(text: String): String {
    val trimmed = text.trimEnd()
    if (trimmed.isEmpty() || trimmed.last() in SENTENCE_ENDS) return trimmed
    // 목록으로 답하면 마침표 없이 줄이 끝나므로 줄바꿈도 끝으로 본다.
    val end = trimmed.indexOfLast { it in SENTENCE_ENDS || it == '\n' }
    return if (end >= trimmed.length / 2) trimmed.substring(0, end + 1).trimEnd() else trimmed
}

private const val SENTENCE_ENDS = ".!?。"
