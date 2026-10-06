package com.naury.chageun.core.ui.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID
import kotlin.math.max

/**
 * 카메라 촬영 결과와 편집한 사진을 저장소로 가져가기 전까지 잠시 두는 캐시 파일.
 * 가져오기는 비동기로 끝나므로 바로 지우지 않고, 다음에 사진을 넣을 때 하루 지난 파일을 정리한다.
 */
object PhotoFiles {
    private const val DIRECTORY = "photo_input"
    private const val STALE_MILLIS = 24 * 60 * 60 * 1000L
    private const val JPEG_QUALITY = 95

    /** 편집할 때 다루는 최대 크기. 저장할 때는 저장소가 다시 크기를 정한다. */
    const val MAX_EDIT_EDGE_PX = 4_096

    fun authority(context: Context) = "${context.packageName}.photos"

    /** 시스템 카메라 앱이 결과를 쓸 수 있는 content URI. */
    fun newCaptureUri(context: Context): Uri = FileProvider.getUriForFile(context, authority(context), newFile(context))

    fun cleanStale(context: Context, now: Long = System.currentTimeMillis()) {
        directory(context).listFiles()?.filter { now - it.lastModified() > STALE_MILLIS }?.forEach(File::delete)
    }

    /**
     * [sourceUri]를 방향 정보대로 세운 뒤 [quarterTurns]만큼 시계 방향으로 돌리고 [crop]으로 잘라 JPEG로 쓴다.
     * 위치와 카메라 정보는 남기지 않고, 앨범이 날짜별로 정리할 수 있게 촬영일만 옮겨 적는다.
     *
     * @return 쓴 파일의 URI. 읽을 수 없는 이미지면 null.
     */
    fun render(context: Context, sourceUri: String, quarterTurns: Int, crop: CropRect): String? {
        val bitmap = decodeUpright(context, sourceUri) ?: return null
        val rotated = bitmap.rotate(quarterTurns * QUARTER_DEGREES)
        val area = CropMath.toPixels(crop, rotated.width, rotated.height)
        val cropped = Bitmap.createBitmap(rotated, area.x, area.y, area.width, area.height)
        val target = newFile(context)
        target.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        copyTakenDate(context, sourceUri, target)
        return target.toUri().toString()
    }

    /** 편집 화면 미리보기와 자르기에 쓸 수 있게 방향을 바로잡아 읽는다. */
    fun decodeUpright(context: Context, sourceUri: String, maxEdge: Int = MAX_EDIT_EDGE_PX): Bitmap? {
        val uri = sourceUri.toUri()
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val degrees = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees }
        }.getOrNull() ?: 0
        return bitmap.rotate(degrees)
    }

    private fun copyTakenDate(context: Context, sourceUri: String, target: File) {
        val taken = runCatching {
            context.contentResolver.openInputStream(sourceUri.toUri())?.use { stream ->
                ExifInterface(stream).let {
                    it.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: it.getAttribute(ExifInterface.TAG_DATETIME)
                }
            }
        }.getOrNull() ?: return
        runCatching {
            ExifInterface(target).apply {
                setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, taken)
                saveAttributes()
            }
        }
    }

    private fun Bitmap.rotate(degrees: Int): Bitmap {
        if (degrees % FULL_TURN_DEGREES == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun directory(context: Context) = File(context.cacheDir, DIRECTORY).apply { mkdirs() }

    private fun newFile(context: Context) = File(directory(context), "${UUID.randomUUID()}.jpg")

    private const val QUARTER_DEGREES = 90
    private const val FULL_TURN_DEGREES = 360
}
