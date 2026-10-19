package com.naury.chageun.core.ui.photo

import android.content.Context
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.naury.chageun.core.designsystem.component.ChageunToasts
import com.naury.chageun.core.ui.R
import com.naury.chageun.core.ui.launchExternal
import com.naury.framekit.FrameKitContract
import com.naury.framekit.FrameKitRequest
import com.naury.framekit.android.output.FrameKitOutputs
import com.naury.framekit.android.result.FrameKitResult
import com.naury.framekit.ui.image.contract.ImageEditorContract
import com.naury.framekit.ui.image.contract.ImageEditorRequest
import com.naury.framekit.ui.motion.contract.MotionEditorContract
import com.naury.framekit.ui.motion.contract.MotionEditorRequest
import com.naury.framekit.ui.video.contract.VideoEditorContract
import com.naury.framekit.ui.video.contract.VideoEditorRequest
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 편집을 마친 사진이나 동영상.
 *
 * @property uris 앱 캐시에 옮겨 둔 JPEG·MP4 URI. 첨부·대표 사진·앨범의 가져오기에 그대로 넘기면 크기 조정, 위치 정보 제거,
 *   썸네일 생성이 기존과 같게 된다. 캐시 파일은 하루 뒤 정리된다.
 * @property tag 편집을 시작할 때 넘긴 값. 어느 첨부를 고치는 중이었는지처럼, 화면이 다시 만들어져도 알아야 하는 것을 담는다.
 */
data class EditedPhotos(val uris: List<String>, val tag: String? = null)

/**
 * 워터마크 도구에 미리 채울 문구([text])와 빠르게 고를 추천 문구([suggestions]). 사진에 얹을지는 사용자가 정한다.
 * 다른 사람에게 보여 줄 수 있으니 번호판·차대번호·이름처럼 차나 사람을 알아볼 수 있는 값은 넣지 않는다.
 */
@Immutable
data class WatermarkText(val text: String, val suggestions: List<String> = listOf(text))

/**
 * FrameKit 사진·동영상 편집기를 여는 손잡이. [rememberPhotoEditorLauncher]로 만든다.
 *
 * 편집기 결과 파일은 캐시로 옮긴 뒤 바로 지우고, 실패하면 짧은 안내를 띄운다. 닫기만 하면 아무것도 하지 않는다.
 * 앨범에서는 사진 한 장을 고칠 때 [edit], 골라서 넣을 때 [pickWithChooser], 여러 장으로 동영상을 만들 때 [makeSlideshow],
 * 다른 앱으로 보낼 때 [share]를 쓰면 된다. 동영상은 [editVideo]와 [pickAndEditVideo]를 쓴다.
 */
@Stable
class PhotoEditorLauncher internal constructor(private val open: (EditSource, Int, String?) -> Unit) {
    /** 앱 저장소의 사진 파일 한 장을 편집한다. 결과도 한 장이다. [watermark]는 워터마크 도구에 미리 채울 문구다. */
    fun edit(file: File, tag: String? = null, watermark: WatermarkText? = null) =
        open(PhotoEditSource.Existing(listOf(file.absolutePath), watermark), 1, tag)

    /**
     * 이미 고른 사진들을 한꺼번에 편집한다. `file://`·`content://` URI와 절대 경로를 받는다.
     *
     * @param maxItems 편집기 안에서 사진을 더해도 넘지 않을 결과 수.
     */
    fun edit(uris: List<String>, maxItems: Int = uris.size, tag: String? = null) {
        if (uris.isNotEmpty()) open(PhotoEditSource.Existing(uris), maxOf(maxItems, uris.size), tag)
    }

    /** 시스템 Photo Picker로 [maxItems]장까지 고르게 한 뒤 바로 편집한다. */
    fun pickAndEdit(maxItems: Int, tag: String? = null) {
        if (maxItems > 0) open(PhotoEditSource.Pick(maxItems), maxItems, tag)
    }

