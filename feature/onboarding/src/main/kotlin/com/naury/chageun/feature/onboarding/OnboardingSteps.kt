package com.naury.chageun.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.ChageunCard
import com.naury.chageun.core.designsystem.component.ChoicePill
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.NumericTextStyles
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.ui.ItemIconBadge
import com.naury.chageun.core.ui.R as UiR
import com.naury.chageun.core.ui.VehicleInfoField
import com.naury.chageun.core.ui.VehicleInfoFields
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.icon
import com.naury.kinetica.core.scene.AppearEffect
import com.naury.kinetica.core.scene.AppearKind
import com.naury.kinetica.core.scene.NodeEffects
import com.naury.kinetica.render.kineticaEffects
import com.naury.kinetica.ui.KDivider
import com.naury.kinetica.ui.jellyPress
import com.naury.kinetica.ui.kClickable
import com.naury.kinetica.ui.staggerIn

@Composable
internal fun IntroStep() {
    val primary = MaterialTheme.colorScheme.primary
    val lead = stringResource(R.string.onboarding_intro_lead)
    val emphasis = stringResource(R.string.onboarding_intro_emphasis)
    Text(
        buildAnnotatedString {
            append(lead)
            append('\n')
            withStyle(SpanStyle(color = primary)) { append(emphasis) }
        },
        style = MaterialTheme.typography.displaySmall,
        modifier = Modifier
            .staggerIn(0, delayMs = INTRO_STAGGER_MS)
            .padding(top = ChageunTheme.spacing.lg),
    )
    Image(
        painter = painterResource(UiR.drawable.vehicle_silhouette_suv),
        contentDescription = null,
        modifier = Modifier
            .kineticaEffects(IntroHeroAppear)
            .fillMaxWidth()
            .aspectRatio(INTRO_IMAGE_RATIO)
            .padding(horizontal = ChageunTheme.spacing.lg),
    )
    Text(
        stringResource(R.string.onboarding_intro_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.staggerIn(1, delayMs = INTRO_STAGGER_MS),
    )
    val colors = ChageunTheme.colors
    listOf(
        Triple(Icons.Filled.Build, colors.upcoming, R.string.onboarding_intro_feature_due),
        Triple(Icons.Filled.NotificationsActive, colors.good, R.string.onboarding_intro_feature_remind),
        Triple(Icons.Filled.AutoAwesome, colors.ai, R.string.onboarding_intro_feature_ai),
    ).forEachIndexed { index, (icon, tone, textRes) ->
        // 머리글과 그림이 자리 잡은 뒤 기능 줄이 하나씩 떠오른다.
        Row(
            modifier = Modifier.staggerIn(index + INTRO_FEATURE_FIRST, delayMs = INTRO_STAGGER_MS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        ) {
            ItemIconBadge(icon, tone, size = 40.dp)
            Text(stringResource(textRes), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
internal fun PlateStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepHeader(R.string.onboarding_plate_title, R.string.onboarding_plate_body)
    val error = uiState.errors[OnboardingField.Plate]
    // 실제 번호판처럼 보이는 입력칸. 무엇을 어떤 형식으로 넣어야 하는지 설명 없이 알 수 있다.
    val shape = MaterialTheme.shapes.small
    ChageunCard(
        shape = shape,
        modifier = Modifier
            .shakeOnError(error)
            .fillMaxWidth()
            // 판보다 바깥에 두어야 번호판 테두리가 판 색 위에 그려진다.
            .border(
                2.dp,
                if (error !=
                    null
                ) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                shape,
            ),
    ) {
        Box(Modifier.padding(vertical = 20.dp, horizontal = 16.dp), contentAlignment = Alignment.Center) {
            val plateLabel = stringResource(R.string.onboarding_plate_field)
            val style = NumericTextStyles.Hero.copy(
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (uiState.plate.isEmpty()) {
                Text(
                    stringResource(R.string.onboarding_plate_placeholder_short),
                    style = style.copy(color = MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            BasicTextField(
                value = uiState.plate,
                onValueChange = { onAction(OnboardingAction.PlateChanged(it)) },
                textStyle = style,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                // 자리 표시 번호는 따로 그린 글자라 입력칸 이름으로 읽히지 않는다.
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = plateLabel },
            )
        }
    }
    error?.let { ErrorText(it) }
    // 문구가 길어 큰 글자에서 한 줄짜리 KButton에 다 들어가지 않는다. 줄을 바꾸는 글자 버튼으로 둔다.
    val linkShape = MaterialTheme.shapes.small
    Text(
        stringResource(R.string.onboarding_plate_skip),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .jellyPress()
            .clip(linkShape)
            .kClickable(linkShape) { onAction(OnboardingAction.SkipPlate) }
            .heightIn(min = LINK_MIN_HEIGHT)
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(horizontal = ChageunTheme.spacing.sm),
    )
}

@Composable
internal fun VehicleInfoStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepHeader(R.string.onboarding_vehicle_title, bodyRes = null)
    VehicleInfoFields(
        maker = uiState.maker,
        model = uiState.model,
        modelYear = uiState.modelYear,
        fuelType = uiState.fuelType,
        onMakerChanged = { onAction(OnboardingAction.MakerChanged(it)) },
        onModelChanged = { onAction(OnboardingAction.ModelChanged(it)) },
        onModelYearChanged = { onAction(OnboardingAction.ModelYearChanged(it)) },
        onFuelTypeSelected = { onAction(OnboardingAction.FuelTypeSelected(it)) },
        error = { field -> uiState.errors[field.onboardingField]?.let { ErrorText(it) } },
    )
}

private val VehicleInfoField.onboardingField: OnboardingField
    get() = when (this) {
        VehicleInfoField.Maker -> OnboardingField.Maker
        VehicleInfoField.Model -> OnboardingField.Model
        VehicleInfoField.ModelYear -> OnboardingField.ModelYear
        VehicleInfoField.FuelType -> OnboardingField.FuelType
    }

@Composable
internal fun MileageStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepHeader(R.string.onboarding_mileage_title, R.string.onboarding_mileage_body)
    val error = uiState.errors[OnboardingField.Mileage]
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        ItemIconBadge(Icons.Filled.Speed, ChageunTheme.colors.unknown, size = 64.dp)
        val style = NumericTextStyles.Hero.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        val unit = " " + stringResource(R.string.onboarding_mileage_unit)
        val unitStyle = SpanStyle(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
        )
        val transformation = remember(unit, unitStyle) { MileageTransformation(unit, unitStyle) }
        val focusRequester = remember { FocusRequester() }
        val mileageLabel = stringResource(R.string.onboarding_mileage_field)
        // 이 단계는 숫자 하나만 받으므로 들어오자마자 키패드를 띄운다.
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        BasicTextField(
            value = uiState.mileage,
            onValueChange = { onAction(OnboardingAction.MileageChanged(it)) },
            textStyle = style,
            singleLine = true,
            visualTransformation = transformation,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier
                .shakeOnError(error)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics { contentDescription = mileageLabel },
        )
        KDivider(
            modifier = Modifier.fillMaxWidth(UNDERLINE_WIDTH),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        val year = uiState.modelYear.toIntOrNull()
        val estimate = uiState.mileageEstimate
        if (year != null && estimate != null) {
            ChoicePill(
                label = stringResource(R.string.onboarding_mileage_estimate, year, formatNumber(estimate)),
                selected = uiState.mileage == estimate.toString(),
                onClick = { onAction(OnboardingAction.MileageChanged(estimate.toString())) },
            )
        }
        Text(
            stringResource(R.string.onboarding_mileage_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        error?.let { ErrorText(it) }
    }
}

/**
 * 숫자 문자열은 그대로 저장하고, 화면에는 천 단위 쉼표와 단위를 붙여 보여 준다.
 * 비어 있으면 0을 흐리게 보여 주어 무엇을 넣는 칸인지 알 수 있다.
 */
private class MileageTransformation(private val unit: String, private val unitStyle: SpanStyle) :
    VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val grouped = if (digits.isEmpty()) "0" else digits.reversed().chunked(GROUP).joinToString(",").reversed()
        val display = buildAnnotatedString {
            if (digits.isEmpty()) withStyle(unitStyle) { append(grouped) } else append(grouped)
            withStyle(unitStyle) { append(unit) }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (digits.isEmpty()) return 0
                val commasBefore = (digits.length - 1) / GROUP - (digits.length - offset) / GROUP
                return offset + commasBefore.coerceAtLeast(0)
            }

            override fun transformedToOriginal(offset: Int): Int =
                if (digits.isEmpty()) 0 else grouped.take(offset).count { it != ',' }
        }
        return TransformedText(display, mapping)
    }

    private companion object {
        const val GROUP = 3
    }
}

private const val UNDERLINE_WIDTH = 0.7f
private val LINK_MIN_HEIGHT = 48.dp
private const val INTRO_STAGGER_MS = 90
private const val INTRO_FEATURE_FIRST = 3

/** 소개 그림은 첫 글이 뜬 뒤 살짝 크게 시작해 제자리로 줄며 나타난다. */
private val IntroHeroAppear = NodeEffects(appear = AppearEffect(AppearKind.Zoom, durationMs = 600L, delayMs = 120L))
private const val INTRO_IMAGE_RATIO = 360f / 160f
