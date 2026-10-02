package com.naury.chageun.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.component.StatusTone
import com.naury.chageun.core.designsystem.component.colors
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.ui.AdaptiveSheet
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
        Text(stringResource(R.string.home_mileage_update_title), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = uiState.mileage,
            onValueChange = onMileageChanged,
            label = { Text(stringResource(R.string.home_mileage_update_label)) },
            suffix = { Text(stringResource(R.string.home_unit_km)) },
            isError = uiState.isMissing,
            supportingText = if (uiState.isMissing) {
                (
                    {
                        Text(stringResource(R.string.home_mileage_update_required))
                    }
                    )
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
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
