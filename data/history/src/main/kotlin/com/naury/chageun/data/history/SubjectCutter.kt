package com.naury.chageun.data.history

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.io.File
import kotlinx.coroutines.tasks.await

/** 사진에서 피사체만 남긴 투명 PNG를 만든다. 실제 모델 없이 Repository를 테스트할 수 있도록 추상화했다. */
internal fun interface SubjectCutter {
    /** 피사체를 찾아 [target]에 저장하면 true. 모델이 아직 없거나 피사체가 너무 작으면 false다. */
    suspend fun cutout(source: File, target: File): Boolean
}

/**
 * ML Kit 피사체 분리. 기기 안에서만 처리하므로 사진이 밖으로 나가지 않는다.
 * 투명한 여백은 잘라 내 Hero에서 차가 가운데 크게 보이게 한다.
 *
 * 네이티브 코드라 일부 CPU(에뮬레이터 등)에서 SIGILL로 프로세스가 죽을 수 있다. 앱이 함께 죽지 않도록
 * 이 클래스는 [CutoutService]의 별도 프로세스에서만 쓴다.
 */
internal object MlKitSubjectCutter : SubjectCutter {

    override suspend fun cutout(source: File, target: File): Boolean {
        val bitmap = BitmapFactory.decodeFile(source.path) ?: return false
        val segmenter = SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder().enableForegroundBitmap().build(),
        )
        val trimmed = try {
            segmenter.process(InputImage.fromBitmap(bitmap, 0)).await().foregroundBitmap?.let { foreground ->
                // 피사체가 사진의 아주 작은 부분이면 차가 아닌 다른 것을 잡았을 가능성이 커 원본을 쓴다.
                SubjectBounds.of(foreground)
                    ?.takeIf { it.area >= bitmap.width.toLong() * bitmap.height * MIN_SUBJECT_FRACTION }
                    ?.let { Bitmap.createBitmap(foreground, it.left, it.top, it.width, it.height) }
            }
        } finally {
            segmenter.close()
        }
        trimmed?.let { image ->
            target.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
        }
        return trimmed != null
    }

    private const val MIN_SUBJECT_FRACTION = 0.05
    private const val PNG_QUALITY = 100
}

/** 불투명한 픽셀을 모두 담는 가장 작은 사각형. */
internal data class SubjectBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left + 1
    val height get() = bottom - top + 1
    val area get() = width.toLong() * height

    companion object {
        fun of(bitmap: Bitmap): SubjectBounds? {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            return of(pixels, bitmap.width, bitmap.height)
        }

        fun of(argb: IntArray, width: Int, height: Int): SubjectBounds? {
            fun opaque(x: Int, y: Int) = argb[y * width + x] ushr ALPHA_SHIFT > ALPHA_THRESHOLD
            val rows = (0 until height).filter { y -> (0 until width).any { x -> opaque(x, y) } }
            if (rows.isEmpty()) return null
            val columns = (0 until width).filter { x -> rows.any { y -> opaque(x, y) } }
            return SubjectBounds(columns.first(), rows.first(), columns.last(), rows.last())
        }

        private const val ALPHA_SHIFT = 24

        // 가장자리의 거의 투명한 번짐은 여백으로 본다.
        private const val ALPHA_THRESHOLD = 16
    }
}
