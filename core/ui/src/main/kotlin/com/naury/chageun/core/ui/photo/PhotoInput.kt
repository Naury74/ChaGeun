package com.naury.chageun.core.ui.photo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.naury.chageun.core.designsystem.theme.ChageunTheme
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

@Composable
fun rememberPhotoInputState(): PhotoInputState = rememberSaveable(saver = PhotoInputState.Saver) {
    PhotoInputState(isChoosing = false)
}

/**
 * 사진 찍기와 앨범에서 고르기를 한곳에서 다룬다. 한 장이면 편집기를 거치고, 여러 장이면 바로 넘긴다.
 * 카메라는 시스템 카메라 앱에 맡기므로 CAMERA 권한이 필요 없다.
 *
 * @param onPhotos 가져올 사진 URI 목록과 고화질 저장 여부.
 */
@Composable
fun PhotoInput(
    state: PhotoInputState,
    maxItems: Int,
    onPhotos: (List<String>, Boolean) -> Unit,
    showQualityOption: Boolean = false,
) {
    val context = LocalContext.current
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) editing = pendingCapture
        pendingCapture = null
    }
    val handlePicked: (List<String>) -> Unit = { uris ->
        when (uris.size) {
            0 -> Unit
            1 -> editing = uris.single()
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
            onCamera = {
                state.isChoosing = false
                PhotoFiles.cleanStale(context)
                val uri = PhotoFiles.newCaptureUri(context)
                pendingCapture = uri.toString()
                context.launchExternal { camera.launch(uri) }
            },
            onGallery = {
                state.isChoosing = false
                PhotoFiles.cleanStale(context)
                val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                context.launchExternal { if (maxItems > 1) pickMany.launch(request) else pickOne.launch(request) }
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
private fun SourceDialog(onCamera: () -> Unit, onGallery: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.photo_source_title)) },
        text = {
            Column {
                SourceRow(Icons.Filled.PhotoCamera, stringResource(R.string.photo_source_camera), onCamera)
                SourceRow(Icons.Filled.PhotoLibrary, stringResource(R.string.photo_source_gallery), onGallery)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.photo_editor_cancel)) } },
    )
}

@Composable
private fun SourceRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ChageunTheme.spacing.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = ChageunTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = ChageunTheme.spacing.md),
        )
    }
}
