package com.naury.chageun.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.ChoicePill
import com.naury.chageun.core.designsystem.motion.ChageunMotion
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.ToneColors
import java.time.LocalDate

/** 시트 맨 위의 아이콘과 제목. 어떤 기록을 남기는지 아이콘으로 먼저 알려 준다. */
@Composable
fun FormHeader(icon: ImageVector, tone: ToneColors, title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIconBadge(icon, tone, size = 48.dp)
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    }
}

/** 입력 묶음 위의 작은 제목. */
@Composable
fun FormLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * 기록은 대부분 오늘이나 어제 일이므로 두 칩으로 바로 고르고, 그 외 날짜만 달력을 연다.
 * 달력에서 고른 날짜는 세 번째 칩에 표시해 무엇을 골랐는지 계속 보이게 한다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickDateField(
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = remember { LocalDate.now() },
) {
    var isPickerOpen by rememberSaveable { mutableStateOf(false) }
    val yesterday = today.minusDays(1)
    val isOther = date != null && date != today && date != yesterday
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
    ) {
        ChoicePill(stringResource(R.string.date_today), date == today, { onDateSelected(today) })
        ChoicePill(stringResource(R.string.date_yesterday), date == yesterday, { onDateSelected(yesterday) })
        ChoicePill(
            label = if (isOther) formatDate(checkNotNull(date)) else stringResource(R.string.date_other),
            selected = isOther,
            onClick = { isPickerOpen = true },
        )
    }
    if (isPickerOpen) {
        PastDatePickerDialog(date = date, onDateSelected = onDateSelected, onDismiss = { isPickerOpen = false })
    }
}

/** 빠르게 고르는 값 하나. [label]은 칩에 보이는 글자, [value]는 입력란에 들어갈 숫자 문자열이다. */
data class QuickPick(val label: String, val value: String)

/**
 * 숫자 입력란과 자주 쓰는 값 칩. 칩을 누르면 입력란이 채워지고, 칩에 없는 값만 직접 입력한다.
 * 입력란은 숫자만 저장하고 화면에는 천 단위 쉼표를 붙인다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    modifier: Modifier = Modifier,
    picks: List<QuickPick> = emptyList(),
    errorText: String? = null,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Number,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            suffix = { Text(unit) },
            isError = errorText != null,
            supportingText = (errorText ?: supportingText)?.let { { Text(it) } },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium,
            visualTransformation = if (keyboardType == KeyboardType.Number) {
                GroupedNumberTransformation
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        if (picks.isNotEmpty()) {
            // 칩이 줄을 바꾸면 입력란과 멀어져 보이므로 한 줄로 두고 가로로 넘긴다.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            ) {
                picks.forEach { pick ->
                    ChoicePill(pick.label, selected = value == pick.value, onClick = { onValueChange(pick.value) })
                }
            }
        }
    }
}

/** 정비소·메모처럼 대부분 비워 두는 입력을 접어 둔다. 이미 값이 있으면 펼친 상태로 시작한다. */
@Composable
fun OptionalSection(hasValue: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var isOpen by rememberSaveable { mutableStateOf(hasValue) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        TextButton(onClick = { isOpen = !isOpen }) {
            Text(stringResource(if (isOpen) R.string.form_fewer_details else R.string.form_more_details))
            Icon(
                if (isOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
            )
        }
        AnimatedVisibility(visible = isOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm)) { content() }
        }
    }
}

/** 저장이 끝났음을 알리는 체크 표시. 원이 커지며 나타나고, Reduce Motion에서는 바로 보인다. */
@Composable
fun SaveSuccessMark(modifier: Modifier = Modifier) {
    var isShown by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        isShown = true
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    val scale by animateFloatAsState(
        targetValue = if (isShown) 1f else INITIAL_SCALE,
        animationSpec = motionSpec(ChageunMotion.MEDIUM_MS),
        label = "save-success",
    )
    val tone = ChageunTheme.colors.good
    Box(
        modifier = modifier
            .size(72.dp)
            .scale(scale)
            .background(tone.container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = tone.content, modifier = Modifier.size(40.dp))
    }
}

/** 숫자 문자열을 그대로 두고 화면에만 천 단위 쉼표를 넣는다. 커서 위치는 쉼표 수만큼 옮긴다. */
object GroupedNumberTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val grouped = groupDigits(digits)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (digits.isEmpty()) return 0
                val commasBefore = (digits.length - 1) / GROUP - (digits.length - offset) / GROUP
                return offset + commasBefore.coerceAtLeast(0)
            }

            override fun transformedToOriginal(offset: Int): Int = grouped.take(offset).count { it != ',' }
        }
        return TransformedText(AnnotatedString(grouped), mapping)
    }

    fun groupDigits(digits: String): String = digits.reversed().chunked(GROUP).joinToString(",").reversed()

    private const val GROUP = 3
}

private const val INITIAL_SCALE = 0.4f
