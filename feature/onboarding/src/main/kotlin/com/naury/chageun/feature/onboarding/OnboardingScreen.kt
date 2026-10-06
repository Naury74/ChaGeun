package com.naury.chageun.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme

@Composable
fun OnboardingRoute(onRestoreFromBackup: () -> Unit = {}, viewModel: OnboardingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(uiState = uiState, onAction = viewModel::onAction, onRestoreFromBackup = onRestoreFromBackup)
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
    onRestoreFromBackup: () -> Unit = {},
) {
    BackHandler(enabled = uiState.canGoBack) { onAction(OnboardingAction.Back) }
    val slide = motionSpec<androidx.compose.ui.unit.IntOffset>()
    val fade = motionSpec<Float>()

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = ChageunTheme.spacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepTopBar(uiState, onBack = { onAction(OnboardingAction.Back) })
        AnimatedContent(
            targetState = uiState.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val direction = if (forward) SlideDirection.Start else SlideDirection.End
                (slideIntoContainer(direction, slide) + fadeIn(fade))
                    .togetherWith(slideOutOfContainer(direction, slide) + fadeOut(fade))
            },
            modifier = Modifier
                .weight(1f)
                .widthIn(max = CONTENT_MAX_WIDTH)
                .fillMaxWidth(),
            label = "onboarding-step",
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = ChageunTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
            ) {
                when (step) {
                    OnboardingStep.Intro -> IntroStep()
                    OnboardingStep.Plate -> PlateStep(uiState, onAction)
                    OnboardingStep.VehicleInfo -> VehicleInfoStep(uiState, onAction)
                    OnboardingStep.Photo -> PhotoStep(uiState, onAction)
                    OnboardingStep.Mileage -> MileageStep(uiState, onAction)
                    OnboardingStep.QuickMaintenance -> QuickMaintenanceStep(uiState, onAction)
                    OnboardingStep.Notifications -> NotificationsStep()
                }
                if (uiState.hasSaveFailed) {
                    Text(
                        stringResource(R.string.onboarding_save_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        BottomActions(
            uiState = uiState,
            onAction = onAction,
            onRestoreFromBackup = onRestoreFromBackup,
            modifier = Modifier
                .widthIn(max = CONTENT_MAX_WIDTH)
                .fillMaxWidth()
                .padding(bottom = ChageunTheme.spacing.md),
        )
    }
}

/** 소개 화면에는 진행 막대를 두지 않는다. 입력 단계부터 남은 단계를 보여 준다. */
@Composable
private fun StepTopBar(uiState: OnboardingUiState, onBack: () -> Unit) {
    if (uiState.step == OnboardingStep.Intro) return
    val index = uiState.step.ordinal
    val total = OnboardingStep.entries.size - 1
    val progress by animateFloatAsState(index / total.toFloat(), motionSpec(), label = "onboarding-progress")
    Row(
        modifier = Modifier
            .widthIn(max = CONTENT_MAX_WIDTH)
            .fillMaxWidth()
            .padding(top = ChageunTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        IconButton(onClick = onBack, enabled = uiState.canGoBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.onboarding_back))
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            drawStopIndicator = {},
        )
        Text(
            stringResource(R.string.onboarding_step_progress, index, total),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BottomActions(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    onRestoreFromBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val optInToNotifications = rememberNotificationOptIn { onAction(OnboardingAction.Finish) }
    val (labelRes, onPrimary) = when (uiState.step) {
        OnboardingStep.Intro -> R.string.onboarding_start to { onAction(OnboardingAction.Start) }
        OnboardingStep.Plate -> R.string.onboarding_next to { onAction(OnboardingAction.SubmitPlate) }
        OnboardingStep.VehicleInfo -> R.string.onboarding_next to { onAction(OnboardingAction.SubmitVehicleInfo) }
        // 사진은 고르지 않아도 넘어갈 수 있다. 버튼 이름으로 건너뛰는 것임을 알린다.
        OnboardingStep.Photo -> (
            if (uiState.photoUri ==
                null
            ) {
                R.string.onboarding_photo_later
            } else {
                R.string.onboarding_next
            }
            ) to
            { onAction(OnboardingAction.SubmitPhoto) }
        OnboardingStep.Mileage -> R.string.onboarding_next to { onAction(OnboardingAction.SubmitMileage) }
        OnboardingStep.QuickMaintenance ->
            R.string.onboarding_next to { onAction(OnboardingAction.SubmitQuickMaintenance) }
        OnboardingStep.Notifications -> R.string.onboarding_notifications_allow to optInToNotifications
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Button(
            onClick = onPrimary,
            enabled = !uiState.isSaving,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CTA_HEIGHT),
        ) {
            Text(stringResource(labelRes), style = MaterialTheme.typography.titleMedium)
        }
        // 새 휴대폰으로 바꾼 사람은 차량을 다시 입력하지 않고 클라우드 백업에서 시작한다.
        if (uiState.step == OnboardingStep.Intro) {
            TextButton(onClick = onRestoreFromBackup, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_restore_from_backup))
            }
        }
        if (uiState.step == OnboardingStep.Notifications) {
            TextButton(
                onClick = { onAction(OnboardingAction.Finish) },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.onboarding_notifications_later))
            }
        }
    }
}

@Composable
internal fun StepHeader(@StringRes titleRes: Int, @StringRes bodyRes: Int?) {
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        bodyRes?.let {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(ChageunTheme.spacing.xs))
}

@Composable
internal fun ErrorText(error: FieldError) {
    Text(
        text = stringResource(error.messageRes),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

private val FieldError.messageRes: Int
    get() = when (this) {
        FieldError.Required -> R.string.onboarding_error_required
        FieldError.InvalidPlate -> R.string.onboarding_error_plate_invalid
        FieldError.UnsupportedPlate -> R.string.onboarding_error_plate_unsupported
        FieldError.InvalidYear -> R.string.onboarding_error_year_invalid
        FieldError.InvalidMileage -> R.string.onboarding_error_mileage_invalid
        FieldError.FutureDate -> R.string.onboarding_error_future_date
        FieldError.ExceedsCurrentMileage -> R.string.onboarding_error_exceeds_mileage
    }

private val CONTENT_MAX_WIDTH = 560.dp
private val CTA_HEIGHT = 56.dp
