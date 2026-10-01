package com.naury.chageun.feature.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.model.RecordRef
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.feature.history.form.CheckFormHost
import com.naury.chageun.feature.history.form.FuelFormHost

private enum class AddDialog { Chooser, MaintenanceItem, Fuel, Check }

@Composable
fun HistoryRoute(onRecordService: (MaintenanceItem) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isExpanded = currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    var dialog by rememberSaveable { mutableStateOf<AddDialog?>(null) }
    val itemLabels = MaintenanceItem.entries.associateWith { stringResource(it.labelRes) }

    HistoryScreen(
        uiState = uiState,
        isTwoPane = isExpanded,
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
        onAdd = { dialog = AddDialog.Chooser },
        onAttach = viewModel::attach,
        onDeleteAttachment = viewModel::deleteAttachment,
        onDismissAttachFailure = viewModel::dismissAttachFailure,
    )

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
    onAttach: (RecordRef, List<String>) -> Unit,
    onDeleteAttachment: (String) -> Unit,
    onDismissAttachFailure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val attachments =
        AttachmentsState(
            uiState.attachments,
            uiState.attachFailedCount,
            onAttach,
            onDeleteAttachment,
            onDismissAttachFailure,
        )
    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val detail = uiState.detail
    val timeline: @Composable (Modifier) -> Unit = { paneModifier ->
        Box(paneModifier) {
            TimelineContent(uiState, onKeywordChanged, onFilterSelected, onSelect, onAdd, Modifier.fillMaxSize())
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.history_add)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ChageunTheme.spacing.gutter),
            )
        }
    }
    when {
        isTwoPane -> Row(
            modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.paneGap),
        ) {
            timeline(
                Modifier
                    .weight(LIST_PANE_WEIGHT)
                    .fillMaxHeight(),
            )
            val detailModifier = Modifier
                .weight(1f - LIST_PANE_WEIGHT)
                .fillMaxHeight()
            if (detail != null) {
                RecordDetailPane(detail, attachments, onBack = null, onDelete = onDelete, modifier = detailModifier)
            } else {
                DetailPlaceholder(detailModifier)
            }
        }
        detail != null -> {
            BackHandler { onSelect(null) }
            RecordDetailPane(
                detail = detail,
                attachments = attachments,
                onBack = { onSelect(null) },
                onDelete = onDelete,
                modifier = modifier.fillMaxSize(),
            )
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

private const val LIST_PANE_WEIGHT = 0.42f
