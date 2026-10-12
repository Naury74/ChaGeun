package com.naury.chageun.feature.vehicle.album

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.applyCanvas
import com.naury.chageun.core.model.AlbumMediaType
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.ui.rememberFileImage
import java.io.File
import java.time.Instant
import java.time.LocalDate

/** 앨범 화면 테스트가 함께 쓰는 사진과 화면 상태. 실제 사진 대신 하늘·도로 위에 단순한 차 모양을 그린 가로 사진을 쓴다. */
internal object AlbumFixtures {
    val photos by lazy {
        listOf(
            photo("p1", Scene.Dusk, LocalDate.of(2026, 10, 3), "Car wash before the trip"),
            photo("p2", Scene.Day, LocalDate.of(2026, 10, 3), "New tires"),
            photo("p3", Scene.Night, LocalDate.of(2026, 9, 21), null),
            photo("p4", Scene.Day, LocalDate.of(2026, 9, 21), null),
            photo("p5", Scene.Dusk, LocalDate.of(2026, 9, 2), "Coastal road"),
        )
    }

    /** 사진 사이에 42초짜리 가로 동영상 하나를 섞은 앨범. Robolectric은 동영상을 읽지 못하므로 포스터만 그린다. */
    val withVideo by lazy {
        val poster = File.createTempFile("album-video", ".jpg").apply { deleteOnExit() }
        val bitmap = drawScene(Scene.Dusk, VIDEO_WIDTH, VIDEO_HEIGHT)
        poster.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        val video = AlbumPhoto(
            "v1", "/missing/album_v1.mp4", poster.absolutePath, LocalDate.of(2026, 10, 3),
            "Highway drive", Instant.EPOCH,
            mediaType = AlbumMediaType.Video, durationMs = 42_000, width = VIDEO_WIDTH, height = VIDEO_HEIGHT,
        )
        listOf(photos[0], video) + photos.drop(1)
    }

    fun state(
        selected: String? = "p2",
        isCoverUpdated: Boolean = false,
        picked: Set<String> = emptySet(),
        items: List<AlbumPhoto> = photos,
        isEditFailed: Boolean = false,
    ) = AlbumUiState(
        isLoading = false,
        days = items.groupBy { it.takenOn }.map { (date, day) -> AlbumDay(date, day) },
        selected = items.firstOrNull { it.id == selected },
        isCoverUpdated = isCoverUpdated,
        isEditFailed = isEditFailed,
        picked = picked,
    )

    private enum class Scene { Day, Dusk, Night }

    private fun photo(id: String, scene: Scene, date: LocalDate, comment: String?): AlbumPhoto {
        val file = File.createTempFile("album-$id", ".jpg").apply { deleteOnExit() }
        val bitmap = drawScene(scene)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        return AlbumPhoto(id, file.absolutePath, file.absolutePath, date, comment, Instant.EPOCH)
    }

    private fun drawScene(scene: Scene, width: Int = PHOTO_WIDTH, height: Int = PHOTO_HEIGHT): Bitmap {
        val (skyTop, skyBottom, body) = when (scene) {
            Scene.Day -> Triple(Color.rgb(120, 170, 225), Color.rgb(200, 225, 245), Color.rgb(36, 52, 90))
            Scene.Dusk -> Triple(Color.rgb(250, 150, 110), Color.rgb(255, 214, 160), Color.rgb(200, 40, 50))
            Scene.Night -> Triple(Color.rgb(15, 20, 45), Color.rgb(50, 60, 100), Color.rgb(230, 230, 235))
        }
        val w = width.toFloat()
        val h = height.toFloat()
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).applyCanvas {
            // Robolectric native 그래픽에서 Shader를 쓰면 프로세스가 멈추므로 한 줄씩 색을 바꿔 하늘을 그린다.
            val horizon = h * 0.65f
            repeat(horizon.toInt()) { row ->
                val color = ColorUtils.blendARGB(skyTop, skyBottom, row / horizon)
                drawRect(0f, row.toFloat(), w, row + 1f, Paint().apply { this.color = color })
            }
            drawRect(0f, h * 0.65f, w, h, Paint().apply { color = Color.rgb(70, 72, 78) })
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = body }
            drawRoundRect(RectF(w * 0.22f, h * 0.5f, w * 0.78f, h * 0.72f), 30f, 30f, paint)
            drawRoundRect(RectF(w * 0.33f, h * 0.38f, w * 0.64f, h * 0.55f), 40f, 40f, paint)
            val wheel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(25, 25, 28) }
            drawCircle(w * 0.33f, h * 0.72f, h * 0.07f, wheel)
            drawCircle(w * 0.67f, h * 0.72f, h * 0.07f, wheel)
        }
    }

    private const val PHOTO_WIDTH = 800
    private const val PHOTO_HEIGHT = 600
    private const val VIDEO_WIDTH = 800
    private const val VIDEO_HEIGHT = 450
    private const val JPEG_QUALITY = 90
}

/**
 * 사진을 한 장씩 차례로 읽어 캐시에 넣은 뒤 화면을 그린다. 여러 IO 스레드가 동시에 사진을 읽은 채로 테스트가 끝나면
 * Robolectric native 그래픽이 프로세스를 멈추고, 캡처할 때 아직 안 읽힌 사진이 빈칸으로 찍히기 때문이다.
 */
internal fun ComposeContentTestRule.setContentWithPhotos(items: List<AlbumPhoto>, content: @Composable () -> Unit) {
    var preloaded by mutableIntStateOf(0)
    setContent {
        if (preloaded == items.size) content() else PreloadPhoto(items[preloaded])
    }
    repeat(items.size) {
        waitUntil(PHOTO_LOAD_TIMEOUT_MS) { onAllNodesWithTag(PRELOADED_TAG).fetchSemanticsNodes().isNotEmpty() }
        preloaded++
        waitForIdle()
    }
    // 사진 보기의 조작 막대는 연 뒤 조금 늦게 나타난다.
    mainClock.advanceTimeBy(CHROME_WAIT_MS)
    waitForIdle()
}

internal fun hasClickLabel(label: String) = SemanticsMatcher("click label = $label") {
    it.config.getOrNull(SemanticsActions.OnClick)?.label == label
}

// 동영상은 파일 대신 포스터를 읽어 둔다.
@Composable
private fun PreloadPhoto(photo: AlbumPhoto) {
    val image by rememberFileImage(if (photo.isVideo) photo.thumbnailPath else photo.filePath)
    if (image != null) Box(Modifier.testTag(PRELOADED_TAG))
}

private const val PHOTO_LOAD_TIMEOUT_MS = 5_000L
private const val PRELOADED_TAG = "preloaded"
private const val CHROME_WAIT_MS = 1_000L
