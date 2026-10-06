package com.naury.chageun.feature.manage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.ui.AdaptiveListDetail
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
            viewModel.selectItem(pendingSelection)
            onPendingSelectionHandled()
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    var editingItem by rememberSaveable { mutableStateOf<MaintenanceItem?>(null) }
    ManageScreen(
        uiState = uiState,
        isTwoPane = isListDetailTwoPane(),
        onFilterSelected = viewModel::selectFilter,
        onItemSelected = viewModel::selectItem,
        onRecordService = onRecordService,
        onEditRule = { editingItem = it },
        onAskAi = onAskAi,
    )
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
) {
    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val detail = uiState.detail
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
    if (isTwoPane) {
        BackHandler(enabled = detail != null) { onItemSelected(null) }
        AdaptiveListDetail(
            selected = detail,
            list = {
                ManageList(
                    uiState = uiState,
                    onFilterSelected = onFilterSelected,
                    onItemSelected = { item ->
                        // 같은 항목을 다시 누르면 상세를 닫고 목록을 넓게 되돌린다.
                        onItemSelected(item.takeIf { it != uiState.selectedItem })
                    },
                    onEditRule = onEditRule,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            detail = { detailPane(it, Modifier.fillMaxSize()) },
            emptyDetail = { DetailPlaceholder(Modifier.fillMaxSize()) },
            modifier = modifier,
        )
    } else if (detail != null) {
        BackHandler { onItemSelected(null) }
        detailPane(detail, modifier.fillMaxSize())
    } else {
        ManageList(uiState, onFilterSelected, onItemSelected, onEditRule, modifier.fillMaxSize())
    }
}
