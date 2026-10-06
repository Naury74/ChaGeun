package com.naury.chageun.feature.vehicle.album

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.album.MAX_ALBUM_PHOTOS_PER_ADD
import com.naury.chageun.core.model.AlbumPhoto
import com.naury.chageun.core.ui.AdaptiveListDetail
import com.naury.chageun.core.ui.CardGroup
import com.naury.chageun.core.ui.EmptyState
import com.naury.chageun.core.ui.FormLabel
import com.naury.chageun.core.ui.GroupDivider
import com.naury.chageun.core.ui.ListRow
import com.naury.chageun.core.ui.QuickDateField
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.isListDetailTwoPane
import com.naury.chageun.core.ui.photo.PhotoInput
import com.naury.chageun.core.ui.photo.rememberPhotoInputState
import com.naury.chageun.core.ui.rememberFileImage
import com.naury.chageun.feature.vehicle.R
import java.time.LocalDate

/**
 * 내 차 앨범. 사진 입력·확인 창은 배치(한 칸·두 칸)와 상관없는 이 자리에 두어 접거나 펴도 이어진다.
 */
@Composable
fun AlbumRoute(onBack: () -> Unit, viewModel: AlbumViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photoInput = rememberPhotoInputState()
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    PhotoInput(photoInput, maxItems = MAX_ALBUM_PHOTOS_PER_ADD, onPhotos = viewModel::add)

    LargeTitleScaffold(
        title = stringResource(R.string.album_title),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.album_back))
            }
        },
        actions = {
            IconButton(onClick = photoInput::open) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = stringResource(R.string.album_add))
            }
        },
    ) { padding ->
        AlbumScreen(
            uiState = uiState,
            isTwoPane = isListDetailTwoPane(),
            actions = AlbumActions(
                onSelect = viewModel::select,
                onAdd = photoInput::open,
                onEdit = viewModel::startEditing,
                onDelete = { deleting = it },
                onSetCover = viewModel::setAsCover,
                onDismissMessage = viewModel::dismissMessage,
            ),
            modifier = Modifier.padding(padding),
        )
    }

    uiState.editing?.let { photo ->
        DetailsDialog(
            photo = photo,
            onSave = { date, comment -> viewModel.saveDetails(photo.id, date, comment) },
            onDismiss = viewModel::stopEditing,
        )
    }
    deleting?.let { id ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.album_delete_title)) },
            text = { Text(stringResource(R.string.album_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(id)
                        deleting = null
                    },
                ) { Text(stringResource(R.string.album_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.album_cancel)) }
            },
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
)

@Composable
fun AlbumScreen(uiState: AlbumUiState, isTwoPane: Boolean, actions: AlbumActions, modifier: Modifier = Modifier) {
    val selected = uiState.selected
    when {
        uiState.isLoading -> Unit
        uiState.days.isEmpty() -> EmptyState(
            icon = Icons.Filled.PhotoLibrary,
            title = stringResource(R.string.album_empty_title),
            body = stringResource(R.string.album_empty_body),
            actionLabel = stringResource(R.string.album_add),
            onAction = actions.onAdd,
            modifier = modifier,
        )
        isTwoPane -> {
            BackHandler(enabled = selected != null) { actions.onSelect(null) }
            AdaptiveListDetail(
                selected = selected,
                list = { AlbumGrid(uiState, actions, Modifier.fillMaxSize()) },
                detail = { PhotoDetail(uiState, it, actions, showBack = false, modifier = Modifier.fillMaxSize()) },
                emptyDetail = {},
                modifier = modifier,
            )
        }
        selected != null -> {
            BackHandler { actions.onSelect(null) }
            PhotoDetail(uiState, selected, actions, showBack = true, modifier = modifier.fillMaxSize())
        }
        else -> AlbumGrid(uiState, actions, modifier.fillMaxSize())
    }
}

@Composable
private fun AlbumGrid(uiState: AlbumUiState, actions: AlbumActions, modifier: Modifier = Modifier) {
    val gutter = ChageunTheme.spacing.gutter
    LazyVerticalGrid(
        columns = GridCells.Adaptive(THUMBNAIL_MIN_SIZE),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = gutter, vertical = ChageunTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(GRID_GAP),
        verticalArrangement = Arrangement.spacedBy(GRID_GAP),
    ) {
        if (uiState.failedCount > 0) {
            item(key = "failed", span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        pluralStringResource(R.plurals.album_failed, uiState.failedCount, uiState.failedCount),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = actions.onDismissMessage) { Text(stringResource(R.string.album_close)) }
                }
            }
        }
        uiState.days.forEach { day ->
            item(key = "day-${day.date}", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.album_day, formatDate(day.date), day.photos.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = ChageunTheme.spacing.sm)
                        .semantics { heading() },
                )
            }
            items(day.photos, key = { it.id }) { photo ->
                Thumbnail(photo, isSelected = photo.id == uiState.selected?.id, onClick = {
                    actions.onSelect(photo.id)
                })
            }
        }
    }
}

