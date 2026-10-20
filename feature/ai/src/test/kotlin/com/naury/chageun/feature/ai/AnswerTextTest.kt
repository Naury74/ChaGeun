package com.naury.chageun.feature.ai

import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnswerTextTest {

    @Test
    fun markdown_becomesBoldAndBullets() {
        val text = answerText("## 요약\n* **엔진오일** 교환이 다가와요\n- 점검 권장")
        assertThat(text.text).isEqualTo("요약\n• 엔진오일 교환이 다가와요\n• 점검 권장")
        val bold = text.spanStyles.filter { it.item.fontWeight == FontWeight.SemiBold }
            .map { text.text.substring(it.start, it.end) }
        assertThat(bold).containsExactly("요약", "엔진오일").inOrder()
    }

    @Test
    fun unclosedBold_isDroppedWithoutStyle() {
        val text = answerText("곧 **엔진오")
        assertThat(text.text).isEqualTo("곧 엔진오")
        assertThat(text.spanStyles).isEmpty()
    }
}
