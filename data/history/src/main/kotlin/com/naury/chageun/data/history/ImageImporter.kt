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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** [takenOn]은 원본 EXIF의 촬영일이다. 저장한 파일에는 EXIF를 남기지 않는다. */
internal data class ImportedImage(
    val file: File,
    val thumbnail: File,
    val sizeBytes: Long,
    val takenOn: LocalDate? = null,
)

/** 실제 이미지를 디코딩하지 않고 Repository를 테스트할 수 있도록 추상화했다. */
/** [ImageImporter]는 항상 JPEG로 다시 인코딩한다. */
internal const val MIME_JPEG = "image/jpeg"

internal fun interface ImageImporter {
    /** [maxEdge]는 저장할 이미지의 긴 변 최대 픽셀이다. */
    fun import(sourceUri: String, target: File, thumbnail: File, maxEdge: Int): ImportedImage?

    companion object {
        const val DEFAULT_EDGE_PX = 2_048

        /** 영수증의 작은 글씨를 읽을 수 있을 만큼. 사진 편집기의 최대 크기와 같다. */
        const val HIGH_QUALITY_EDGE_PX = 4_096
    }
}

/**
 * 디코딩 후 크기를 줄여 JPEG로 다시 인코딩한다. 재인코딩 시 EXIF를 쓰지 않으므로 GPS 위치와 카메라 정보가
 * 제거된다. 사진 회전이 유지되도록 방향 정보는 먼저 픽셀에 반영한다.
 */
internal class BitmapImageImporter @Inject constructor(@ApplicationContext private val context: Context) :
    ImageImporter {

    override fun import(sourceUri: String, target: File, thumbnail: File, maxEdge: Int): ImportedImage? {
        val uri = sourceUri.toUri()
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageSizing.sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        }
        val decoded =
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val exif = resolver.openInputStream(uri)?.use { ExifInterface(it) }
        val rotation = exif?.rotationDegrees ?: 0
        val image = decoded.scaledTo(maxEdge).rotated(rotation)

        target.parentFile?.mkdirs()
        target.outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        thumbnail.outputStream().use {
            image.scaledTo(THUMBNAIL_EDGE_PX).compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
        }
        return ImportedImage(target, thumbnail, target.length(), exif?.takenOn())
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

/** 촬영일 "yyyy:MM:dd HH:mm:ss". 형식이 다르거나 없으면 null이다. */
internal fun ExifInterface.takenOn(): LocalDate? {
    val value = getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: getAttribute(ExifInterface.TAG_DATETIME)
    return value?.take(EXIF_DATE_LENGTH)
        ?.let { runCatching { LocalDate.parse(it, EXIF_DATE) }.getOrNull() }
}

private const val EXIF_DATE_LENGTH = 10
private val EXIF_DATE = DateTimeFormatter.ofPattern("yyyy:MM:dd")
