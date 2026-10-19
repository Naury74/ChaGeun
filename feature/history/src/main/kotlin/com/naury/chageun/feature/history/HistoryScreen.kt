package com.naury.chageun.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.designsystem.component.SegmentedControl
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.LocalBottomBarPadding
import com.naury.chageun.core.domain.analytics.AnalyticsEvent
import com.naury.chageun.core.domain.analytics.AnalyticsTracker
import com.naury.chageun.core.domain.analytics.CalendarItemKind
import com.naury.chageun.core.domain.history.MAX_ATTACHMENTS_PER_RECORD
import com.naury.chageun.core.domain.history.MAX_SOUND_DURATION_MS
import com.naury.chageun.core.model.HistoryView
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordDetail
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.model.TimelineEventType
import com.naury.chageun.core.model.TimelineItem
import com.naury.chageun.core.ui.AdaptiveListDetail
import com.naury.chageun.core.ui.ChageunOptionDialog
import com.naury.chageun.core.ui.DialogOption
import com.naury.chageun.core.ui.LocalAnalyticsTracker
import com.naury.chageun.core.ui.ThreePaneListDetail
import com.naury.chageun.core.ui.currentLocale
import com.naury.chageun.core.ui.icon
import com.naury.chageun.core.ui.isListDetailThreePane
import com.naury.chageun.core.ui.isListDetailTwoPane
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.photo.PhotoInput
import com.naury.chageun.core.ui.photo.ReceiptEditorLauncher
import com.naury.chageun.core.ui.photo.rememberPhotoInputState
import com.naury.chageun.core.ui.photo.rememberReceiptEditorLauncher
import com.naury.chageun.core.ui.sound.SoundRecorderLauncher
import com.naury.chageun.core.ui.sound.rememberSoundRecorderLauncher
import com.naury.chageun.core.ui.tone
import com.naury.chageun.feature.history.calendar.CalendarActions
import com.naury.chageun.feature.history.calendar.HistoryCalendar
import com.naury.chageun.feature.history.calendar.HistoryCalendarSlot
import com.naury.chageun.feature.history.calendar.HistoryCalendarViewModel
import com.naury.chageun.feature.history.calendar.closingDetailOnMove
import com.naury.chageun.feature.history.form.CheckFormHost
import com.naury.chageun.feature.history.form.FuelFormHost
import com.naury.kinetica.ui.KExtendedFab
import com.naury.kinetica.ui.KSkeletonLeading
import com.naury.kinetica.ui.KSkeletonListItem
import com.naury.kinetica.ui.KVariant
import com.naury.kinetica.ui.rememberFabExpanded
import java.io.File
import java.time.LocalDate
import java.time.temporal.WeekFields

private enum class AddDialog { Chooser, MaintenanceItem, Fuel, Check }

