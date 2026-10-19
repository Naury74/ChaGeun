package com.naury.chageun.core.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.naury.kinetica.ui.KTextField

/**
 * 차근의 입력 칸. Kinetica 입력란이라 초점이 오면 이름이 위로 떠오르고 대표 색 테두리가 부드럽게 나타나며,
 * 글자가 있으면 지우기 버튼이 나온다. 오류가 생기면 테두리가 위험 색으로 바뀌며 한 번 흔들리고, 아래 안내 글이
 * 위험 색으로 바뀐다. TalkBack은 [supportingText]를 오류 설명으로 읽는다.
 *
 * [label] 없이 [placeholder]만 주면 그 글을 이름 자리에 둔다. 빈 칸에도 무엇을 넣는지 보이고, 초점이 오면 위로 비킨다.
 * [modifier]는 입력란과 안내 글을 감싼 바깥 묶음에 붙는다.
 *
 * @param textStyle 예전 호출과 맞추려고 남겨 둔 값이다. 글자 모양은 Kinetica 입력란을 따른다.
 */
@Suppress("UnusedParameter") // textStyle은 기존 호출 코드를 그대로 두려고 받기만 한다.
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
    val errorText = if (isError) supportingText ?: stringResource(R.string.field_error_default) else null
    KTextField(
        value = value,
        onValueChange = onValueChange,
        label = label ?: placeholder.orEmpty(),
        modifier = modifier,
        error = errorText,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        keyboardActions = keyboardActions,
        enabled = enabled,
        singleLine = singleLine,
        placeholder = placeholder.takeIf { label != null },
        supportingText = supportingText.takeIf { !isError },
        leadingIcon = leadingIcon?.let { { MutedIcon(it) } },
        trailingIcon = trailingIcon?.let { { MutedIcon(it) } },
        minLines = minLines,
        suffix = suffix,
    )
}

/** Material 아이콘은 Kinetica 글자 색을 모르므로 칸 안 아이콘을 보조 글자 색으로 맞춘다. */
@Composable
private fun MutedIcon(icon: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant, content = icon)
}