    /** 앱 저장소의 동영상 하나를 편집한다. 결과도 하나다. [watermark]는 워터마크 도구에 미리 채울 문구다. */
    fun editVideo(file: File, tag: String? = null, watermark: WatermarkText? = null) =
        open(VideoEditSource.Existing(file.absolutePath, watermark), 1, tag)

    /** 시스템 Photo Picker로 동영상 하나를 고르게 한 뒤 바로 편집한다. 결과는 MP4 하나다. */
    fun pickAndEditVideo(tag: String? = null) = open(VideoEditSource.Pick, 1, tag)

    /**
     * 사진·동영상을 [maxItems]개까지 고르게 한 뒤 편집할지, 사진으로 움직이는 영상을 만들지, 그대로 넣을지 묻는다.
     * 결과는 편집한 JPEG·MP4나 그대로 넣을 원본의 캐시 사본이다.
     */
    fun pickWithChooser(maxItems: Int, tag: String? = null) {
        if (maxItems > 0) open(ChooserPickSource(maxItems), maxItems, tag)
    }

    /** 앱 저장소의 사진 [files]로 슬라이드쇼 동영상(MP4 하나)을 만든다. 두 장 이상이어야 연다. */
    fun makeSlideshow(files: List<File>, tag: String? = null) {
        if (files.size >= MIN_SLIDESHOW_PHOTOS) open(SlideshowSource(files.map { it.absolutePath }), 1, tag)
    }

    /**
     * 앱 저장소의 사진·동영상 하나를 다른 앱으로 보낸다. 편집해서 보낼지 그대로 보낼지 먼저 묻고, 편집을 고르면
     * 워터마크 도구에 [watermark]가 채워져 있다. 결과는 공유 창으로 넘기고 [rememberPhotoEditorLauncher]의 결과로는 오지 않는다.
     */
    fun share(file: File, watermark: WatermarkText? = null) = open(ShareSource(file.absolutePath, watermark), 1, null)
}

/**
 * 앱 테마·언어에 맞춘 사진·동영상 편집기를 준비한다. 편집이 끝나면 [onEdited]가 메인 스레드에서 불린다.
 * 접거나 펴서, 또는 프로세스가 다시 만들어져도 결과를 받을 수 있게 화면 배치와 상관없는 자리에서 부른다.
 *
 * @param maxVideoDurationMs 동영상 편집 결과의 최대 길이. 더 긴 원본은 앞부분만 남긴 채로 열린다.
 */
@Composable
fun rememberPhotoEditorLauncher(
    maxVideoDurationMs: Long = DEFAULT_MAX_VIDEO_DURATION_MS,
    onEdited: (EditedPhotos) -> Unit,
): PhotoEditorLauncher {
    val context = LocalContext.current
    val launchers = rememberEditorLaunchers(onEdited)
    val ui = photoEditUi(MaterialTheme.colorScheme, stringResource(R.string.photo_edit_locale_tag))
    val latestUi by rememberUpdatedState(ui)
    val latestMaxVideo by rememberUpdatedState(maxVideoDurationMs)
    return remember(launchers) {
        PhotoEditorLauncher { source, maxItems, tag ->
            launchers.setTag(tag)
            when (source) {
                is PhotoEditSource -> context.launchEditor(
                    launchers.image,
                    photoEditRequest(source, maxItems, latestUi),
                )
                is VideoEditSource ->
                    context.launchEditor(launchers.video, videoEditRequest(source, latestMaxVideo, latestUi))
                is ChooserPickSource ->
                    context.launchEditor(launchers.chooser, chooserPickRequest(source, latestMaxVideo, latestUi))
                is SlideshowSource -> context.launchEditor(launchers.motion, slideshowRequest(source, latestUi))
                is ShareSource -> context.launchEditor(launchers.share, shareRequest(source, latestMaxVideo, latestUi))
            }
        }
    }
}

