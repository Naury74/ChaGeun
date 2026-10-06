package com.naury.chageun.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
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
import com.naury.chageun.core.designsystem.component.ChoiceCard
import com.naury.chageun.core.designsystem.component.ChoicePill
import com.naury.chageun.core.designsystem.motion.ChageunMotion
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.NumericTextStyles
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.ui.ItemIconBadge
import com.naury.chageun.core.ui.R as UiR
import com.naury.chageun.core.ui.formatNumber
import com.naury.chageun.core.ui.icon
import com.naury.chageun.core.ui.labelRes
import java.time.Year

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
        modifier = Modifier.padding(top = ChageunTheme.spacing.lg),
    )
    Image(
        painter = painterResource(UiR.drawable.vehicle_silhouette_suv),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(INTRO_IMAGE_RATIO)
            .padding(horizontal = ChageunTheme.spacing.lg),
    )
    Text(
        stringResource(R.string.onboarding_intro_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val colors = ChageunTheme.colors
    listOf(
        Triple(Icons.Filled.Build, colors.upcoming, R.string.onboarding_intro_feature_due),
        Triple(Icons.Filled.NotificationsActive, colors.good, R.string.onboarding_intro_feature_remind),
        Triple(Icons.Filled.AutoAwesome, colors.ai, R.string.onboarding_intro_feature_ai),
    ).forEachIndexed { index, (icon, tone, textRes) ->
        StaggeredReveal(index) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            ) {
                ItemIconBadge(icon, tone, size = 40.dp)
                Text(stringResource(textRes), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** 처음 그릴 때 아래에서 살짝 올라오며 나타난다. [index]만큼 늦게 시작해 순서대로 읽힌다. */
@Composable
private fun StaggeredReveal(index: Int, content: @Composable () -> Unit) {
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val delay = ChageunMotion.MEDIUM_MS + index * ChageunMotion.STAGGER_MS
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(motionSpec(delayMs = delay)) + slideInVertically(motionSpec(delayMs = delay)) { it / 2 },
    ) { content() }
}

@Composable
internal fun PlateStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepHeader(R.string.onboarding_plate_title, R.string.onboarding_plate_body)
    val error = uiState.errors[OnboardingField.Plate]
    // 실제 번호판처럼 보이는 입력칸. 무엇을 어떤 형식으로 넣어야 하는지 설명 없이 알 수 있다.
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            2.dp,
            if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.padding(vertical = 20.dp, horizontal = 16.dp), contentAlignment = Alignment.Center) {
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
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    error?.let { ErrorText(it) }
    TextButton(onClick = { onAction(OnboardingAction.SkipPlate) }) {
        Text(stringResource(R.string.onboarding_plate_skip))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VehicleInfoStep(uiState: OnboardingUiState, onAction: (OnboardingAction) -> Unit) {
    StepHeader(R.string.onboarding_vehicle_title, bodyRes = null)
    val makerNames = VehicleMaker.entries.associateWith { stringResource(it.nameRes) }
    val selectedMaker = makerNames.entries.firstOrNull { it.value == uiState.maker }?.key
    var isCustomMaker by rememberSaveable { mutableStateOf(uiState.maker.isNotEmpty() && selectedMaker == null) }

    SectionLabel(R.string.onboarding_vehicle_maker)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
    ) {
        VehicleMaker.entries.forEach { maker ->
            ChoicePill(
                label = makerNames.getValue(maker),
                selected = maker == selectedMaker && !isCustomMaker,
                onClick = {
                    isCustomMaker = false
                    if (maker != selectedMaker) {
                        onAction(OnboardingAction.MakerChanged(makerNames.getValue(maker)))
                        onAction(OnboardingAction.ModelChanged(""))
                    }
                },
            )
        }
        ChoicePill(
            label = stringResource(R.string.onboarding_vehicle_other_maker),
            selected = isCustomMaker,
            onClick = {
                if (!isCustomMaker) {
                    isCustomMaker = true
                    onAction(OnboardingAction.MakerChanged(""))
                    onAction(OnboardingAction.ModelChanged(""))
                }
            },
        )
    }
    uiState.errors[OnboardingField.Maker]?.let { ErrorText(it) }
    AnimatedVisibility(visible = isCustomMaker) {
        PlainField(uiState.maker, { onAction(OnboardingAction.MakerChanged(it)) }, R.string.onboarding_vehicle_maker)
    }

    if (selectedMaker != null || isCustomMaker) {
        ModelPicker(
            models = selectedMaker?.let { stringArrayResource(it.modelsRes).toList() }.orEmpty(),
            model = uiState.model,
            onModelChanged = { onAction(OnboardingAction.ModelChanged(it)) },
        )
        uiState.errors[OnboardingField.Model]?.let { ErrorText(it) }
    }

    YearPicker(uiState.modelYear) { onAction(OnboardingAction.ModelYearChanged(it)) }
    uiState.errors[OnboardingField.ModelYear]?.let { ErrorText(it) }

    SectionLabel(R.string.onboarding_vehicle_fuel)
    FuelPicker(uiState.fuelType) { onAction(OnboardingAction.FuelTypeSelected(it)) }
    uiState.errors[OnboardingField.FuelType]?.let { ErrorText(it) }
}

@Composable
private fun FuelPicker(selected: FuelType?, onSelected: (FuelType) -> Unit) {
    // 마지막 줄이 한 칸만 남아도 다른 카드와 너비를 맞추도록 빈 칸을 채우고, 한 줄의 카드 높이를 같게 둔다.
    Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        FuelType.entries.chunked(FUEL_COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            ) {
                row.forEach { fuel ->
                    ChoiceCard(
                        label = stringResource(fuel.labelRes),
                        icon = fuel.icon,
                        selected = selected == fuel,
                        onClick = { onSelected(fuel) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                repeat(FUEL_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelPicker(models: List<String>, model: String, onModelChanged: (String) -> Unit) {
    var isCustom by rememberSaveable { mutableStateOf(models.isEmpty() || (model.isNotEmpty() && model !in models)) }
    SectionLabel(R.string.onboarding_vehicle_model)
    if (models.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        ) {
            models.forEach { name ->
                ChoicePill(
                    label = name,
                    selected = !isCustom && model == name,
                    onClick = {
                        isCustom = false
                        onModelChanged(name)
                    },
                )
            }
            ChoicePill(
                label = stringResource(R.string.onboarding_vehicle_custom),
                selected = isCustom,
                onClick = {
                    isCustom = true
                    onModelChanged("")
                },
            )
        }
    }
    AnimatedVisibility(visible = isCustom || models.isEmpty()) {
        PlainField(model, onModelChanged, R.string.onboarding_vehicle_model)
    }
}

@Composable
private fun YearPicker(modelYear: String, onYearChanged: (String) -> Unit) {
    val newest = remember { Year.now().value + 1 }
    val years = remember(newest) { (newest downTo newest - RECENT_YEARS).map(Int::toString) }
    var isCustom by rememberSaveable { mutableStateOf(modelYear.isNotEmpty() && modelYear !in years) }
    SectionLabel(R.string.onboarding_vehicle_year)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        items(years, key = { it }) { year ->
            ChoicePill(
                label = year,
                selected = !isCustom && modelYear == year,
                onClick = {
                    isCustom = false
                    onYearChanged(year)
                },
            )
        }
        item(key = "older") {
            ChoicePill(
                label = stringResource(R.string.onboarding_vehicle_older),
                selected = isCustom,
                onClick = {
                    isCustom = true
                    onYearChanged("")
                },
            )
        }
    }
    AnimatedVisibility(visible = isCustom) {
        PlainField(modelYear, onYearChanged, R.string.onboarding_vehicle_year, KeyboardType.Number)
    }
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
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
        HorizontalDivider(
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(UNDERLINE_WIDTH),
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

@Composable
internal fun SectionLabel(titleRes: Int) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = ChageunTheme.spacing.xs),
    )
}

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
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
private const val INTRO_IMAGE_RATIO = 360f / 160f
private const val FUEL_COLUMNS = 3
private const val RECENT_YEARS = 15
