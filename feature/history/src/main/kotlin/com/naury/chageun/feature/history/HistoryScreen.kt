package com.naury.chageun.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.history.MAX_ATTACHMENTS_PER_RECORD
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.ui.AdaptiveListDetail
import com.naury.chageun.core.ui.isListDetailTwoPane
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.photo.PhotoInput
import com.naury.chageun.core.ui.photo.rememberPhotoInputState
import com.naury.chageun.feature.history.form.CheckFormHost
import com.naury.chageun.feature.history.form.FuelFormHost

private enum class AddDialog { Chooser, MaintenanceItem, Fuel, Check }

@Composable
fun HistoryRoute(
    onRecordService: (MaintenanceItem) -> Unit,
    onEditService: (MaintenanceItem, String) -> Unit = { _, _ -> },
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isExpanded = currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val isTwoPane = isListDetailTwoPane()
    var dialog by rememberSaveable { mutableStateOf<AddDialog?>(null) }
    var isFilterOpen by rememberSaveable { mutableStateOf(false) }
    // 상세는 한 칸·두 칸 배치에 따라 컴포지션 위치가 달라 접거나 펼 때 상태가 사라진다.
    // 사진 선택·편집은 배치와 상관없는 이 자리에 두어, 편집 도중 접어도 이어지게 한다.
    val photoInput = rememberPhotoInputState()
    var photoTarget by rememberSaveable { mutableStateOf<String?>(null) }
    PhotoInput(
        state = photoInput,
        maxItems = (MAX_ATTACHMENTS_PER_RECORD - uiState.attachments.size).coerceAtLeast(1),
        // 영수증의 작은 글씨를 남길 수 있게 첨부에서는 고화질 저장을 고르게 한다.
        showQualityOption = true,
        onPhotos = { uris, highQuality ->
            photoTarget?.toRecordRef()?.let { viewModel.attach(it, uris, highQuality) }
        },
    )
    // RecordRef는 저장할 수 없으므로 종류와 ID를 나눠 보관한다.
    var editingType by rememberSaveable { mutableStateOf<TimelineEventType?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val stopEditing = {
        editingType = null
        editingId = null
    }
    val itemLabels = MaintenanceItem.entries.associateWith { stringResource(it.labelRes) }

    LargeTitleScaffold(
        title = stringResource(R.string.history_title).takeUnless {
            !isTwoPane &&
                uiState.selected != null
        },
    ) { padding ->
        HistoryScreen(
            uiState = uiState,
            isTwoPane = isTwoPane,
            onKeywordChanged = { keyword ->
                val matching = if (keyword.isBlank()) {
                    emptySet()
                } else {
                    itemLabels.filterValues {
                        it.contains(keyword.trim(), ignoreCase = true)
                    }.keys
                }
                viewModel.search(keyword, matching)
            },
            onFilterSelected = viewModel::selectFilter,
            onSelect = viewModel::select,
            onDelete = viewModel::delete,
            onEdit = { detail ->
                if (detail is RecordDetail.Maintenance) {
                    onEditService(detail.item, detail.ref.id)
                } else {
                    editingType = detail.ref.type
                    editingId = detail.ref.id
                }
            },
            onAdd = { dialog = AddDialog.Chooser },
            onOpenAdvancedFilter = { isFilterOpen = true },
            onClearAdvancedFilter = { viewModel.applyAdvancedFilter(AdvancedFilter()) },
            onLoadMore = viewModel::loadMore,
            onAddPhotos = { ref ->
                photoTarget = "${ref.type.name}:${ref.id}"
                photoInput.open()
            },
            onDeleteAttachment = viewModel::deleteAttachment,
            onDismissAttachFailure = viewModel::dismissAttachFailure,
            modifier = Modifier.padding(padding),
        )
    }

    if (isFilterOpen) {
        AdvancedFilterSheet(
            current = uiState.advanced,
            isExpanded = isExpanded,
            onApply = viewModel::applyAdvancedFilter,
            onDismiss = { isFilterOpen = false },
        )
    }
    when (dialog) {
        AddDialog.Chooser -> AddRecordChooser(onPick = { dialog = it }, onDismiss = { dialog = null })
        AddDialog.MaintenanceItem -> MaintenanceItemPicker(
            onPick = {
                dialog = null
                onRecordService(it)
            },
            onDismiss = { dialog = null },
        )
        AddDialog.Fuel -> FuelFormHost(isExpanded = isExpanded, onDismiss = { dialog = null })
        AddDialog.Check -> CheckFormHost(isExpanded = isExpanded, onDismiss = { dialog = null })
        null -> Unit
    }
    val editing = editingType?.let { type -> editingId?.let { RecordRef(type, it) } }
    when (editing?.type) {
        TimelineEventType.Fuel -> FuelFormHost(isExpanded, onDismiss = stopEditing, editingId = editing.id)
        TimelineEventType.Inspection, TimelineEventType.Repair, TimelineEventType.Note ->
            CheckFormHost(isExpanded, onDismiss = stopEditing, editing = editing)
        TimelineEventType.Maintenance, null -> Unit
    }
}

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    isTwoPane: Boolean,
    onKeywordChanged: (String) -> Unit,
    onFilterSelected: (HistoryFilter) -> Unit,
    onSelect: (RecordRef?) -> Unit,
    onDelete: (RecordRef) -> Unit,
    onAdd: () -> Unit,
    onAddPhotos: (RecordRef) -> Unit,
    onDeleteAttachment: (String) -> Unit,
    onDismissAttachFailure: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAdvancedFilter: () -> Unit = {},
    onClearAdvancedFilter: () -> Unit = {},
    onEdit: (RecordDetail) -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    val attachments =
        AttachmentsState(
            uiState.attachments,
            uiState.attachFailedCount,
            onAddPhotos,
            onDeleteAttachment,
            onDismissAttachFailure,
        )
    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val detail = uiState.detail
    // 접거나 펼쳐 한 칸·두 칸이 바뀌면 상세가 컴포지션의 다른 자리로 간다. 상태째 옮겨야 삭제 확인 같은
    // 상세 안의 상태가 사라지지 않는다. 다른 기록을 열면 key로 상태를 새로 시작한다.
    val currentAttachments by rememberUpdatedState(attachments)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnEdit by rememberUpdatedState(onEdit)
    val detailPane = remember {
        movableContentOf { shown: RecordDetail, paneModifier: Modifier ->
            key(shown.ref) {
                RecordDetailPane(
                    shown,
                    currentAttachments,
                    onBack = { currentOnSelect(null) },
                    onDelete = currentOnDelete,
                    modifier = paneModifier,
                    onEdit = currentOnEdit,
                )
            }
        }
    }
    val timeline: @Composable (Modifier) -> Unit = { paneModifier ->
        Box(paneModifier) {
            // 넓은 창에서 목록만 있을 때 한 줄이 너무 길어지지 않도록 가운데에 폭을 제한해 둔다.
            TimelineContent(
                uiState,
                onKeywordChanged,
                onFilterSelected,
                // 두 칸에서는 같은 기록을 다시 누르면 상세를 닫고 목록을 넓게 되돌린다.
                { ref -> onSelect(if (isTwoPane && ref == uiState.selected) null else ref) },
                onAdd,
                onOpenAdvancedFilter = onOpenAdvancedFilter,
                onClearAdvancedFilter = onClearAdvancedFilter,
                onLoadMore = onLoadMore,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = TIMELINE_MAX_WIDTH)
                    .align(Alignment.TopCenter),
            )
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.history_add)) },
                // 다른 주요 버튼(Tonal)과 같은 색을 써서 다크 모드에서도 같은 무게로 보이게 한다.
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ChageunTheme.spacing.gutter),
            )
        }
    }
    when {
        isTwoPane -> {
            BackHandler(enabled = detail != null) { onSelect(null) }
            AdaptiveListDetail(
                selected = detail,
                list = { timeline(Modifier.fillMaxSize()) },
                detail = { detailPane(it, Modifier.fillMaxSize()) },
                emptyDetail = { DetailPlaceholder(Modifier.fillMaxSize()) },
                modifier = modifier,
            )
        }
        detail != null -> {
            BackHandler { onSelect(null) }
            detailPane(detail, modifier.fillMaxSize())
        }
        else -> timeline(modifier.fillMaxSize())
    }
}

@Composable
private fun AddRecordChooser(onPick: (AddDialog) -> Unit, onDismiss: () -> Unit) {
    OptionDialog(
        title = stringResource(R.string.history_add),
        options = listOf(
            AddDialog.MaintenanceItem to stringResource(R.string.history_add_maintenance),
            AddDialog.Fuel to stringResource(R.string.history_add_fuel),
            AddDialog.Check to stringResource(R.string.history_add_check),
        ),
        onPick = onPick,
        onDismiss = onDismiss,
    )
}

@Composable
private fun MaintenanceItemPicker(onPick: (MaintenanceItem) -> Unit, onDismiss: () -> Unit) {
    OptionDialog(
        title = stringResource(R.string.history_pick_item),
        options = MaintenanceItem.entries.map { it to stringResource(it.labelRes) },
        onPick = onPick,
        onDismiss = onDismiss,
    )
}

@Composable
private fun <T> OptionDialog(
    title: String,
    options: List<Pair<T, String>>,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (value, label) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { onPick(value) }
                            .padding(vertical = ChageunTheme.spacing.sm),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.history_cancel)) } },
    )
}

private val TIMELINE_MAX_WIDTH = 720.dp
