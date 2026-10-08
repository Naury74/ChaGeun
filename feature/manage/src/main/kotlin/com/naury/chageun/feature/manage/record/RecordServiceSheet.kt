package com.naury.chageun.feature.manage.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.StatusTone
import com.naury.chageun.core.designsystem.component.colors
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.history.MAX_ATTACHMENTS_PER_RECORD
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.ui.AdaptiveSheet
import com.naury.chageun.core.ui.FormHeader
import com.naury.chageun.core.ui.FormLabel
import com.naury.chageun.core.ui.NumberInputField
import com.naury.chageun.core.ui.OptionalSection
import com.naury.chageun.core.ui.QuickDateField
import com.naury.chageun.core.ui.QuickPick
import com.naury.chageun.core.ui.SaveSuccessMark
import com.naury.chageun.core.ui.formatDate
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.icon
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.core.ui.photo.PhotoInput
import com.naury.chageun.core.ui.photo.rememberPhotoInputState
import com.naury.chageun.core.ui.tone
import com.naury.chageun.feature.manage.R
import java.time.LocalDate

/**
 * 앱 셸에서 쓰는 진입점. Compact 창에서는 Bottom Sheet, 그 외에는 너비를 제한한 Dialog로 띄운다.
 *
 * ViewModel은 앱 셸의 Activity 범위에 남는다. 시트를 열 때마다 [sessionKey]를 바꿔 주어야
 * 같은 항목을 다시 열었을 때 지난번 저장 결과 대신 새 입력 화면이 뜬다.
 */
@Composable
fun RecordServiceHost(
    item: MaintenanceItem,
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    editingRecordId: String? = null,
    sessionKey: Int = 0,
) {
    val target = editingRecordId?.let { "${item.name}-edit-$it" } ?: item.name
    val viewModel = hiltViewModel<RecordServiceViewModel, RecordServiceViewModel.Factory>(
        key = "$target-$sessionKey",
    ) {
        it.create(RecordServiceTarget(item, editingRecordId))
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isEditSaved) { if (uiState.isEditSaved) onDismiss() }
    val photoInput = rememberPhotoInputState()
    PhotoInput(
        state = photoInput,
        maxItems = MAX_ATTACHMENTS_PER_RECORD,
        // 영수증의 작은 글씨를 남길 수 있게 고화질 저장을 고르게 한다.
        showQualityOption = true,
        onPhotos = viewModel::onPhotosChanged,
    )
    val content: @Composable () -> Unit = {
        RecordServiceContent(
            uiState = uiState,
            actions = RecordServiceActions(
                onDateSelected = viewModel::onDateSelected,
                onMileageChanged = viewModel::onMileageChanged,
                onCostChanged = viewModel::onCostChanged,
                onShopNameChanged = viewModel::onShopNameChanged,
                onMemoChanged = viewModel::onMemoChanged,
                onToggleAlsoReplaced = viewModel::toggleAlsoReplaced,
                onSave = { viewModel.save() },
                onConfirmLowerMileage = { viewModel.save(confirmLowerMileage = true) },
                onEditLowerMileage = viewModel::dismissLowerMileageWarning,
                onDismiss = onDismiss,
                onDecideOdometer = viewModel::decideOdometer,
                onPickPhotos = photoInput::open,
                onClearPhotos = { viewModel.onPhotosChanged(emptyList(), highQuality = false) },
            ),
        )
    }
    AdaptiveSheet(isExpanded = isExpanded, onDismiss = onDismiss, content = content)
}

data class RecordServiceActions(
    val onDateSelected: (LocalDate) -> Unit,
    val onMileageChanged: (String) -> Unit,
    val onCostChanged: (String) -> Unit,
    val onShopNameChanged: (String) -> Unit,
    val onMemoChanged: (String) -> Unit,
    val onSave: () -> Unit,
    val onConfirmLowerMileage: () -> Unit,
    val onEditLowerMileage: () -> Unit,
    val onDismiss: () -> Unit,
    val onDecideOdometer: (Boolean) -> Unit = {},
    val onToggleAlsoReplaced: (MaintenanceItem) -> Unit = {},
    val onPickPhotos: () -> Unit = {},
    val onClearPhotos: () -> Unit = {},
)