@Composable
fun HistoryRoute(
    onRecordService: (MaintenanceItem) -> Unit,
    onEditService: (MaintenanceItem, String) -> Unit = { _, _ -> },
    onAskAi: ((RecordRef) -> Unit)? = null,
    calendarLinks: HistoryCalendarLinks = HistoryCalendarLinks(),
    viewModel: HistoryViewModel = hiltViewModel(),
    calendarViewModel: HistoryCalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view by calendarViewModel.view.collectAsStateWithLifecycle()
    val calendarState by calendarViewModel.uiState.collectAsStateWithLifecycle()
    val isExpanded = currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val isTwoPane = isListDetailTwoPane()
    val isThreePane = isListDetailThreePane()
    val analytics = LocalAnalyticsTracker.current
    var dialog by rememberSaveable { mutableStateOf<AddDialog?>(null) }
    // 달력의 [이날 기록 추가]로 열면 입력 창 날짜를 그날로 채운다. 목록의 추가 버튼으로 열면 null이다.
    var addDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var isFilterOpen by rememberSaveable { mutableStateOf(false) }
    val locale = currentLocale()
    LaunchedEffect(locale) { calendarViewModel.setFirstDayOfWeek(WeekFields.of(locale).firstDayOfWeek) }
    val pendingDate = calendarLinks.pendingDate
    LaunchedEffect(pendingDate) {
        if (pendingDate != null) {
            viewModel.select(null)
            calendarViewModel.focusOn(pendingDate)
            calendarLinks.onPendingDateHandled()
        }
    }
    // 상세는 한 칸·두 칸 배치에 따라 컴포지션 위치가 달라 접거나 펼 때 상태가 사라진다.
    // 사진 선택·편집은 배치와 상관없는 이 자리에 두어, 편집 도중 접어도 이어지게 한다.
    val photoInput = rememberPhotoInputState()
    var photoTarget by rememberSaveable { mutableStateOf<String?>(null) }
    PhotoInput(
        state = photoInput,
        maxItems = (MAX_ATTACHMENTS_PER_RECORD - uiState.photos.size).coerceAtLeast(1),
        // 영수증의 작은 글씨를 남길 수 있게 첨부에서는 고화질 저장을 고르게 한다.
        showQualityOption = true,
        onPhotos = { uris, highQuality ->
            photoTarget?.toRecordRef()?.let { viewModel.attach(it, uris, highQuality) }
        },
    )
    // 사진 편집기도 같은 이유로 여기서 연다. 어느 기록·첨부의 편집인지는 태그로 남긴다.
    // 첨부는 대개 영수증이라 문서 보정을 맨 앞에 둔 편집기를 쓴다. 일반 사진에서는 그 도구가 보이지 않는다.
    val receiptEditor = rememberReceiptEditorLauncher { HistoryPhotoEditTag.apply(it, viewModel) }
    val photoEdits = receiptPhotoEdits(receiptEditor, remaining = MAX_ATTACHMENTS_PER_RECORD - uiState.photos.size)
    // 소리 녹음도 같은 이유로 여기서 연다. 녹음 화면은 3분에 닿으면 저절로 멈춘다.
    val soundRecorder =
        rememberSoundRecorderLauncher(MAX_SOUND_DURATION_MS) { HistorySoundTag.apply(it, viewModel.sounds) }
    val soundNotice by viewModel.sounds.notice.collectAsStateWithLifecycle()
    // RecordRef는 저장할 수 없으므로 종류와 ID를 나눠 보관한다.
    var editingType by rememberSaveable { mutableStateOf<TimelineEventType?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val stopEditing = {
        editingType = null
        editingId = null
    }
    val startEditing = { ref: RecordRef ->
        editingType = ref.type
        editingId = ref.id
    }
    val itemLabels = MaintenanceItem.entries.associateWith { stringResource(it.labelRes) }
    // 목록 검색과 같이, 현지화된 항목 이름에 검색어가 들어 있으면 그 항목의 정비 기록도 찾는다.
    val matchingItems = { keyword: String ->
        if (keyword.isBlank()) emptySet() else itemLabels.filterValues { it.contains(keyword.trim(), true) }.keys
    }
    val calendarActions = calendarActions(calendarViewModel, viewModel, calendarLinks, analytics) { date ->
        addDate = date
        dialog = AddDialog.Chooser
    }.copy(onSearch = { keyword -> calendarViewModel.query.search(keyword, matchingItems(keyword)) })

    LargeTitleScaffold(
        title = stringResource(R.string.history_title).takeUnless {
            !isTwoPane &&
                uiState.selected != null
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            val shownView = view
            if (shownView == null) {
                // 고른 보기를 읽기 전에 목록을 잠깐 보였다가 달력으로 바뀌지 않게 비워 둔다.
                Box(Modifier.fillMaxSize())
                return@Column
            }
            if (isTwoPane || uiState.selected == null) {
                HistoryViewToggle(
                    view = shownView,
                    onChange = { next ->
                        analytics.track(AnalyticsEvent.HistoryViewChanged(next))
                        calendarViewModel.setView(next)
                    },
                    modifier = Modifier
                        .padding(horizontal = ChageunTheme.spacing.gutter, vertical = ChageunTheme.spacing.xxs)
                        .widthIn(max = TOGGLE_MAX_WIDTH),
                )
            }
            HistoryScreen(
                uiState = uiState,
                isTwoPane = isTwoPane,
                isThreePane = isThreePane,
                onKeywordChanged = { keyword -> viewModel.search(keyword, matchingItems(keyword)) },
                onFilterSelected = viewModel::selectFilter,
                onSelect = viewModel::select,
                onDelete = viewModel::delete,
                onEdit = { detail ->
                    openEditor(detail.ref, (detail as? RecordDetail.Maintenance)?.item, onEditService, startEditing)
                },
                // 목록에서 길게 눌러 고친 기록도 상세의 수정과 같은 입력 창을 연다.
                onEditRecord = { item -> openEditor(item.ref, item.maintenanceItem, onEditService, startEditing) },
                onAdd = {
                    addDate = null
                    dialog = AddDialog.Chooser
                },
                onOpenAdvancedFilter = { isFilterOpen = true },
                onClearAdvancedFilter = { viewModel.applyAdvancedFilter(AdvancedFilter()) },
                onLoadMore = viewModel::loadMore,
                onAskAi = onAskAi,
                onAddPhotos = { ref ->
                    photoTarget = "${ref.type.name}:${ref.id}"
                    photoInput.open()
                },
                onDeleteAttachment = viewModel::deleteAttachment,
                onDismissAttachFailure = viewModel::dismissAttachFailure,
                view = shownView,
                calendar = HistoryCalendarSlot(calendarState, calendarActions),
                modifier = Modifier.weight(1f),
                photoEdits = photoEdits,
                soundEdits = soundEdits(soundRecorder, viewModel.sounds),
                soundNotice = soundNotice,
            )
        }
    }

    if (isFilterOpen) {
        AdvancedFilterSheet(
            current = uiState.advanced,
            isExpanded = isExpanded,
            onApply = viewModel::applyAdvancedFilter,
            onDismiss = { isFilterOpen = false },
        )
    }
    AddRecordDialogs(
        dialog = dialog,
        addDate = addDate,
        isExpanded = isExpanded,
        onChange = { dialog = it },
        onRecordService = onRecordService,
        onRecordServiceOn = calendarLinks.onRecordServiceOn,
    )
    val editing = editingType?.let { type -> editingId?.let { RecordRef(type, it) } }
    when (editing?.type) {
        TimelineEventType.Fuel -> FuelFormHost(isExpanded, onDismiss = stopEditing, editingId = editing.id)
        TimelineEventType.Inspection, TimelineEventType.Repair, TimelineEventType.Note ->
            CheckFormHost(isExpanded, onDismiss = stopEditing, editing = editing)
        TimelineEventType.Maintenance, TimelineEventType.Mileage, null -> Unit
    }
}

/** 정비 기록은 정비 입력 창으로, 나머지는 이 화면의 주유·점검 입력 창으로 고친다. */
private fun openEditor(
    ref: RecordRef,
    item: MaintenanceItem?,
    onEditService: (MaintenanceItem, String) -> Unit,
    startEditing: (RecordRef) -> Unit,
) {
    if (ref.type == TimelineEventType.Maintenance && item != null) onEditService(item, ref.id) else startEditing(ref)
}

private fun receiptPhotoEdits(editor: ReceiptEditorLauncher, remaining: Int) = AttachmentPhotoEdits(
    onEdit = { attachment -> editor.edit(File(attachment.filePath), tag = HistoryPhotoEditTag.replace(attachment.id)) },
    onAddEdited = { ref -> editor.pickAndEdit(maxItems = remaining, tag = HistoryPhotoEditTag.add(ref)) },
    onScan = { ref -> editor.scan(maxItems = remaining, tag = HistoryPhotoEditTag.add(ref)) },
    onSharePdf = { detail, attachments ->
        editor.sharePdf(attachments.map { File(it.filePath) }, detail.date ?: LocalDate.now())
    },
)

private fun soundEdits(recorder: SoundRecorderLauncher, actions: SoundAttachmentActions) = SoundAttachmentEdits(
    onRecord = { ref -> recorder.record(tag = HistorySoundTag.add(ref)) },
    onPickFile = { ref -> recorder.pickFile(tag = HistorySoundTag.add(ref)) },
    onEdit = { sound -> recorder.edit(File(sound.filePath), tag = HistorySoundTag.replace(sound.id)) },
    onRename = actions::rename,
    onDelete = actions::delete,
    onDismissNotice = actions::dismissNotice,
)

/** 영수증 PDF 파일 이름에 넣을 기록 날짜. 날짜를 모르는 공공데이터 정비 기록이면 null이다. */
private val RecordDetail.date: LocalDate?
    get() = when (this) {
        is RecordDetail.Maintenance -> entry.date
        is RecordDetail.Fuel -> entry.date
        is RecordDetail.Check -> entry.date
        is RecordDetail.Mileage -> date
    }

private fun calendarActions(
    calendarViewModel: HistoryCalendarViewModel,
    historyViewModel: HistoryViewModel,
    links: HistoryCalendarLinks,
    analytics: AnalyticsTracker,
    onAddOnDate: (LocalDate) -> Unit,
) = CalendarActions(
    onSelectDate = calendarViewModel.navigation::selectDate,
    onShowMonth = calendarViewModel.navigation::showMonth,
    onToday = calendarViewModel.navigation::showToday,
    onMoveWeek = calendarViewModel.navigation::moveWeek,
    onSetMode = calendarViewModel.navigation::setMode,
    onToggleKind = calendarViewModel.query::toggleKind,
    onSetItems = calendarViewModel.query::setItems,
    onOpenSearch = calendarViewModel.query::openSearch,
    onCloseSearch = calendarViewModel.query::closeSearch,
    onPreviousResult = calendarViewModel::showPreviousResult,
    onNextResult = calendarViewModel::showNextResult,
    onOpenResult = calendarViewModel::openResult,
    onClearAll = calendarViewModel.query::clearAll,
    onOpenUpcoming = { event ->
        analytics.track(AnalyticsEvent.CalendarItemOpened(event.itemKind))
        links.open(event)
    },
    onOpenRecord = { ref ->
        analytics.track(AnalyticsEvent.CalendarItemOpened(CalendarItemKind.Record))
        historyViewModel.select(ref)
    },
    onOpenPhoto = { photo ->
        analytics.track(AnalyticsEvent.CalendarItemOpened(CalendarItemKind.Photo))
        links.onOpenPhoto(photo.id)
    },
    onAddOnDate = onAddOnDate,
).closingDetailOnMove { historyViewModel.select(null) }

/** [addDate]가 있으면 달력에서 그날 기록을 추가하는 중이라 입력 창 날짜를 그날로 채운다. */
@Composable
private fun AddRecordDialogs(
    dialog: AddDialog?,
    addDate: LocalDate?,
    isExpanded: Boolean,
    onChange: (AddDialog?) -> Unit,
    onRecordService: (MaintenanceItem) -> Unit,
    onRecordServiceOn: ((MaintenanceItem, LocalDate) -> Unit)?,
) {
    when (dialog) {
        AddDialog.Chooser -> AddRecordChooser(onPick = onChange, onDismiss = { onChange(null) })
        AddDialog.MaintenanceItem -> MaintenanceItemPicker(
            onPick = { item ->
                onChange(null)
                if (addDate != null &&
                    onRecordServiceOn != null
                ) {
                    onRecordServiceOn(item, addDate)
                } else {
                    onRecordService(item)
                }
            },
            onDismiss = { onChange(null) },
        )
        AddDialog.Fuel -> FuelFormHost(isExpanded = isExpanded, onDismiss = { onChange(null) }, initialDate = addDate)
        AddDialog.Check -> CheckFormHost(isExpanded = isExpanded, onDismiss = { onChange(null) }, initialDate = addDate)
        null -> Unit
    }
}

@Composable
internal fun HistoryViewToggle(view: HistoryView, onChange: (HistoryView) -> Unit, modifier: Modifier = Modifier) {
    val views = HistoryView.entries
    SegmentedControl(
        options = views.map {
            stringResource(
                if (it ==
                    HistoryView.List
                ) {
                    R.string.history_view_list
                } else {
                    R.string.history_view_calendar
                },
            )
        },
        selectedIndex = views.indexOf(view),
        onSelect = { onChange(views[it]) },
        modifier = modifier,
    )
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
    /** 목록 카드를 길게 눌러 고칠 때. null이면 메뉴에 수정을 두지 않는다. */
    onEditRecord: ((TimelineItem) -> Unit)? = null,
    onLoadMore: () -> Unit = {},
    onAskAi: ((RecordRef) -> Unit)? = null,
    /** [isTwoPane]보다 우선한다. 왼쪽에 검색·필터 Pane을 더한다. */
    isThreePane: Boolean = false,
    view: HistoryView = HistoryView.List,
    calendar: HistoryCalendarSlot? = null,
    photoEdits: AttachmentPhotoEdits = AttachmentPhotoEdits(),
    soundEdits: SoundAttachmentEdits = SoundAttachmentEdits(),
    soundNotice: SoundNotice? = null,
) {
    val sounds = SoundsState(uiState.sounds, soundNotice, soundEdits)
    val attachments =
        AttachmentsState(
            uiState.photos,
            uiState.attachFailedCount,
            onAddPhotos,
            onDeleteAttachment,
            onDismissAttachFailure,
            photoEdits.onEdit,
            photoEdits.onAddEdited,
            photoEdits.onScan,
            photoEdits.onSharePdf,
        )
    if (uiState.isLoading) {
        TimelineSkeleton(modifier)
        return
    }
    val detail = uiState.detail
    // 접거나 펼쳐 한 칸·두 칸이 바뀌면 상세가 컴포지션의 다른 자리로 간다. 상태째 옮겨야 삭제 확인 같은
    // 상세 안의 상태가 사라지지 않는다. 다른 기록을 열면 key로 상태를 새로 시작한다.
    val currentAttachments by rememberUpdatedState(attachments)
    val currentSounds by rememberUpdatedState(sounds)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnAskAi by rememberUpdatedState(onAskAi)
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
                    onAskAi = currentOnAskAi,
                    sounds = currentSounds,
                )
            }
        }
    }
    if (view == HistoryView.Calendar && calendar != null) {
        // 달력에서 연 기록도 목록과 같은 상세를 쓴다. 닫으면 고른 날 목록으로 돌아간다.
        HistoryCalendar(
            state = calendar.state,
            actions = calendar.actions,
            detail = detail,
            detailPane = detailPane,
            onCloseDetail = { onSelect(null) },
            isTwoPane = isTwoPane,
            isThreePane = isThreePane,
            modifier = modifier,
        )
        return
    }
    // 휴대폰에서 상세를 열면 목록이 컴포지션에서 빠지므로, 돌아왔을 때 보던 자리를 잇도록 여기서 기억한다.
    val timelineState = rememberLazyListState()
    val timeline: @Composable (Modifier) -> Unit = { paneModifier ->
        Box(paneModifier) {
            // 넓은 창에서 목록만 있을 때 한 줄이 너무 길어지지 않도록 가운데에 폭을 제한해 둔다.
            TimelineContent(
                uiState,
                onKeywordChanged,
                onFilterSelected,
                // 두 칸에서는 같은 기록을 다시 누르면 상세를 닫고 목록을 넓게 되돌린다.
                { ref -> onSelect(if ((isTwoPane || isThreePane) && ref == uiState.selected) null else ref) },
                onAdd,
                onOpenAdvancedFilter = onOpenAdvancedFilter,
                onClearAdvancedFilter = onClearAdvancedFilter,
                onLoadMore = onLoadMore,
                listState = timelineState,
                showFilters = !isThreePane,
                recordActions = TimelineRecordActions(onEdit = onEditRecord, onAskAi = onAskAi, onDelete = onDelete),
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = TIMELINE_MAX_WIDTH)
                    .align(Alignment.TopCenter),
            )
            // 목록을 내리면 아이콘만 남기고 접혀 기록을 덜 가린다. 접혀도 TalkBack은 글자를 읽는다.
            KExtendedFab(
                text = stringResource(R.string.history_add),
                icon = Icons.Filled.Add,
                onClick = onAdd,
                expanded = rememberFabExpanded(timelineState),
                // 다른 주요 버튼(Tonal)과 같은 무게로 보이게 한다. 옅은 톤 아래에는 판 색을 먼저 깔아 목록이 비치지 않는다.
                variant = KVariant.Tonal,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // 아래에 떠 있는 탭 바 위로 띄운다.
                    .padding(bottom = LocalBottomBarPadding.current)
                    .padding(ChageunTheme.spacing.gutter),
            )
        }
    }
    when {
        isThreePane -> {
            BackHandler(enabled = detail != null) { onSelect(null) }
            ThreePaneListDetail(
                selected = detail,
                supporting = {
                    HistoryFilterPane(
                        uiState = uiState,
                        onKeywordChanged = onKeywordChanged,
                        onFilterSelected = onFilterSelected,
                        onOpenAdvancedFilter = onOpenAdvancedFilter,
                        onClearAdvancedFilter = onClearAdvancedFilter,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                list = { timeline(Modifier.fillMaxSize()) },
                detail = { detailPane(it, Modifier.fillMaxSize()) },
                emptyDetail = { DetailPlaceholder(Modifier.fillMaxSize()) },
                modifier = modifier,
            )
        }
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

/** 기록을 읽는 동안 카드 자리를 미리 그려 둔다. 불러온 뒤 목록이 갑자기 튀어나오지 않는다. */
@Composable
private fun TimelineSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = ChageunTheme.spacing.gutter, vertical = ChageunTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
    ) {
        repeat(SKELETON_ROWS) { KSkeletonListItem(leading = KSkeletonLeading.Avatar, trailing = true) }
    }
}

@Composable
private fun AddRecordChooser(onPick: (AddDialog) -> Unit, onDismiss: () -> Unit) {
    val choices = listOf(
        Triple(AddDialog.MaintenanceItem, R.string.history_add_maintenance, TimelineEventType.Maintenance),
        Triple(AddDialog.Fuel, R.string.history_add_fuel, TimelineEventType.Fuel),
        Triple(AddDialog.Check, R.string.history_add_check, TimelineEventType.Inspection),
    )
    ChageunOptionDialog(
        title = stringResource(R.string.history_add),
        options = choices.map { (dialog, labelRes, type) ->
            DialogOption(stringResource(labelRes), { onPick(dialog) }, type.icon, type.tone())
        },
        cancelLabel = stringResource(R.string.history_cancel),
        onDismiss = onDismiss,
    )
}

@Composable
private fun MaintenanceItemPicker(onPick: (MaintenanceItem) -> Unit, onDismiss: () -> Unit) {
    ChageunOptionDialog(
        title = stringResource(R.string.history_pick_item),
        options = MaintenanceItem.entries.map { item ->
            DialogOption(stringResource(item.labelRes), { onPick(item) }, item.icon, item.category.tone())
        },
        cancelLabel = stringResource(R.string.history_cancel),
        onDismiss = onDismiss,
    )
}

private val TIMELINE_MAX_WIDTH = 720.dp

private val TOGGLE_MAX_WIDTH = 360.dp

private const val SKELETON_ROWS = 6
