package com.naury.chageun.feature.vehicle.album

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.ChageunToasts
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.album.MAX_ALBUM_PHOTOS_PER_ADD
import com.naury.chageun.core.domain.album.MAX_ALBUM_VIDEO_DURATION_MS
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.ui.AdaptiveListDetail
import com.naury.chageun.core.ui.ChageunDialog
import com.naury.chageun.core.ui.ChageunTextField
import com.naury.chageun.core.ui.DialogAction
import com.naury.chageun.core.ui.DialogActionStyle
import com.naury.chageun.core.ui.FormLabel
import com.naury.chageun.core.ui.QuickDateField
import com.naury.chageun.core.ui.isListDetailTwoPane
import com.naury.chageun.core.ui.photo.PhotoInput
import com.naury.chageun.core.ui.photo.WatermarkText
import com.naury.chageun.core.ui.photo.rememberPhotoEditorLauncher
import com.naury.chageun.core.ui.photo.rememberPhotoInputState
import com.naury.chageun.feature.vehicle.ActionButton
import com.naury.chageun.feature.vehicle.R
import com.naury.kinetica.ui.KEmptyState
import com.naury.kinetica.ui.KIconButton
import com.naury.kinetica.ui.KStaggerState
import com.naury.kinetica.ui.KTone
import com.naury.kinetica.ui.KVariant
import com.naury.kinetica.ui.rememberKStaggerState
import java.io.File
import java.time.LocalDate

/**
 * 내 차 앨범. 사진 입력·확인 창은 배치(한 칸·두 칸)와 상관없는 이 자리에 두어 접거나 펴도 이어진다.
 * [focusPhotoId]가 있으면 그 사진을 열어 둔 채로 시작한다. 달력에서 사진을 눌러 들어올 때 쓴다.
 */
