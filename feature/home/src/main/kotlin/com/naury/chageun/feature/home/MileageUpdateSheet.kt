package com.naury.chageun.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

/** Home과 My car가 하나의 진입점을 공유하도록 앱 셸에서 띄운다. */
@Composable
fun MileageUpdateHost(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    viewModel: MileageUpdateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onDismiss() }
    AdaptiveSheet(isExpanded = isExpanded, onDismiss = onDismiss) {
        MileageUpdateContent(
            uiState,
            viewModel::onMileageChanged,
            viewModel::save,
            viewModel::confirmCorrection,
            onDismiss,
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

/** 마지막 기록 이후 흔히 달리는 거리. 칩을 누르면 마지막 값에 더한 주행거리가 채워진다. */
private val MILEAGE_STEPS = listOf(100L, 300L, 500L, 1_000L)
