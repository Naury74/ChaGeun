package com.naury.chageun.data.history

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

internal data class ImportedImage(val file: File, val thumbnail: File, val sizeBytes: Long)

/** 실제 이미지를 디코딩하지 않고 Repository를 테스트할 수 있도록 추상화했다. */
/** [ImageImporter]는 항상 JPEG로 다시 인코딩한다. */
internal const val MIME_JPEG = "image/jpeg"

internal fun interface ImageImporter {
    fun import(sourceUri: String, target: File, thumbnail: File): ImportedImage?
}

/**
 * 디코딩 후 크기를 줄여 JPEG로 다시 인코딩한다. 재인코딩 시 EXIF를 쓰지 않으므로 GPS 위치와 카메라 정보가
 * 제거된다. 사진 회전이 유지되도록 방향 정보는 먼저 픽셀에 반영한다.
 */
internal class BitmapImageImporter @Inject constructor(@ApplicationContext private val context: Context) :
    ImageImporter {

    override fun import(sourceUri: String, target: File, thumbnail: File): ImportedImage? {
        val uri = sourceUri.toUri()
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageSizing.sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE_PX)
        }
        val decoded =
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val rotation = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        val image = decoded.scaledTo(MAX_EDGE_PX).rotated(rotation)

        target.parentFile?.mkdirs()
        target.outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        thumbnail.outputStream().use {
            image.scaledTo(THUMBNAIL_EDGE_PX).compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
        }
        return ImportedImage(target, thumbnail, target.length())
    }

    private fun Bitmap.scaledTo(maxEdge: Int): Bitmap {
        val (targetWidth, targetHeight) = ImageSizing.fit(width, height, maxEdge)
        return if (targetWidth == width && targetHeight == height) this else scale(targetWidth, targetHeight)
    }

    private fun Bitmap.rotated(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private companion object {
        const val MAX_EDGE_PX = 2_048
        const val THUMBNAIL_EDGE_PX = 320
        const val JPEG_QUALITY = 85
    }
}

internal object ImageSizing {
    /** 긴 변이 [maxEdge] 이상으로 유지되는 가장 큰 2의 거듭제곱 서브샘플링 값. */
    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    /** 가로세로 비율을 유지하며 긴 변이 [maxEdge]에 맞도록 축소한다. 확대는 하지 않는다. */
    fun fit(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
        val longer = maxOf(width, height)
        if (longer <= maxEdge) return width to height
        val scale = maxEdge.toDouble() / longer
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }
}