@Composable
fun AlbumRoute(
    onBack: () -> Unit,
    focusPhotoId: String? = null,
    onOpenCalendar: ((LocalDate) -> Unit)? = null,
    /** 한 칸 화면에서 사진 보기를 열고 닫을 때 알린다. 앱은 이때 하단 탭 바를 숨긴다. */
    onImmersiveChange: (Boolean) -> Unit = {},
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photoInput = rememberPhotoInputState()
    // 확인 창을 띄운 채 회전해도 남도록 지울 사진 id를 저장한다. 되돌릴 수 있어 많이 지울 때만 묻는다.
    var deleting by rememberSaveable { mutableStateOf<List<String>?>(null) }
    // 처음 열 때 한 번만 고른다. 그 뒤 사용자가 다른 사진으로 넘기거나 닫으면 그대로 둔다.
    var isFocusHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(focusPhotoId) {
        if (focusPhotoId != null && !isFocusHandled) {
            viewModel.select(focusPhotoId)
            isFocusHandled = true
        }
    }
    // 사진 편집기는 접거나 펴도 결과를 받을 수 있게 배치와 상관없는 이 자리에서 연다. 무엇을 고치는지는 태그로 남긴다.
    val editor = rememberPhotoEditorLauncher(maxVideoDurationMs = MAX_ALBUM_VIDEO_DURATION_MS) {
        AlbumEditTag.apply(it, viewModel)
    }
    // 고른 뒤 편집할지, 사진으로 움직이는 영상을 만들지, 그대로 넣을지는 FrameKit 선택 창이 묻는다.
    PhotoInput(
        photoInput,
        maxItems = MAX_ALBUM_PHOTOS_PER_ADD,
        onPhotos = viewModel::add,
        includeVideos = true,
        onPickFromGallery = { editor.pickWithChooser(MAX_ALBUM_PHOTOS_PER_ADD, AlbumEditTag.ADD) },
    )
    val watermarkLabels = rememberWatermarkLabels()
    val watermarkFor: (AlbumPhoto) -> WatermarkText = { photo ->
        albumWatermark(uiState.vehicle, photo.takenOn, uiState.mileageLog, watermarkLabels)
    }
    val slideshowNote = stringResource(R.string.album_slideshow_videos_skipped)
    val makeSlideshow = {
        val photos = uiState.slideshowPhotos()
        if (uiState.hasPickedVideos) ChageunToasts.show(slideshowNote)
        val takenOn = slideshowDate(photos, LocalDate.now())
        editor.makeSlideshow(photos.map { File(it.filePath) }, AlbumEditTag.slideshow(takenOn))
        viewModel.picks.clear()
    }

    FinishDeleteWhenLeaving(viewModel.deletes::finish)
    val isTwoPane = isListDetailTwoPane()
    val isImmersive = !isTwoPane && uiState.selected != null
    val currentOnImmersiveChange by rememberUpdatedState(onImmersiveChange)
    DisposableEffect(isImmersive) {
        currentOnImmersiveChange(isImmersive)
        onDispose { currentOnImmersiveChange(false) }
    }
    // 한 칸 화면에서 사진을 열었다 닫아도 그리드가 보던 자리에 머물고, 사진이 다시 떠오르지 않게 여기서 기억한다.
    val gridState = rememberLazyGridState()
    val stagger = rememberKStaggerState()
    val actions = AlbumActions(
        onSelect = viewModel::select,
        onAdd = photoInput::open,
        onEdit = viewModel::editDetails,
        onDelete = { viewModel.delete(listOf(it)) },
        onSetCover = viewModel::setAsCover,
        onDismissMessage = viewModel::dismissMessage,
        onOpenCalendar = onOpenCalendar,
        onTogglePick = viewModel.picks::toggle,
        onPickDay = viewModel::pickDay,
        onZoomGrid = viewModel::zoomGrid,
        onEditImage = { photo ->
            val tag = AlbumEditTag.replace(photo.id)
            val file = File(photo.filePath)
            val watermark = watermarkFor(photo)
            if (photo.isVideo) editor.editVideo(file, tag, watermark) else editor.edit(file, tag, watermark)
        },
        onShare = { photo -> editor.share(File(photo.filePath), watermarkFor(photo)) },
        onUndoDelete = viewModel.deletes::undo,
        onFinishDelete = viewModel.deletes::finish,
    )
    if (!isTwoPane && uiState.selected != null) {
        // 사진 보기는 제목 바 없이 상태 표시줄 뒤까지 그린다.
        AlbumScreen(uiState, isTwoPane = false, actions = actions, modifier = Modifier.fillMaxSize())
    } else {
        BackHandler(enabled = uiState.isPicking, onBack = viewModel.picks::clear)
        AlbumListScaffold(
            uiState = uiState,
            bar = AlbumBarActions(
                onBack = onBack,
                onClearPicks = viewModel.picks::clear,
                onAdd = photoInput::open,
                onMakeSlideshow = makeSlideshow,
                onDeletePicked = {
                    val ids = uiState.picked.toList()
                    if (ids.size >= CONFIRM_DELETE_COUNT) deleting = ids else viewModel.delete(ids)
                },
            ),
        ) { padding ->
            AlbumScreen(uiState, isTwoPane, actions, Modifier.padding(padding), gridState, stagger)
        }
    }

    uiState.editing?.let { photo ->
        DetailsDialog(
            photo = photo,
            onSave = { date, comment -> viewModel.saveDetails(photo.id, date, comment) },
            onDismiss = { viewModel.editDetails(null) },
        )
    }
    deleting?.let { ids ->
        DeleteDialog(
            count = ids.size,
            isVideo = ids.singleOrNull()?.let { id -> uiState.photos.firstOrNull { it.id == id }?.isVideo } == true,
            onConfirm = {
                viewModel.delete(ids)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

data class AlbumActions(
    val onSelect: (String?) -> Unit = {},
    val onAdd: () -> Unit = {},
    val onEdit: (String) -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onSetCover: (AlbumPhoto) -> Unit = {},
    val onDismissMessage: () -> Unit = {},
    /** 사진 날짜를 누르면 기록 탭 달력의 그날로 간다. null이면 날짜는 글자로만 보인다. */
    val onOpenCalendar: ((LocalDate) -> Unit)? = null,
    /** 사진·동영상 자체를 고치는 편집기. 붙이지 않았으면 null이고 편집 버튼을 숨긴다. */
    val onEditImage: ((AlbumPhoto) -> Unit)? = null,
    /** 길게 눌러 여러 장 고르기. 고르는 중에는 사진을 누를 때도 이것을 부른다. */
    val onTogglePick: (String) -> Unit = {},
    val onPickDay: (LocalDate) -> Unit = {},
    /** 두 손가락으로 그리드를 벌리면 1, 오므리면 -1. */
    val onZoomGrid: (Int) -> Unit = {},
    /** 다른 앱으로 보내기. 편집기를 붙이지 않았으면 null이고 공유 버튼을 숨긴다. */
    val onShare: ((AlbumPhoto) -> Unit)? = null,
    /** 방금 지운 사진을 되살린다. */
    val onUndoDelete: () -> Unit = {},
    /** 되돌리기 안내 시간이 끝나 정말 지운다. */
    val onFinishDelete: () -> Unit = {},
)

/** 그리드 화면 위쪽 버튼이 하는 일. */
data class AlbumBarActions(
    val onBack: () -> Unit = {},
    val onClearPicks: () -> Unit = {},
    val onAdd: () -> Unit = {},
    val onMakeSlideshow: () -> Unit = {},
    val onDeletePicked: () -> Unit = {},
)

/**
 * 그리드 화면의 큰 제목 틀. 여러 장 고르는 중에는 제목이 고른 수로 바뀌고, 뒤로 대신 선택 취소, 추가 대신
 * 슬라이드쇼·지우기 버튼이 놓인다.
 */
@Composable
internal fun AlbumListScaffold(
    uiState: AlbumUiState,
    bar: AlbumBarActions,
    content: @Composable (PaddingValues) -> Unit,
) {
    LargeTitleScaffold(
        title = if (uiState.isPicking) {
            pluralStringResource(R.plurals.album_picked_count, uiState.picked.size, uiState.picked.size)
        } else {
            stringResource(R.string.album_title)
        },
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
        navigationIcon = {
            if (uiState.isPicking) {
                BarIconButton(Icons.Filled.Close, stringResource(R.string.album_clear_picks), bar.onClearPicks)
            } else {
                BarIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.album_back), bar.onBack)
            }
        },
        actions = {
            if (uiState.isPicking) {
                PickingActions(
                    canMakeSlideshow = uiState.slideshowPhotos().size >= MIN_SLIDESHOW_PHOTOS,
                    onMakeSlideshow = bar.onMakeSlideshow,
                    onDelete = bar.onDeletePicked,
                )
            } else {
                BarIconButton(Icons.Filled.AddAPhoto, stringResource(R.string.album_add), bar.onAdd)
            }
        },
        content = content,
    )
}

/**
 * 여러 장 고르는 중의 위쪽 버튼. 슬라이드쇼는 사진이 두 장 이상 골라졌을 때만 누를 수 있다.
 * 지우기는 잠깐 뒤에는 되돌릴 수 없어 위험 색으로 둔다.
 */
@Composable
private fun PickingActions(canMakeSlideshow: Boolean, onMakeSlideshow: () -> Unit, onDelete: () -> Unit) {
    BarIconButton(
        Icons.Filled.MovieCreation,
        stringResource(R.string.album_make_slideshow),
        onMakeSlideshow,
        enabled = canMakeSlideshow,
    )
    BarIconButton(
        Icons.Filled.DeleteOutline,
        stringResource(R.string.album_delete_picked),
        onDelete,
        tone = KTone.Destructive,
    )
}

/** 제목 줄의 아이콘 버튼. 바탕 없이 아이콘만 두고 누르는 동안 원 모양으로 옅게 덮인다. */
@Composable
private fun BarIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tone: KTone = KTone.Neutral,
) {
    KIconButton(
        icon,
        description,
        onClick,
        variant = KVariant.Text,
        tone = tone,
        size = BAR_BUTTON,
        enabled = enabled,
        contentColor = if (tone == KTone.Neutral) MaterialTheme.colorScheme.onSurface else Color.Unspecified,
    )
}

private val BAR_BUTTON = 48.dp

/**
 * 앨범 본문. 사진을 지운 뒤에는 아래에 되돌리기 막대를 띄운다. 한 칸 화면의 사진 보기는 아래 조작 막대를 피해
 * 사진 보기 안에서 띄운다.
 */
@Composable
fun AlbumScreen(
    uiState: AlbumUiState,
    isTwoPane: Boolean,
    actions: AlbumActions,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    stagger: KStaggerState? = null,
) {
    val isViewerOnly = !isTwoPane && uiState.selected != null
    Box(modifier) {
        AlbumBody(uiState, isTwoPane, actions, Modifier.fillMaxSize(), gridState, stagger)
        if (!isViewerOnly) {
            AlbumUndoBar(
                uiState.undo,
                actions,
                Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            )
        }
    }
}

@Composable
private fun AlbumBody(
    uiState: AlbumUiState,
    isTwoPane: Boolean,
    actions: AlbumActions,
    modifier: Modifier,
    gridState: LazyGridState,
    stagger: KStaggerState?,
) {
    val selected = uiState.selected
    when {
        uiState.isLoading -> Unit
        uiState.days.isEmpty() -> Box(modifier, contentAlignment = Alignment.TopCenter) {
            KEmptyState(
                title = stringResource(R.string.album_empty_title),
                message = stringResource(R.string.album_empty_body),
                art = {
                    Icon(
                        Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(EMPTY_ART),
                    )
                },
                action = {
                    ActionButton(
                        stringResource(R.string.album_add),
                        onClick = actions.onAdd,
                        modifier = Modifier.heightIn(min = ChageunTheme.spacing.minTouchTarget),
                        variant = KVariant.Tonal,
                        icon = Icons.Filled.AddAPhoto,
                    )
                },
            )
        }
        isTwoPane -> {
            BackHandler(enabled = selected != null) { actions.onSelect(null) }
            val spacing = ChageunTheme.spacing
            AdaptiveListDetail(
                selected = selected,
                list = { AlbumGrid(uiState, actions, Modifier.fillMaxSize(), gridState, stagger) },
                detail = {
                    PhotoViewer(
                        uiState,
                        it,
                        actions,
                        immersive = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = spacing.gutter, bottom = spacing.gutter)
                            .clip(MaterialTheme.shapes.large),
                    )
                },
                emptyDetail = {},
                modifier = modifier,
            )
        }
        selected != null -> {
            BackHandler { actions.onSelect(null) }
            PhotoViewer(uiState, selected, actions, immersive = true, modifier = modifier.fillMaxSize())
        }
        else -> AlbumGrid(uiState, actions, modifier.fillMaxSize(), gridState, stagger)
    }
}