@Composable
fun RecordServiceContent(uiState: RecordServiceUiState, actions: RecordServiceActions, modifier: Modifier = Modifier) {
    val itemName = stringResource(uiState.item.labelRes)
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(ChageunTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        val saved = uiState.savedResult
        if (saved != null) {
            SavedContent(itemName, saved, actions.onDismiss)
            return@Column
        }
        val titleRes = if (uiState.isEditing) R.string.record_edit_title else R.string.record_title
        FormHeader(uiState.item.icon, uiState.item.category.tone(), stringResource(titleRes, itemName))
        FormLabel(stringResource(R.string.record_date))
        QuickDateField(date = uiState.date, onDateSelected = actions.onDateSelected)
        uiState.errors[RecordServiceField.Date]?.let { ErrorText(it) }
        NumberInputField(
            value = uiState.mileage,
            onValueChange = actions.onMileageChanged,
            label = stringResource(R.string.record_mileage),
            unit = stringResource(R.string.record_unit_km),
            errorText = uiState.errors[RecordServiceField.Mileage]?.let { stringResource(it.messageRes) },
        )
        NumberInputField(
            value = uiState.cost,
            onValueChange = actions.onCostChanged,
            label = stringResource(R.string.record_cost),
            unit = stringResource(R.string.record_unit_won),
            picks = costPicks(uiState.item),
            errorText = uiState.errors[RecordServiceField.Cost]?.let { stringResource(it.messageRes) },
            supportingText = stringResource(R.string.record_cost_hint),
        )
        MoreFields(uiState, actions)
        uiState.lowerMileageWarning?.let { previous ->
            LowerMileageWarning(itemName, formatNumber(previous.value), actions)
        }
        uiState.odometerPrompt?.let { current ->
            OdometerPrompt(formatNumber(current.value), uiState.mileage, actions.onDecideOdometer)
        }
        if (uiState.hasSaveFailed) {
            Text(stringResource(R.string.record_save_failed), color = MaterialTheme.colorScheme.error)
        }
        if (uiState.lowerMileageWarning == null && uiState.odometerPrompt == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                TextButton(onClick = actions.onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.record_cancel))
                }
                Button(
                    onClick = actions.onSave,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ChageunTheme.spacing.minTouchTarget),
                ) { Text(stringResource(R.string.record_save)) }
            }
        }
    }
}

/**
 * 같은 날 함께 바꾼 항목을 여러 개 고른다. 고른 항목마다 같은 날짜·주행거리로 교체 기록이 따로 남는다.
 * 비용·메모는 처음 항목에만 들어간다는 것을 미리 알려 지출이 두 번 잡히지 않는다는 점을 분명히 한다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlsoReplacedPicker(
    candidates: List<MaintenanceItem>,
    selected: Set<MaintenanceItem>,
    onToggle: (MaintenanceItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        FormLabel(stringResource(R.string.record_also_replaced))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            candidates.forEach { candidate ->
                val isSelected = candidate in selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(candidate) },
                    label = { Text(stringResource(candidate.labelRes)) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
        Text(
            stringResource(R.string.record_also_replaced_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SavedContent(itemName: String, saved: SavedResult, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        SaveSuccessMark()
        Text(
            stringResource(R.string.record_saved_title, itemName),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        val km = saved.nextDistanceDue?.let { formatNumber(it.value) }
        val date = saved.nextDateDue?.let { formatDate(it) }
        val message = when {
            km != null && date != null -> stringResource(R.string.record_saved_next_both, km, date)
            km != null -> stringResource(R.string.record_saved_next_km, km)
            date != null -> stringResource(R.string.record_saved_next_date, date)
            else -> null
        }
        val companions = saved.alsoReplaced.takeIf { it.isNotEmpty() }?.let { items ->
            stringResource(
                R.string.record_saved_also_replaced,
                items.map { stringResource(it.labelRes) }.joinToString(", "),
            )
        }
        val photoFailure = saved.failedPhotoCount.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.record_saved_photo_failed, it, it)
        }
        listOfNotNull(message, companions, photoFailure).forEach {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ChageunTheme.spacing.minTouchTarget),
        ) {
            Text(stringResource(R.string.record_done))
        }
    }
}

/** 항목마다 흔한 비용대가 달라 가격대별로 세 가지를 제안한다. 실제 견적이 아니라 입력을 줄이기 위한 값이다. */
@Composable
private fun costPicks(item: MaintenanceItem): List<QuickPick> {
    val amounts = when (item) {
        MaintenanceItem.Wiper, MaintenanceItem.CabinFilter, MaintenanceItem.AirFilter -> LOW_COSTS
        MaintenanceItem.Tire -> TIRE_COSTS
        MaintenanceItem.BrakePad, MaintenanceItem.TransmissionOil, MaintenanceItem.Battery -> HIGH_COSTS
        else -> MID_COSTS
    }
    return listOf(QuickPick(stringResource(R.string.record_cost_free), "0")) +
        amounts.map { QuickPick(stringResource(R.string.record_cost_pick, formatNumber(it)), it.toString()) }
}

