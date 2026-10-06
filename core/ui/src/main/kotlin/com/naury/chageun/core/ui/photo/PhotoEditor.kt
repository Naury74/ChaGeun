package com.naury.chageun.core.ui.photo

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.naury.chageun.core.designsystem.component.ChoicePill
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.R
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 편집을 마친 사진. [uri]는 앱 캐시의 JPEG다. */
data class EditedPhoto(val uri: String, val highQuality: Boolean)

/**
 * 사진 한 장을 돌리고 자르는 전체 화면 편집기. 자르기 영역은 이미지 비율 좌표로 보관해서
 * 접거나 펴서 화면 크기가 바뀌어도 같은 영역이 유지된다.
 *
 * @param showQualityOption 기록 첨부처럼 긴 변을 줄여 저장하는 곳에서만 고화질 저장을 고르게 한다.
 */
@Composable
fun PhotoEditor(
    sourceUri: String,
    onDone: (EditedPhoto) -> Unit,
    onCancel: () -> Unit,
    showQualityOption: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var quarterTurns by rememberSaveable(sourceUri) { mutableIntStateOf(0) }
    var aspect by rememberSaveable(sourceUri) { mutableStateOf(CropAspect.Free) }
    var crop by rememberSaveable(sourceUri, stateSaver = CropRectSaver) { mutableStateOf(CropRect.Full) }
    var highQuality by rememberSaveable(sourceUri) { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val upright by produceState<Bitmap?>(null, sourceUri) {
        value = withContext(Dispatchers.IO) { PhotoFiles.decodeUpright(context, sourceUri, PREVIEW_EDGE_PX) }
    }
    val preview = remember(upright, quarterTurns) {
        upright?.let { it.rotatedBy(quarterTurns * QUARTER_DEGREES).asImageBitmap() }
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = ChageunTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.photo_editor_cancel), color = Color.White)
                }
                Text(
                    stringResource(R.string.photo_editor_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    enabled = preview != null && !isSaving,
                    onClick = {
                        isSaving = true
                        scope.launch {
                            val uri = withContext(Dispatchers.IO) {
                                PhotoFiles.render(context, sourceUri, quarterTurns, crop)
                            }
                            isSaving = false
                            if (uri != null) onDone(EditedPhoto(uri, highQuality)) else onCancel()
                        }
                    },
                ) { Text(stringResource(R.string.photo_editor_done), color = Color.White) }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (preview == null || isSaving) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    CropArea(
                        image = preview,
                        crop = crop,
                        aspect = aspect,
                        onCropChanged = { crop = it },
                    )
                }
            }
            EditorControls(
                aspect = aspect,
                onAspectSelected = { selected ->
                    aspect = selected
                    preview?.let { crop = CropMath.centered(selected, it.width, it.height) }
                },
                onRotate = {
                    quarterTurns = (quarterTurns + 1) % TURNS_PER_CIRCLE
                    // 돌리면 가로세로가 바뀌므로 고른 비율로 다시 가운데에 맞춘다.
                    crop = upright?.let {
                        val swapped = quarterTurns % 2 == 1
                        val width = if (swapped) it.height else it.width
                        val height = if (swapped) it.width else it.height
                        CropMath.centered(aspect, width, height)
                    } ?: CropRect.Full
                },
                showQualityOption = showQualityOption,
                highQuality = highQuality,
                onHighQualityChanged = { highQuality = it },
            )
        }
    }
}

