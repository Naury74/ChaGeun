package com.naury.chageun.core.ui.photo

/**
 * 이미지 안의 자르기 영역. 회전을 반영한 이미지 크기에 대한 0~1 비율 좌표라서 화면 크기나 해상도와 상관없다.
 */
data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    companion object {
        val Full = CropRect(0f, 0f, 1f, 1f)
    }
}

private const val RATIO_4_3 = 4f / 3f
private const val RATIO_16_9 = 16f / 9f

enum class CropAspect(val ratio: Float?) {
    Free(null),
    Square(1f),
    FourThree(RATIO_4_3),
    SixteenNine(RATIO_16_9),
}

/** 픽셀 단위 자르기 영역. */
data class PixelRect(val x: Int, val y: Int, val width: Int, val height: Int)

enum class CropHandle { TopLeft, TopRight, BottomLeft, BottomRight, Move }

object CropMath {

    /** 너무 작게 줄여 손가락으로 다시 잡을 수 없게 되지 않도록 한 변의 최소 비율. */
    const val MIN_SIZE = 0.1f

    /**
     * [aspect] 비율을 [imageWidth]×[imageHeight] 픽셀 기준으로 맞춘, 이미지 가운데의 가장 큰 영역.
     * 비율이 없으면 전체를 쓴다.
     */
    fun centered(aspect: CropAspect, imageWidth: Int, imageHeight: Int): CropRect {
        val ratio = aspect.ratio ?: return CropRect.Full
        val imageRatio = imageWidth.toFloat() / imageHeight
        return if (imageRatio > ratio) {
            val width = ratio / imageRatio
            CropRect((1f - width) / 2f, 0f, (1f + width) / 2f, 1f)
        } else {
            val height = imageRatio / ratio
            CropRect(0f, (1f - height) / 2f, 1f, (1f + height) / 2f)
        }
    }

    /**
     * [handle]을 비율 좌표로 [dx], [dy]만큼 끈 결과. 이미지 밖으로 나가지 않고 최소 크기를 지킨다.
     * 비율이 고정이면 모서리를 끌 때 가로 변화에 맞춰 세로를 정한다.
     */
    fun drag(
        rect: CropRect,
        handle: CropHandle,
        dx: Float,
        dy: Float,
        aspect: CropAspect,
        imageWidth: Int,
        imageHeight: Int,
    ): CropRect {
        if (handle == CropHandle.Move) return move(rect, dx, dy)
        var left = rect.left
        var top = rect.top
        var right = rect.right
        var bottom = rect.bottom
        when (handle) {
            CropHandle.TopLeft -> {
                left += dx
                top += dy
            }
            CropHandle.TopRight -> {
                right += dx
                top += dy
            }
            CropHandle.BottomLeft -> {
                left += dx
                bottom += dy
            }
            CropHandle.BottomRight -> {
                right += dx
                bottom += dy
            }
            CropHandle.Move -> Unit
        }
        left = left.coerceIn(0f, rect.right - MIN_SIZE)
        right = right.coerceIn(rect.left + MIN_SIZE, 1f)
        top = top.coerceIn(0f, rect.bottom - MIN_SIZE)
        bottom = bottom.coerceIn(rect.top + MIN_SIZE, 1f)
        val free = CropRect(left, top, right, bottom)
        val ratio = aspect.ratio ?: return free
        return lockAspect(free, handle, ratio * imageHeight / imageWidth)
    }

    private fun move(rect: CropRect, dx: Float, dy: Float): CropRect {
        val x = dx.coerceIn(-rect.left, 1f - rect.right)
        val y = dy.coerceIn(-rect.top, 1f - rect.bottom)
        return CropRect(rect.left + x, rect.top + y, rect.right + x, rect.bottom + y)
    }

    /** [normalizedRatio]는 비율 좌표에서의 가로/세로 비다. 끌던 모서리의 맞은편 꼭짓점을 고정한다. */
    private fun lockAspect(rect: CropRect, handle: CropHandle, normalizedRatio: Float): CropRect {
        val anchorX = if (handle == CropHandle.TopLeft || handle == CropHandle.BottomLeft) rect.right else rect.left
        val anchorY = if (handle == CropHandle.TopLeft || handle == CropHandle.TopRight) rect.bottom else rect.top
        val maxWidth = if (anchorX == rect.right) anchorX else 1f - anchorX
        val maxHeight = if (anchorY == rect.bottom) anchorY else 1f - anchorY
        var width = rect.width.coerceAtMost(maxWidth)
        var height = width / normalizedRatio
        if (height > maxHeight) {
            height = maxHeight
            width = height * normalizedRatio
        }
        val left = if (anchorX == rect.right) anchorX - width else anchorX
        val top = if (anchorY == rect.bottom) anchorY - height else anchorY
        return CropRect(left, top, left + width, top + height)
    }

    /** 비율 좌표를 픽셀 영역으로 바꾼다. 최소 1px을 보장한다. */
    fun toPixels(rect: CropRect, width: Int, height: Int): PixelRect {
        val left = (rect.left * width).toInt().coerceIn(0, width - 1)
        val top = (rect.top * height).toInt().coerceIn(0, height - 1)
        val right = (rect.right * width).toInt().coerceIn(left + 1, width)
        val bottom = (rect.bottom * height).toInt().coerceIn(top + 1, height)
        return PixelRect(left, top, right - left, bottom - top)
    }
}