@Composable
private fun LowerMileageWarning(itemName: String, previousKm: String, actions: RecordServiceActions) {
    val tone = StatusTone.Upcoming.colors
    Surface(color = tone.container, contentColor = tone.content, shape = MaterialTheme.shapes.medium) {
        Column(
            Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            Text(stringResource(R.string.record_lower_mileage_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.record_lower_mileage_body, itemName, previousKm),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                TextButton(onClick = actions.onEditLowerMileage, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.record_lower_mileage_edit))
                }
                Button(onClick = actions.onConfirmLowerMileage, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.record_lower_mileage_save))
                }
            }
        }
    }
}

@Composable
private fun OdometerPrompt(currentKm: String, enteredKm: String, onDecide: (Boolean) -> Unit) {
    val entered = enteredKm.toLongOrNull()?.let { formatNumber(it) } ?: enteredKm
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            Text(stringResource(R.string.record_odometer_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.record_odometer_body, currentKm, entered),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                TextButton(onClick = { onDecide(false) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.record_odometer_keep))
                }
                Button(onClick = { onDecide(true) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.record_odometer_update))
                }
            }
        }
    }
}

/** 정비소·메모·함께 교체·사진처럼 꼭 넣지 않아도 되는 칸. */
@Composable
private fun MoreFields(uiState: RecordServiceUiState, actions: RecordServiceActions) {
    OptionalSection(
        hasValue = uiState.shopName.isNotEmpty() ||
            uiState.memo.isNotEmpty() ||
            uiState.alsoReplaced.isNotEmpty() ||
            uiState.photos.isNotEmpty(),
    ) {
        if (uiState.companionCandidates.isNotEmpty()) {
            AlsoReplacedPicker(uiState.companionCandidates, uiState.alsoReplaced, actions.onToggleAlsoReplaced)
        }
        OutlinedTextField(
            value = uiState.shopName,
            onValueChange = actions.onShopNameChanged,
            label = { Text(stringResource(R.string.record_shop)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.memo,
            onValueChange = actions.onMemoChanged,
            label = { Text(stringResource(R.string.record_memo)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        // 고칠 때는 기록 상세의 사진 칸에서 붙이고 지운다.
        if (!uiState.isEditing) PhotoPicker(uiState.photos.size, actions.onPickPhotos, actions.onClearPhotos)
    }
}

@Composable
private fun PhotoPicker(count: Int, onPick: () -> Unit, onClear: () -> Unit) {
    if (count == 0) {
        OutlinedButton(
            onClick = onPick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ChageunTheme.spacing.minTouchTarget),
        ) {
            Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(ChageunTheme.spacing.xs))
            Text(stringResource(R.string.record_photos_add))
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Photo, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                pluralStringResource(R.plurals.record_photos_count, count, count),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = ChageunTheme.spacing.xs),
            )
            TextButton(onClick = onPick) { Text(stringResource(R.string.record_photos_change)) }
            TextButton(onClick = onClear) { Text(stringResource(R.string.record_photos_clear)) }
        }
    }
    Text(
        stringResource(R.string.record_photos_notice),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ErrorText(error: RecordServiceError) {
    Text(
        stringResource(error.messageRes),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

private val RecordServiceError.messageRes: Int
    get() = when (this) {
        RecordServiceError.Required -> R.string.record_error_required
        RecordServiceError.FutureDate -> R.string.record_error_future_date
        RecordServiceError.InvalidNumber -> R.string.record_error_invalid_number
    }

private val LOW_COSTS = listOf(20_000L, 30_000L, 50_000L)
private val MID_COSTS = listOf(50_000L, 80_000L, 100_000L)
private val HIGH_COSTS = listOf(100_000L, 150_000L, 200_000L)
private val TIRE_COSTS = listOf(300_000L, 600_000L, 800_000L)