@Composable
private fun CropArea(image: ImageBitmap, crop: CropRect, aspect: CropAspect, onCropChanged: (CropRect) -> Unit) {
    val description = stringResource(R.string.photo_editor_crop_area)
    val handleRadius = with(LocalDensity.current) { HANDLE_TOUCH.toPx() }
    BoxWithConstraints(Modifier.fillMaxSize().padding(ChageunTheme.spacing.lg), contentAlignment = Alignment.Center) {
        val scale = min(constraints.maxWidth.toFloat() / image.width, constraints.maxHeight.toFloat() / image.height)
        val shown = Size(image.width * scale, image.height * scale)
        // 제스처는 처음 한 번만 붙으므로 콜백 안에서 최신 영역을 읽는다.
        val latestCrop by rememberUpdatedState(crop)
        var handle by remember { mutableStateOf<CropHandle?>(null) }
        Box(
            Modifier
                .size(
                    with(LocalDensity.current) {
                        shown.width.toDp()
                    },
                    with(LocalDensity.current) { shown.height.toDp() },
                )
                .semantics { contentDescription = description }
                .pointerInput(image, aspect) {
                    detectDragGestures(
                        onDragStart = { start -> handle = pickHandle(latestCrop, start, shown, handleRadius) },
                        onDragEnd = { handle = null },
                        onDrag = { change, amount ->
                            val active = handle ?: return@detectDragGestures
                            change.consume()
                            onCropChanged(
                                CropMath.drag(
                                    latestCrop,
                                    active,
                                    amount.x / shown.width,
                                    amount.y / shown.height,
                                    aspect,
                                    image.width,
                                    image.height,
                                ),
                            )
                        },
                    )
                },
        ) {
            Image(
                image,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            Canvas(Modifier.fillMaxSize()) {
                val rect = Rect(
                    crop.left * size.width,
                    crop.top * size.height,
                    crop.right * size.width,
                    crop.bottom * size.height,
                )
                val dim = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(Offset.Zero, size))
                    addRect(rect)
                }
                drawPath(dim, Color.Black.copy(alpha = DIM_ALPHA))
                drawRect(Color.White, rect.topLeft, rect.size, style = Stroke(width = 2.dp.toPx()))
                val corner = CORNER_SIZE.toPx()
                listOf(rect.topLeft, rect.topRight, rect.bottomLeft, rect.bottomRight).forEach { point ->
                    drawRoundRect(
                        Color.White,
                        topLeft = Offset(point.x - corner / 2, point.y - corner / 2),
                        size = Size(corner, corner),
                        cornerRadius = CornerRadius(CORNER_RADIUS.toPx()),
                    )
                }
            }
        }
    }
}

private fun pickHandle(crop: CropRect, point: Offset, shown: Size, radius: Float): CropHandle? {
    val corners = mapOf(
        CropHandle.TopLeft to Offset(crop.left * shown.width, crop.top * shown.height),
        CropHandle.TopRight to Offset(crop.right * shown.width, crop.top * shown.height),
        CropHandle.BottomLeft to Offset(crop.left * shown.width, crop.bottom * shown.height),
        CropHandle.BottomRight to Offset(crop.right * shown.width, crop.bottom * shown.height),
    )
    corners.entries.firstOrNull { (_, corner) -> abs(corner.x - point.x) < radius && abs(corner.y - point.y) < radius }
        ?.let { return it.key }
    val inside = point.x / shown.width in crop.left..crop.right && point.y / shown.height in crop.top..crop.bottom
    return if (inside) CropHandle.Move else null
}

@Composable
private fun EditorControls(
    aspect: CropAspect,
    onAspectSelected: (CropAspect) -> Unit,
    onRotate: () -> Unit,
    showQualityOption: Boolean,
    highQuality: Boolean,
    onHighQualityChanged: (Boolean) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(ChageunTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                items(CropAspect.entries) { option ->
                    ChoicePill(stringResource(option.labelRes), aspect == option, { onAspectSelected(option) })
                }
            }
            FilledTonalIconButton(onClick = onRotate) {
                Icon(
                    Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = stringResource(R.string.photo_editor_rotate),
                )
            }
        }
        if (showQualityOption) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = ChageunTheme.spacing.minTouchTarget)
                    .toggleable(value = highQuality, role = Role.Switch, onValueChange = onHighQualityChanged),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.photo_editor_high_quality), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.photo_editor_high_quality_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = highQuality, onCheckedChange = null)
            }
        }
    }
}

private val CropAspect.labelRes: Int
    get() = when (this) {
        CropAspect.Free -> R.string.photo_editor_aspect_free
        CropAspect.Square -> R.string.photo_editor_aspect_square
        CropAspect.FourThree -> R.string.photo_editor_aspect_4_3
        CropAspect.SixteenNine -> R.string.photo_editor_aspect_16_9
    }

private fun Bitmap.rotatedBy(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private val CropRectSaver = Saver<CropRect, List<Float>>(
    save = { listOf(it.left, it.top, it.right, it.bottom) },
    restore = { values ->
        val (start, end) = values.chunked(2)
        CropRect(start.first(), start.last(), end.first(), end.last())
    },
)

private const val PREVIEW_EDGE_PX = 2_048
private const val QUARTER_DEGREES = 90
private const val TURNS_PER_CIRCLE = 4
private const val DIM_ALPHA = 0.55f
private val HANDLE_TOUCH = 32.dp
private val CORNER_SIZE = 18.dp
private val CORNER_RADIUS = 4.dp
