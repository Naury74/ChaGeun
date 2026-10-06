package com.naury.chageun.core.ui.photo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CropMathTest {

    @Test
    fun centered_fitsSquareInsideLandscape() {
        val rect = CropMath.centered(CropAspect.Square, imageWidth = 400, imageHeight = 200)

        assertThat(rect.left).isWithin(EPS).of(0.25f)
        assertThat(rect.right).isWithin(EPS).of(0.75f)
        assertThat(rect.top).isEqualTo(0f)
        assertThat(rect.bottom).isEqualTo(1f)
    }

    @Test
    fun drag_staysInsideImage_andKeepsMinimumSize() {
        val moved = CropMath.drag(CropRect.Full, CropHandle.TopLeft, -0.3f, 0.95f, CropAspect.Free, 100, 100)

        assertThat(moved.left).isEqualTo(0f)
        assertThat(moved.height).isWithin(EPS).of(CropMath.MIN_SIZE)
    }

    @Test
    fun move_isClampedToEdges() {
        val rect = CropRect(0.2f, 0.2f, 0.6f, 0.6f)

        val moved = CropMath.drag(rect, CropHandle.Move, 0.9f, -0.9f, CropAspect.Free, 100, 100)

        assertThat(moved.left).isWithin(EPS).of(0.6f)
        assertThat(moved.top).isEqualTo(0f)
        assertThat(moved.right).isEqualTo(1f)
        assertThat(moved.bottom).isWithin(EPS).of(0.4f)
    }

    @Test
    fun lockedAspect_keepsPixelRatio_whileDraggingCorner() {
        val start = CropMath.centered(CropAspect.FourThree, 800, 600)

        val dragged = CropMath.drag(start, CropHandle.BottomRight, -0.2f, 0f, CropAspect.FourThree, 800, 600)

        val pixelRatio = (dragged.width * 800) / (dragged.height * 600)
        assertThat(pixelRatio).isWithin(EPS).of(4f / 3f)
        // 맞은편 꼭짓점은 그대로다.
        assertThat(dragged.left).isEqualTo(start.left)
        assertThat(dragged.top).isEqualTo(start.top)
    }

    @Test
    fun toPixels_neverReturnsEmptyArea() {
        assertThat(CropMath.toPixels(CropRect(1f, 1f, 1f, 1f), 50, 40)).isEqualTo(PixelRect(49, 39, 1, 1))
    }

    private companion object {
        const val EPS = 0.0001f
    }
}
