package com.naury.chageun.core.ui.photo

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.naury.chageun.core.ui.ChageunOptionDialog
import com.naury.chageun.core.ui.DialogOption
import com.naury.chageun.core.ui.R
import com.naury.chageun.core.ui.launchExternal

/** [PhotoInput]을 여는 손잡이. 화면에서 "사진 추가" 버튼이 [open]을 부른다. */
@Stable
class PhotoInputState internal constructor(isChoosing: Boolean) {
    internal var isChoosing by mutableStateOf(isChoosing)

    fun open() {
        isChoosing = true
    }

    internal companion object {
        val Saver = Saver<PhotoInputState, Boolean>(save = { it.isChoosing }, restore = { PhotoInputState(it) })
    }
}

/** 사진 추가 창 아래쪽에 더하는 선택지. 누르면 창을 닫은 뒤 [onClick]을 부른다. 편집해서 넣기처럼 화면마다 다른 길에 쓴다. */
@Immutable
data class PhotoSourceOption(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun rememberPhotoInputState(): PhotoInputState = rememberSaveable(saver = PhotoInputState.Saver) {
    PhotoInputState(isChoosing = false)
}

/**
 * 사진 찍기와 앨범에서 고르기를 한곳에서 다룬다. 한 장이면 편집기를 거치고, 여러 장이면 바로 넘긴다.
 * 카메라는 시스템 카메라 앱에 맡기므로 CAMERA 권한이 필요 없다.
 *
 * @param onPhotos 가져올 사진 URI 목록과 고화질 저장 여부.
 * @param includeVideos true면 동영상도 고를 수 있다. 동영상은 자르기 편집기를 거치지 않고 바로 넘긴다.
 * @param extraOptions 사진 찍기·고르기 아래에 더할 선택지.
 * @param onPickFromGallery 고르기를 화면이 직접 맡을 때. 주면 이 창의 Photo Picker 대신 이것을 부른다.
 *   앨범처럼 고른 뒤 편집할지 묻는 편집기로 바로 넘길 때 쓴다.
 */
@Composable
fun PhotoInput(
    state: PhotoInputState,
    maxItems: Int,
    onPhotos: (List<String>, Boolean) -> Unit,
    showQualityOption: Boolean = false,
    includeVideos: Boolean = false,
    extraOptions: List<PhotoSourceOption> = emptyList(),
    onPickFromGallery: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) editing = pendingCapture
        pendingCapture = null
    }
    val handlePicked: (List<String>) -> Unit = { uris ->
        when {
            uris.isEmpty() -> Unit
            uris.size == 1 && !(includeVideos && context.isVideo(uris.single())) -> editing = uris.single()
            else -> onPhotos(uris.take(maxItems), false)
        }
    }
    val pickOne = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        handlePicked(listOfNotNull(uri?.toString()))
    }
    // PickMultipleVisualMedia는 최대 개수가 2 이상이어야 한다.
    val pickMany = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems.coerceAtLeast(2)),
    ) { uris -> handlePicked(uris.map { it.toString() }) }

    if (state.isChoosing) {
        SourceDialog(
            includeVideos = includeVideos,
            extraOptions = extraOptions.map { option ->
                option.copy(
                    onClick = {
                        state.isChoosing = false
                        option.onClick()
                    },
                )
            },
            onCamera = {
                state.isChoosing = false
                PhotoFiles.cleanStale(context)
                val uri = PhotoFiles.newCaptureUri(context)
                pendingCapture = uri.toString()
                context.launchExternal { camera.launch(uri) }
            },
            onGallery = {
                state.isChoosing = false
                if (onPickFromGallery != null) {
                    onPickFromGallery()
                } else {
                    PhotoFiles.cleanStale(context)
                    val type = if (includeVideos) {
                        ActivityResultContracts.PickVisualMedia.ImageAndVideo
                    } else {
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    }
                    val request = PickVisualMediaRequest(type)
                    context.launchExternal { if (maxItems > 1) pickMany.launch(request) else pickOne.launch(request) }
                }
            },
            onDismiss = { state.isChoosing = false },
        )
    }
    editing?.let { source ->
        PhotoEditor(
            sourceUri = source,
            showQualityOption = showQualityOption,
            onDone = { edited ->
                editing = null
                onPhotos(listOf(edited.uri), edited.highQuality)
            },
            onCancel = { editing = null },
        )
    }
}

@Composable
private fun SourceDialog(
    includeVideos: Boolean,
    extraOptions: List<PhotoSourceOption>,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    ChageunOptionDialog(
        title = stringResource(if (includeVideos) R.string.photo_source_title_media else R.string.photo_source_title),
        options = listOf(
            DialogOption(stringResource(R.string.photo_source_camera), onCamera, Icons.Filled.PhotoCamera),
            DialogOption(
                stringResource(
                    if (includeVideos) R.string.photo_source_gallery_media else R.string.photo_source_gallery,
                ),
                onGallery,
                Icons.Filled.PhotoLibrary,
            ),
        ) + extraOptions.map { DialogOption(it.label, it.onClick, it.icon) },
        cancelLabel = stringResource(R.string.photo_editor_cancel),
        onDismiss = onDismiss,
    )
}

/** Photo Picker가 준 URI가 동영상인지. 형식을 알 수 없으면 사진으로 보고 편집기에 맡긴다. */
private fun Context.isVideo(uri: String): Boolean =
    runCatching { contentResolver.getType(uri.toUri()) }.getOrNull()?.startsWith("video/") == true
