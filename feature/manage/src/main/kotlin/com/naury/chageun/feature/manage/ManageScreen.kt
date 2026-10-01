package com.naury.chageun.feature.manage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.ui.HingeAwarePanes
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
 * Compact shows list then detail; wider windows keep both so items can be compared without going back.
 * The selection lives in the ViewModel, so folding or rotating switches layouts without losing it.
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
    if (isTwoPane) {
        HingeAwarePanes(
            weights = listOf(LIST_PANE_WEIGHT, 1f - LIST_PANE_WEIGHT),
            modifier = modifier.fillMaxSize(),
        ) {
            ManageList(
                uiState = uiState,
                onFilterSelected = onFilterSelected,
                onItemSelected = onItemSelected,
                onEditRule = onEditRule,
                modifier = Modifier.fillMaxSize(),
            )
            val detailModifier = Modifier.fillMaxSize()
            if (detail != null) {
                ManageDetailPane(
                    detail,
                    onBack = null,
                    onRecordService = onRecordService,
                    onEditRule = onEditRule,
                    onAskAi = onAskAi,
                    modifier = detailModifier,
                )
            } else {
                DetailPlaceholder(detailModifier)
            }
        }
    } else if (detail != null) {
        BackHandler { onItemSelected(null) }
        ManageDetailPane(
            detail,
            onBack = { onItemSelected(null) },
            onRecordService = onRecordService,
            onEditRule = onEditRule,
            onAskAi = onAskAi,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        ManageList(uiState, onFilterSelected, onItemSelected, onEditRule, modifier.fillMaxSize())
    }
}

private const val LIST_PANE_WEIGHT = 0.42f
