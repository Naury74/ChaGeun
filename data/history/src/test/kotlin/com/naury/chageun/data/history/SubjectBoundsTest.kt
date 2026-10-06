package com.naury.chageun.data.history

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SubjectBoundsTest {

    @Test
    fun findsSmallestBoxAroundOpaquePixels() {
        val width = 5
        val pixels = IntArray(width * 4)
        pixels[1 * width + 1] = OPAQUE
        pixels[2 * width + 3] = OPAQUE
        // 거의 투명한 번짐은 무시한다.
        pixels[3 * width + 4] = FAINT

        assertThat(SubjectBounds.of(pixels, width, 4)).isEqualTo(SubjectBounds(1, 1, 3, 2))
    }

    @Test
    fun returnsNull_whenNothingIsOpaque() {
        assertThat(SubjectBounds.of(IntArray(9), 3, 3)).isNull()
    }

    private companion object {
        const val OPAQUE = 0xFF336699.toInt()
        const val FAINT = 0x08FFFFFF
    }
}