@Composable
private fun Thumbnail(photo: AlbumPhoto, isSelected: Boolean, onClick: () -> Unit) {
    val image by rememberFileImage(photo.thumbnailPath)
    val description = photo.comment ?: stringResource(R.string.album_photo_on, formatDate(photo.takenOn))
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .background(if (isSelected) colors.primaryContainer else colors.surfaceVariant)
            .pressable(onClick = onClick, role = Role.Image, onClickLabel = description),
    ) {
        image?.let {
            Image(
                it,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isSelected) SELECTED_INSET else 0.dp)
                    .clip(MaterialTheme.shapes.small),
            )
        }
    }
}

/** 좌우로 넘겨 같은 앨범의 다른 사진을 본다. 넘기면 선택도 따라 바뀌어 두 칸에서 그리드 표시가 맞는다. */
@Composable
private fun PhotoDetail(
    uiState: AlbumUiState,
    photo: AlbumPhoto,
    actions: AlbumActions,
    showBack: Boolean,
    modifier: Modifier = Modifier,
) {
    val photos = uiState.photos
    val pager = rememberPagerState(initialPage = photos.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)) {
        photos.size
    }
    LaunchedEffect(photo.id) {
        val index = photos.indexOfFirst { it.id == photo.id }
        if (index >= 0 && index != pager.currentPage) pager.scrollToPage(index)
    }
    LaunchedEffect(pager, photos) {
        snapshotFlow { pager.settledPage }.collect { page -> photos.getOrNull(page)?.let { actions.onSelect(it.id) } }
    }
    Column(modifier.verticalScroll(rememberScrollState())) {
        if (showBack) {
            IconButton(onClick = { actions.onSelect(null) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.album_back))
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().aspectRatio(PHOTO_ASPECT)) { page ->
            val image by rememberFileImage(photos[page].filePath)
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
                image?.let {
                    Image(
                        it,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Column(
            Modifier.padding(ChageunTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        ) {
            Text(formatDate(photo.takenOn), style = MaterialTheme.typography.titleMedium)
            Text(
                photo.comment ?: stringResource(R.string.album_no_comment),
                style = MaterialTheme.typography.bodyLarge,
                color = if (photo.comment ==
                    null
                ) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (uiState.isCoverUpdated) {
                Text(stringResource(R.string.album_cover_updated), style = MaterialTheme.typography.bodySmall)
            }
            CardGroup(null) {
                ListRow(Icons.Filled.Edit, stringResource(R.string.album_edit_details), onClick = {
                    actions.onEdit(photo.id)
                })
                GroupDivider()
                ListRow(Icons.Filled.DirectionsCar, stringResource(R.string.album_set_cover), onClick = {
                    actions.onSetCover(photo)
                })
                GroupDivider()
                ListRow(Icons.Filled.DeleteOutline, stringResource(R.string.album_delete), onClick = {
                    actions.onDelete(photo.id)
                })
            }
        }
    }
}

@Composable
private fun DetailsDialog(photo: AlbumPhoto, onSave: (LocalDate, String) -> Unit, onDismiss: () -> Unit) {
    var date by rememberSaveable(photo.id) { mutableStateOf(photo.takenOn) }
    var comment by rememberSaveable(photo.id) { mutableStateOf(photo.comment.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.album_details_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm)) {
                FormLabel(stringResource(R.string.album_details_date))
                QuickDateField(date = date, onDateSelected = { date = it })
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it.take(MAX_COMMENT_LENGTH) },
                    label = { Text(stringResource(R.string.album_details_comment)) },
                    placeholder = { Text(stringResource(R.string.album_details_comment_hint)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(date, comment) }) { Text(stringResource(R.string.album_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.album_later)) } },
    )
}

private val THUMBNAIL_MIN_SIZE = 104.dp
private val GRID_GAP = 4.dp
private val SELECTED_INSET = 4.dp
private const val PHOTO_ASPECT = 4f / 3f
private const val MAX_COMMENT_LENGTH = 300
