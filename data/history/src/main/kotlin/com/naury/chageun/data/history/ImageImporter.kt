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

/** Abstracted so the repository can be tested without decoding real images. */
internal fun interface ImageImporter {
    fun import(sourceUri: String, target: File, thumbnail: File): ImportedImage?
}

/**
 * Decodes, downsizes and re-encodes as JPEG. Re-encoding writes no EXIF, which drops GPS location and
 * camera data; orientation is applied to the pixels first so photos keep their rotation.
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
    /** Largest power-of-two subsampling that keeps the longer edge at or above [maxEdge]. */
    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    /** Scales down to fit [maxEdge] on the longer side, keeping the aspect ratio; never upscales. */
    fun fit(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
        val longer = maxOf(width, height)
        if (longer <= maxEdge) return width to height
        val scale = maxEdge.toDouble() / longer
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }
}