/** 한 장이든 여러 장이든 같은 창으로 묻는다. 되돌릴 수 없으므로 위험 색으로 표시한다. */
@Composable
private fun DeleteDialog(count: Int, isVideo: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ChageunDialog(
        title = when {
            count > 1 -> pluralStringResource(R.plurals.album_delete_many_title, count, count)
            isVideo -> stringResource(R.string.album_delete_video_title)
            else -> stringResource(R.string.album_delete_title)
        },
        message = stringResource(R.string.album_delete_body),
        icon = Icons.Filled.DeleteOutline,
        tone = ChageunTheme.colors.critical,
        onDismiss = onDismiss,
        actions = listOf(
            DialogAction(stringResource(R.string.album_delete), DialogActionStyle.Destructive, onClick = onConfirm),
            DialogAction(stringResource(R.string.album_cancel), DialogActionStyle.Secondary, onClick = onDismiss),
        ),
    )
}

@Composable
private fun DetailsDialog(photo: AlbumPhoto, onSave: (LocalDate, String) -> Unit, onDismiss: () -> Unit) {
    var date by rememberSaveable(photo.id) { mutableStateOf(photo.takenOn) }
    var comment by rememberSaveable(photo.id) { mutableStateOf(photo.comment.orEmpty()) }
    ChageunDialog(
        title = stringResource(R.string.album_details_title),
        icon = Icons.Filled.EditNote,
        onDismiss = onDismiss,
        actions = listOf(
            DialogAction(stringResource(R.string.album_save), onClick = { onSave(date, comment) }),
            DialogAction(stringResource(R.string.album_later), DialogActionStyle.Secondary, onClick = onDismiss),
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm)) {
            FormLabel(stringResource(R.string.album_details_date))
            // 다른 날을 고르면 차근 날짜 선택 창(ChageunDatePickerDialog)이 뜬다.
            QuickDateField(date = date, onDateSelected = { date = it })
            ChageunTextField(
                value = comment,
                onValueChange = { comment = it.take(MAX_COMMENT_LENGTH) },
                label = stringResource(R.string.album_details_comment),
                placeholder = stringResource(R.string.album_details_comment_hint),
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val MAX_COMMENT_LENGTH = 300
private val EMPTY_ART = 56.dp

/** 이보다 적게 고른 사진은 묻지 않고 지운다. 지운 뒤 되돌리기 막대로 되살릴 수 있다. */
private const val CONFIRM_DELETE_COUNT = 5
