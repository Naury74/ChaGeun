package com.naury.chageun.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.naury.chageun.core.designsystem.component.ChoicePill
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.ui.MaintenanceItemIcon
import com.naury.chageun.core.ui.PastDateField
import com.naury.chageun.core.ui.labelRes

@Composable
internal fun QuickMaintenanceStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepHeader(R.string.onboarding_quick_title, R.string.onboarding_quick_body)
    QUICK_SERVICE_ITEMS.forEach { item ->
        QuickServiceCard(
            item = item,
            input = uiState.quickServices[item] ?: QuickServiceInput(),
            error = uiState.quickServiceErrors[item],
            onAction = onAction,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickServiceCard(
    item: MaintenanceItem,
    input: QuickServiceInput,
    error: FieldError?,
    onAction: (OnboardingAction) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            ) {
                MaintenanceItemIcon(item)
                Text(stringResource(item.labelRes), style = MaterialTheme.typography.titleMedium)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            ) {
                QuickServiceMode.entries.forEach { mode ->
                    ChoicePill(
                        label = stringResource(mode.labelRes),
                        selected = input.mode == mode,
                        onClick = { onAction(OnboardingAction.QuickServiceModeSelected(item, mode)) },
                    )
                }
            }
            AnimatedVisibility(visible = input.mode == QuickServiceMode.Exact) {
                Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm)) {
                    PastDateField(
                        date = input.date,
                        placeholder = stringResource(R.string.onboarding_quick_pick_date),
                        onDateSelected = { onAction(OnboardingAction.QuickServiceDateSelected(item, it)) },
                    )
                    OutlinedTextField(
                        value = input.mileage,
                        onValueChange = { onAction(OnboardingAction.QuickServiceMileageChanged(item, it)) },
                        label = { Text(stringResource(R.string.onboarding_quick_mileage)) },
                        suffix = { Text(stringResource(R.string.onboarding_mileage_unit)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            error?.let { ErrorText(it) }
        }
    }
}

private val QuickServiceMode.labelRes: Int
    get() = when (this) {
        QuickServiceMode.Unknown -> R.string.onboarding_quick_mode_unknown
        QuickServiceMode.Recent -> R.string.onboarding_quick_mode_recent
        QuickServiceMode.HalfYear -> R.string.onboarding_quick_mode_half_year
        QuickServiceMode.OneYear -> R.string.onboarding_quick_mode_one_year
        QuickServiceMode.Exact -> R.string.onboarding_quick_mode_exact
    }
