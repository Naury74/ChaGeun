package com.naury.chageun.core.domain.weather

import com.naury.chageun.core.model.KoreaArea

/**
 * 지역 이름 찾기. "강남", "서울 역삼", "수원 장안", "ㅇㅅ1"처럼 낱말 일부나 초성으로 찾는다.
 * 띄어 쓴 낱말은 순서와 상관없이 모두 이름 어딘가에 들어 있어야 한다.
 */
internal class KoreaRegionSearch(entries: List<KoreaAreaEntry>) {

    private val items: List<Item> = entries.map { entry ->
        // 원래 이름(서울특별시 수원시장안구)과 짧은 이름(서울, 수원시 장안구)을 모두 넣어 어느 쪽으로 써도 찾는다.
        val text = (entry.rawPath + entry.area.path).filterNot(Char::isWhitespace)
        Item(entry.area, text, initials(text))
    }

    fun find(query: String, limit: Int): List<KoreaArea> {
        val tokens = query.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        val last = tokens.last()
        return items.asSequence()
            .filter { item -> tokens.all { item.matches(it) } }
            .sortedWith(compareBy({ rank(it.area.name, last) }, { it.area.level.ordinal }))
            .take(limit)
            .map { it.area }
            .toList()
    }

    /** 자기 이름이 그 낱말로 시작하면 먼저, 자기 이름에 들어 있으면 그다음, 위 단계 이름에만 있으면 마지막이다. */
    private fun rank(name: String, token: String): Int = when {
        name.startsWith(token) -> 0
        name.contains(token) -> 1
        isInitials(token) && initials(name).contains(token) -> 1
        else -> 2
    }

    private data class Item(val area: KoreaArea, val text: String, val initials: String) {
        fun matches(token: String): Boolean = text.contains(token) || isInitials(token) && initials.contains(token)
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
        const val INITIALS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
        const val SYLLABLE_START = '가'
        const val SYLLABLE_END = '힣'

        // 한 초성에 중성 21개 × 종성 28개가 딸려 있다.
        const val SYLLABLES_PER_INITIAL = 21 * 28

        /** 초성만으로 된 낱말인지. "ㅇㅅ1"처럼 숫자가 섞여도 된다. */
        fun isInitials(token: String): Boolean =
            token.any { it in INITIALS } && token.all { it in INITIALS || it.isDigit() }

        /** 한글 음절을 초성으로 바꾼다(강남구 → ㄱㄴㄱ). 숫자·기호는 그대로 둔다. */
        fun initials(text: String): String = buildString(text.length) {
            text.forEach { c ->
                append(
                    if (c in
                        SYLLABLE_START..SYLLABLE_END
                    ) {
                        INITIALS[(c - SYLLABLE_START) / SYLLABLES_PER_INITIAL]
                    } else {
                        c
                    },
                )
            }
        }
    }
}
