package com.naury.chageun.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.motion.motionSpec
import com.naury.chageun.core.designsystem.theme.ChageunTheme

/**
 * 차근의 입력 칸. 테두리 없는 둥근 회색 칸 안에 이름이 있고, 입력하는 동안에만 대표 색 테두리가 생긴다.
 * 오류일 때는 테두리와 아래 안내 글이 위험 색으로 바뀌고, TalkBack은 [supportingText]를 오류 설명으로 읽는다.
 *
 * Material `TextField`와 같은 [BasicTextField]·DecorationBox 구성이라 이름이 올라가는 동작, 접두·접미어,
 * 키보드 옵션은 그대로다. 바탕만 입력 칸 영역에 그려 아래 안내 글은 칸 밖에 놓인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChageunTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    suffix: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    minLines: Int = 1,
    textStyle: TextStyle = LocalTextStyle.current,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = textFieldColors()
    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val errorDescription = supportingText ?: stringResource(R.string.field_error_default)
    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .semantics { if (isError) error(errorDescription) }
                .defaultMinSize(minWidth = TextFieldDefaults.MinWidth, minHeight = TextFieldDefaults.MinHeight),
            enabled = enabled,
            textStyle = textStyle.merge(TextStyle(color = textColor)),
            cursorBrush = SolidColor(
                if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            ),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            minLines = minLines,
        ) { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = singleLine,
                visualTransformation = visualTransformation,
                interactionSource = interaction,
                isError = isError,
                label = label?.let { { Text(it) } },
                placeholder = placeholder?.let { { Text(it) } },
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                suffix = suffix?.let { { Text(it) } },
                supportingText = supportingText?.let { { Text(it) } },
                colors = colors,
                container = { FieldContainer(focused = focused, isError = isError) },
            )
        }
    }
}

/** 칸 바탕. 초점이 오면 대표 색, 오류면 위험 색 테두리를 짧게 바꿔 그린다. */
@Composable
private fun FieldContainer(focused: Boolean, isError: Boolean) {
    val shape = MaterialTheme.shapes.medium
    val border by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            focused -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        animationSpec = motionSpec(),
        label = "field-border",
    )
    Box(
        Modifier
            .background(ChageunTheme.colors.inputFill, shape)
            .border(if (focused) FOCUSED_BORDER else BORDER, border, shape),
    )
}

@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

private val BORDER = 1.dp
private val FOCUSED_BORDER = 1.5.dp
private const val DISABLED_ALPHA = 0.38f
