package com.naury.chageun.feature.manage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.LargeTitleScaffold
import com.naury.chageun.core.model.MaintenanceCategory
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.ui.AdaptiveListDetail
import com.naury.chageun.core.ui.ThreePaneListDetail
import com.naury.chageun.core.ui.isListDetailThreePane
import com.naury.chageun.core.ui.isListDetailTwoPane
import com.naury.chageun.feature.manage.rule.RuleEditorHost

@Composable
fun ManageRoute(
    onRecordService: (MaintenanceItem) -> Unit,
    onAskAi: (MaintenanceItem) -> Unit = {},
    pendingSelection: MaintenanceItem? = null,
    onPendingSelectionHandled: () -> Unit = {},
    viewModel: ManageViewModel = hiltViewModel(),
) {
    LaunchedEffect(pendingSelection) {
        if (pendingSelection != null) {
            viewModel.selectFilter(ManageFilter.All)
            viewModel.selectCategory(null)
            viewModel.selectItem(pendingSelection)
            onPendingSelectionHandled()
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    var editingItem by rememberSaveable { mutableStateOf<MaintenanceItem?>(null) }
    val isTwoPane = isListDetailTwoPane()
    val isThreePane = isListDetailThreePane()
    // 분류는 세 칸의 왼쪽 Pane에서만 고를 수 있다. 창이 좁아져 그 Pane이 사라지면 보이지 않는 조건이 남지 않게 푼다.
    LaunchedEffect(isThreePane) {
        if (!isThreePane) viewModel.selectCategory(null)
    }
    // 좁은 화면에서 상세를 열면 상세가 자기 머리글(뒤로 가기)을 가지므로 큰 제목을 숨긴다.
    val title = stringResource(R.string.manage_title).takeUnless { !isTwoPane && uiState.selectedItem != null }
    LargeTitleScaffold(title = title) { padding ->
        ManageScreen(
            uiState = uiState,
            isTwoPane = isTwoPane,
            isThreePane = isThreePane,
            onFilterSelected = viewModel::selectFilter,
            onCategorySelected = viewModel::selectCategory,
            onItemSelected = viewModel::selectItem,
            onRecordService = onRecordService,
            onEditRule = { editingItem = it },
            onAskAi = onAskAi,
            modifier = Modifier.padding(padding),
        )
    }
    editingItem?.let { item -> RuleEditorHost(item = item, onDismiss = { editingItem = null }) }
}

/**
 * Compact에서는 목록 다음에 상세를 보여준다. 넓은 창에서는 뒤로 가지 않고 항목을 비교할 수 있도록 둘 다 유지한다.
 * 선택 상태는 ViewModel에 있으므로 접거나 회전해 레이아웃이 바뀌어도 유지된다.
 */
@Composable
fun ManageScreen(
    uiState: ManageUiState,
    isTwoPane: Boolean,
    onFilterSelected: (ManageFilter) -> Unit,
    onItemSelected: (MaintenanceItem?) -> Unit,
    onRecordService: (MaintenanceItem) -> Unit,
    onEditRule: (MaintenanceItem) -> Unit,
    modifier: Modifier = Modifier,
    onAskAi: (MaintenanceItem) -> Unit = {},
    /** [isTwoPane]보다 우선한다. 왼쪽에 상태·분류 필터 Pane을 더한다. */
    isThreePane: Boolean = false,
    onCategorySelected: (MaintenanceCategory?) -> Unit = {},
) {
    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val detail = uiState.detail
    // 휴대폰에서 상세를 열면 목록이 컴포지션에서 빠지므로, 돌아왔을 때 보던 자리를 잇도록 여기서 기억한다.
    val gridState = rememberLazyGridState()
    // 접거나 펼쳐 한 칸·두 칸이 바뀌어도 상세를 상태째 옮겨 스크롤 위치 등을 이어서 쓴다.
    val currentOnItemSelected by rememberUpdatedState(onItemSelected)
    val currentOnRecordService by rememberUpdatedState(onRecordService)
    val currentOnEditRule by rememberUpdatedState(onEditRule)
    val currentOnAskAi by rememberUpdatedState(onAskAi)
    val detailPane = remember {
        movableContentOf { shown: ManageDetail, paneModifier: Modifier ->
            key(shown.status.item) {
                ManageDetailPane(
                    shown,
                    onBack = { currentOnItemSelected(null) },
                    onRecordService = currentOnRecordService,
                    onEditRule = currentOnEditRule,
                    onAskAi = currentOnAskAi,
                    modifier = paneModifier,
                )
            }
        }
    }
    val list = @Composable { listModifier: Modifier, showFilters: Boolean ->
        ManageList(
            uiState = uiState,
            onFilterSelected = onFilterSelected,
            onItemSelected = { item ->
                // 같은 항목을 다시 누르면 상세를 닫는다. 두 칸에서는 목록이 다시 넓어진다.
                onItemSelected(item.takeIf { !(isTwoPane || isThreePane) || it != uiState.selectedItem })
            },
            onEditRule = onEditRule,
            modifier = listModifier,
            gridState = gridState,
            showFilters = showFilters,
        )
    }
    when {
        isThreePane -> {
            BackHandler(enabled = detail != null) { onItemSelected(null) }
            ThreePaneListDetail(
                selected = detail,
                supporting = {
                    ManageFilterPane(uiState, onFilterSelected, onCategorySelected, Modifier.fillMaxSize())
                },
                list = { list(Modifier.fillMaxSize(), false) },
                detail = { detailPane(it, Modifier.fillMaxSize()) },
                emptyDetail = { DetailPlaceholder(Modifier.fillMaxSize()) },
                modifier = modifier,
            )
        }
        isTwoPane -> {
            BackHandler(enabled = detail != null) { onItemSelected(null) }
            AdaptiveListDetail(
                selected = detail,
                list = { list(Modifier.fillMaxSize(), true) },
                detail = { detailPane(it, Modifier.fillMaxSize()) },
                emptyDetail = { DetailPlaceholder(Modifier.fillMaxSize()) },
                modifier = modifier,
            )
        }
        detail != null -> {
            BackHandler { onItemSelected(null) }
            detailPane(detail, modifier.fillMaxSize())
        }
        else -> list(modifier.fillMaxSize(), true)
    }
}