/** 편집기마다 결과 계약이 달라 따로 등록한다. 무엇을 고치던 중인지(tag)는 화면이 다시 만들어져도 남긴다. */
private class EditorLaunchers(
    val image: ManagedActivityResultLauncher<ImageEditorRequest, FrameKitResult>,
    val video: ManagedActivityResultLauncher<VideoEditorRequest, FrameKitResult>,
    val chooser: ManagedActivityResultLauncher<FrameKitRequest, FrameKitResult>,
    val motion: ManagedActivityResultLauncher<MotionEditorRequest, FrameKitResult>,
    val share: ManagedActivityResultLauncher<FrameKitRequest, FrameKitResult>,
    val setTag: (String?) -> Unit,
)

@Composable
private fun rememberEditorLaunchers(onEdited: (EditedPhotos) -> Unit): EditorLaunchers {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnEdited by rememberUpdatedState(onEdited)
    var pendingTag by rememberSaveable { mutableStateOf<String?>(null) }
    val handler = remember(context) { resultHandler(context.applicationContext) }
    val onResult: (FrameKitResult, Boolean) -> Unit = { result, isVideo ->
        val tag = pendingTag
        pendingTag = null
        scope.launch {
            when (val outcome = withContext(Dispatchers.IO) { handler.handle(result, isVideo) }) {
                is PhotoEditOutcome.Edited -> latestOnEdited(EditedPhotos(outcome.uris, tag))
                is PhotoEditOutcome.Failed -> context.showNotice(outcome.message)
                PhotoEditOutcome.Cancelled -> Unit
            }
        }
    }
    val shareTitle = stringResource(R.string.photo_share_title)
    val onShareResult: (FrameKitResult) -> Unit = { result ->
        pendingTag = null
        scope.launch {
            when (val outcome = withContext(Dispatchers.IO) { handler.handleShare(result) }) {
                is PhotoShareOutcome.Ready -> context.launchExternal {
                    context.startActivity(PhotoShare.chooser(outcome.uri, outcome.mimeType, shareTitle))
                }
                is PhotoShareOutcome.Failed -> context.showNotice(outcome.message)
                PhotoShareOutcome.Cancelled -> Unit
            }
        }
    }
    val image = rememberLauncherForActivityResult(ImageEditorContract()) { onResult(it, false) }
    val video = rememberLauncherForActivityResult(VideoEditorContract()) { onResult(it, true) }
    val chooser = rememberLauncherForActivityResult(FrameKitContract()) { onResult(it, false) }
    val motion = rememberLauncherForActivityResult(MotionEditorContract()) { onResult(it, true) }
    val share = rememberLauncherForActivityResult(FrameKitContract(), onShareResult)
    return remember(image, video, chooser, motion, share) {
        EditorLaunchers(image, video, chooser, motion, share) { pendingTag = it }
    }
}

private fun Context.showNotice(@StringRes message: Int) = ChageunToasts.error(getString(message))

private fun <I> Context.launchEditor(launcher: ManagedActivityResultLauncher<I, FrameKitResult>, request: I) {
    PhotoFiles.cleanStale(this)
    PhotoShare.cleanStale(this)
    launchExternal { launcher.launch(request) }
}

private fun resultHandler(context: Context) = PhotoEditResultHandler(
    stage = { uri, isVideo ->
        if (isVideo) PhotoFiles.copyIn(context, uri, PhotoFiles.VIDEO_EXTENSION) else PhotoFiles.copyIn(context, uri)
    },
    deleteOutput = { uri -> FrameKitOutputs.deleteOutput(context, uri) },
    stageShare = { uri, extension -> PhotoShare.copy(context, uri, extension) },
)

private const val MIN_SLIDESHOW_PHOTOS = 2

/** FrameKit 동영상 편집기의 기본 최대 길이(10분)와 같다. */
private const val DEFAULT_MAX_VIDEO_DURATION_MS = 10 * 60 * 1_000L
