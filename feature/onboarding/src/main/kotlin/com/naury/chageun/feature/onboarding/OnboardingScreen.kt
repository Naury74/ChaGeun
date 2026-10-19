package com.naury.chageun.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.kinetica.ui.KAlert
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KButtonSize
import com.naury.kinetica.ui.KIconButton
import com.naury.kinetica.ui.KStatus
import com.naury.kinetica.ui.KStepIndicator
import com.naury.kinetica.ui.KStepStyle
import com.naury.kinetica.ui.KTone
import com.naury.kinetica.ui.KTransitionContent
import com.naury.kinetica.ui.KTransitions
import com.naury.kinetica.ui.KVariant
import com.naury.kinetica.ui.shake

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = ChageunTheme.spacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepTopBar(uiState, onBack = { onAction(OnboardingAction.Back) })
        // 앞 단계로 돌아가면 같은 전환을 거꾸로 재생해 왔던 방향으로 밀려 나간다.
        KTransitionContent(
            targetState = uiState.step,
            transition = KTransitions.SharedAxisX,
            isForward = { from, to -> to.ordinal > from.ordinal },
            modifier = Modifier
                .weight(1f)
                .widthIn(max = CONTENT_MAX_WIDTH)
                .fillMaxWidth(),
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
                    KAlert(
                        message = stringResource(R.string.onboarding_save_failed),
                        status = KStatus.Error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
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
    val stepNames = OnboardingStep.entries.drop(1).map { stringResource(it.nameRes) }
    Row(
        modifier = Modifier
            .widthIn(max = CONTENT_MAX_WIDTH)
            .fillMaxWidth()
            .padding(top = ChageunTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        KIconButton(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.onboarding_back),
            onClick = onBack,
            enabled = uiState.canGoBack,
            variant = KVariant.Text,
            tone = KTone.Neutral,
        )
        // 소개 화면은 단계에 넣지 않으므로 차량 번호가 첫 단계(0)다.
        KStepIndicator(
            steps = stepNames,
            current = uiState.step.ordinal - 1,
            style = KStepStyle.Bar,
            modifier = Modifier.weight(1f),
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
        // 저장하는 동안에는 버튼 안에서 링이 돌고 누를 수 없다.
        KButton(
            text = stringResource(labelRes),
            onClick = onPrimary,
            size = KButtonSize.Large,
            fullWidth = true,
            loading = uiState.isSaving,
        )
        // 새 휴대폰으로 바꾼 사람은 차량을 다시 입력하지 않고 클라우드 백업에서 시작한다.
        if (uiState.step == OnboardingStep.Intro) {
            KButton(
                text = stringResource(R.string.onboarding_restore_from_backup),
                onClick = onRestoreFromBackup,
                variant = KVariant.Text,
                fullWidth = true,
            )
        }
        if (uiState.step == OnboardingStep.Notifications) {
            KButton(
                text = stringResource(R.string.onboarding_notifications_later),
                onClick = { onAction(OnboardingAction.Finish) },
                variant = KVariant.Text,
                enabled = !uiState.isSaving,
                fullWidth = true,
            )
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

/** 단계 표시에 쓰는 짧은 이름. TalkBack은 "6단계 중 2단계, 차 정보"처럼 읽는다. */
private val OnboardingStep.nameRes: Int
    get() = when (this) {
        // 소개는 단계 표시에 넣지 않아 이름이 쓰이지 않는다.
        OnboardingStep.Intro, OnboardingStep.Plate -> R.string.onboarding_step_name_plate
        OnboardingStep.VehicleInfo -> R.string.onboarding_step_name_vehicle
        OnboardingStep.Photo -> R.string.onboarding_step_name_photo
        OnboardingStep.Mileage -> R.string.onboarding_step_name_mileage
        OnboardingStep.QuickMaintenance -> R.string.onboarding_step_name_maintenance
        OnboardingStep.Notifications -> R.string.onboarding_step_name_notifications
    }

/** 입력 칸에 오류가 새로 생길 때마다 좌우로 한 번 흔든다. 오류 문구와 색은 따로 보여 준다. */
@Composable
internal fun Modifier.shakeOnError(error: FieldError?): Modifier {
    var count by remember { mutableIntStateOf(0) }
    LaunchedEffect(error) { if (error != null) count++ }
    return shake(count)
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
