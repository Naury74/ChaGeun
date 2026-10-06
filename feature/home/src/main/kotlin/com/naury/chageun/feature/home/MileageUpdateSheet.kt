package com.naury.chageun.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.StatusTone
import com.naury.chageun.core.designsystem.component.colors
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.AdaptiveSheet
import com.naury.chageun.core.ui.FormHeader
import com.naury.chageun.core.ui.NumberInputField
import com.naury.chageun.core.ui.QuickPick
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.photo.PhotoInput
import com.naury.chageun.core.ui.photo.rememberPhotoInputState

/** Home과 My car가 하나의 진입점을 공유하도록 앱 셸에서 띄운다. */
@Composable
fun MileageUpdateHost(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    viewModel: MileageUpdateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onDismiss() }
    // 편집기에서 계기판 숫자 부분만 남기도록 잘라 내면 더 잘 읽힌다.
    val photoInput = rememberPhotoInputState()
    PhotoInput(photoInput, maxItems = 1, onPhotos = { uris, _ -> uris.firstOrNull()?.let(viewModel::readDashboard) })
    AdaptiveSheet(isExpanded = isExpanded, onDismiss = onDismiss) {
        MileageUpdateContent(
            uiState,
            viewModel::onMileageChanged,
            viewModel::save,
            viewModel::confirmCorrection,
            onDismiss,
            onReadDashboard = photoInput::open,
        )
    }
}

@Composable
internal fun MileageUpdateContent(
    uiState: MileageUpdateUiState,
    onMileageChanged: (String) -> Unit,
    onSave: () -> Unit,
    onConfirmCorrection: () -> Unit,
    onDismiss: () -> Unit,
    onReadDashboard: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .imePadding()
            .navigationBarsPadding()
            .padding(ChageunTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        FormHeader(
            Icons.Filled.Speed,
            ChageunTheme.colors.unknown,
            stringResource(R.string.home_mileage_update_title),
        )
        uiState.previous?.let {
            Text(
                stringResource(R.string.home_mileage_previous, formatNumber(it.value)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        NumberInputField(
            value = uiState.mileage,
            onValueChange = onMileageChanged,
            label = stringResource(R.string.home_mileage_update_label),
            unit = stringResource(R.string.home_unit_km),
            picks = uiState.previous?.let { previous ->
                MILEAGE_STEPS.map { step ->
                    QuickPick(
                        stringResource(R.string.home_mileage_step, formatNumber(step)),
                        (previous.value + step).toString(),
                    )
                }
            }.orEmpty(),
            errorText = stringResource(R.string.home_mileage_update_required).takeIf { uiState.isMissing },
        )
        DashboardRead(uiState.dashboard, onReadDashboard, onMileageChanged)
        val lowerThan = uiState.lowerThan
        if (lowerThan != null) {
            val tone = StatusTone.Upcoming.colors
            Surface(color = tone.container, contentColor = tone.content, shape = MaterialTheme.shapes.medium) {
                Column(
                    Modifier.padding(ChageunTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
                ) {
                    Text(
                        stringResource(R.string.home_mileage_lower, formatNumber(lowerThan.value)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = onConfirmCorrection, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_mileage_save_correction))
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_cancel))
                }
                Button(
                    onClick = onSave,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ChageunTheme.spacing.minTouchTarget),
                ) { Text(stringResource(R.string.home_save)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardRead(state: DashboardReadState, onRead: () -> Unit, onPick: (String) -> Unit) {
    when (state) {
        DashboardReadState.Reading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(DASHBOARD_PROGRESS_SIZE), strokeWidth = 2.dp)
            Text(
                stringResource(R.string.home_dashboard_reading),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = ChageunTheme.spacing.sm),
            )
        }
        is DashboardReadState.Read -> Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
            Text(
                stringResource(R.string.home_dashboard_read, formatNumber(state.best.value)),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.others.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                    (listOf(state.best) + state.others).forEach { candidate ->
                        AssistChip(
                            onClick = { onPick(candidate.value.toString()) },
                            label = {
                                Text(stringResource(R.string.home_mileage_value, formatNumber(candidate.value)))
                            },
                        )
                    }
                }
            }
            state.range?.let {
                Text(
                    stringResource(R.string.home_dashboard_range, formatNumber(it.value)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DashboardReadState.NotFound -> Text(
            stringResource(R.string.home_dashboard_not_found),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        DashboardReadState.Unavailable -> Text(
            stringResource(R.string.home_dashboard_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        DashboardReadState.Idle -> Unit
    }
    if (state != DashboardReadState.Reading) {
        OutlinedButton(onClick = onRead, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.home_dashboard_button),
                modifier = Modifier.padding(start = ChageunTheme.spacing.xs),
            )
        }
    }
}

/** 마지막 기록 이후 흔히 달리는 거리. 칩을 누르면 마지막 값에 더한 주행거리가 채워진다. */
private val MILEAGE_STEPS = listOf(100L, 300L, 500L, 1_000L)

private val DASHBOARD_PROGRESS_SIZE = 18.dp
