package com.naury.chageun.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunGlass
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.ai.OnDeviceAiStatus
import com.naury.kinetica.ui.GlassBackdrop
import com.naury.kinetica.ui.KAssistChip
import com.naury.kinetica.ui.KDropdownMenu
import com.naury.kinetica.ui.KGlassLayer
import com.naury.kinetica.ui.KIconButton
import com.naury.kinetica.ui.KMenuEntry
import com.naury.kinetica.ui.KTone
import com.naury.kinetica.ui.KTopAppBarDropdownTitle
import com.naury.kinetica.ui.KTopAppBarTitleScope
import com.naury.kinetica.ui.KVariant
import com.naury.kinetica.ui.kSurface

/**
 * Gemini처럼 대화 위에 떠 있는 둥근 글래스 입력창. 위에 질문 칸, 아래에 보낼 정보 칩과 보내기(답하는 중에는 멈추기) 버튼.
 * 외부 AI를 고르면 어디서 열리는지 한 줄로 알려 준다.
 */
@Composable
internal fun AiComposer(
    uiState: AiHubUiState,
    actions: AiHubActions,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
) {
    val destinationName = destinationTitle(uiState.destination)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Box(
            Modifier
                .fillMaxWidth()
                .kSurface(ChageunGlass.chrome, ComposerShape, backdrop = backdrop),
        ) {
            // 입력창 판 안의 칩이 판을 뚫고 뒤 대화를 비추지 않도록 판 위 한 겹으로 그린다.
            KGlassLayer {
                Column(
                    Modifier.padding(
                        start = ChageunTheme.spacing.md,
                        end = ChageunTheme.spacing.xxs,
                        top = ChageunTheme.spacing.sm,
                        bottom = ChageunTheme.spacing.xxs,
                    ),
                ) {
                    QuestionField(
                        value = uiState.question,
                        onValueChange = actions.onQuestionChanged,
                        hint = stringResource(R.string.ai_composer_hint, destinationName),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KAssistChip(
                            label = stringResource(R.string.ai_context_chip),
                            onClick = actions.onOpenContext,
                            modifier = Modifier.minimumInteractiveComponentSize(),
                            leadingIcon = Icons.Filled.Info,
                            tone = KTone.Neutral,
                        )
                        Box(Modifier.weight(1f))
                        SendButton(
                            isAnswering = uiState.isAnswering,
                            enabled = (uiState.question.isNotBlank() && !uiState.needsOnDeviceSetup) ||
                                uiState.isAnswering,
                            onSend = actions.onSend,
                            onStop = actions.onStop,
                        )
                    }
                }
            }
        }
        val hint = when {
            uiState.isQuestionMissing -> stringResource(R.string.ai_question_required)
            uiState.needsOnDeviceSetup -> stringResource(R.string.ai_setup_composer_hint)
            uiState.destination.opensInBrowser -> stringResource(R.string.ai_external_hint, destinationName)
            else -> null
        }
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiState.isQuestionMissing) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(horizontal = ChageunTheme.spacing.md),
            )
        }
    }
}

@Composable
private fun QuestionField(value: String, onValueChange: (String) -> Unit, hint: String) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(AiAccent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        maxLines = MAX_INPUT_LINES,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ChageunTheme.spacing.xs)
            .semantics { contentDescription = hint },
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                inner()
            }
        },
    )
}

/** AI 보라로 채운 둥근 보내기 버튼. 답하는 중에는 같은 자리에서 멈추기가 된다. */
@Composable
private fun SendButton(isAnswering: Boolean, enabled: Boolean, onSend: () -> Unit, onStop: () -> Unit) {
    AiAccentColors {
        KIconButton(
            if (isAnswering) Icons.Filled.Stop else Icons.AutoMirrored.Filled.Send,
            contentDescription = stringResource(if (isAnswering) R.string.ai_stop else R.string.ai_send),
            onClick = if (isAnswering) onStop else onSend,
            modifier = Modifier.minimumInteractiveComponentSize(),
            variant = KVariant.Filled,
            enabled = enabled,
        )
    }
}

/** 위쪽 가운데 "차근 AI ▾" 처럼 답할 AI를 고르는 제목과 메뉴. 기기 안 AI와 질문을 채워 여는 외부 AI를 나눠 보여 준다. */
@Composable
internal fun KTopAppBarTitleScope.DestinationPicker(uiState: AiHubUiState, onSelect: (AiDestination) -> Unit) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    val pickerLabel = stringResource(R.string.ai_destination_picker)
    val onDeviceStatus = uiState.onDevice.status
    val entries = listOf(
        KMenuEntry.Section(stringResource(R.string.ai_destination_on_device_body)),
        destinationEntry(
            destination = AiDestination.OnDevice,
            onSelect = onSelect,
            trailing = onDeviceTrailing(onDeviceStatus),
            enabled = onDeviceStatus != null && onDeviceStatus != OnDeviceAiStatus.Unavailable,
        ),
        KMenuEntry.Section(stringResource(R.string.ai_destination_external_section)),
    ) + listOf(AiDestination.ChatGpt, AiDestination.Claude, AiDestination.Gemini, AiDestination.OtherApp)
        .map { destinationEntry(it, onSelect) }
    Box {
        KTopAppBarDropdownTitle(
            text = destinationTitle(uiState.destination),
            expanded = isOpen,
            onClick = { isOpen = !isOpen },
            modifier = Modifier.semantics { contentDescription = pickerLabel },
        )
        KDropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }, entries = entries)
    }
}

@Composable
private fun destinationEntry(
    destination: AiDestination,
    onSelect: (AiDestination) -> Unit,
    trailing: String? = null,
    enabled: Boolean = true,
): KMenuEntry = KMenuEntry.Action(
    label = destinationTitle(destination),
    icon = when (destination) {
        AiDestination.OnDevice -> Icons.Filled.AutoAwesome
        AiDestination.OtherApp -> Icons.Filled.Share
        else -> Icons.Filled.Public
    },
    trailing = trailing,
    enabled = enabled,
    onClick = { onSelect(destination) },
)

/** 기기 안 AI를 아직 바로 쓸 수 없을 때 이름 옆에 짧게 붙이는 상태. */
@Composable
private fun onDeviceTrailing(status: OnDeviceAiStatus?): String? = when (status) {
    null -> stringResource(R.string.ai_destination_on_device_checking_short)
    OnDeviceAiStatus.Available -> null
    OnDeviceAiStatus.Downloadable, OnDeviceAiStatus.Downloading ->
        stringResource(R.string.ai_destination_on_device_download_short)
    OnDeviceAiStatus.Unavailable -> stringResource(R.string.ai_destination_on_device_unavailable_short)
}

@Composable
internal fun destinationTitle(destination: AiDestination): String = stringResource(
    when (destination) {
        AiDestination.OnDevice -> R.string.ai_destination_on_device
        AiDestination.ChatGpt -> R.string.ai_destination_chatgpt
        AiDestination.Claude -> R.string.ai_destination_claude
        AiDestination.Gemini -> R.string.ai_destination_gemini
        AiDestination.OtherApp -> R.string.ai_destination_other
    },
)

internal val AiDestination.opensInBrowser: Boolean
    get() = this == AiDestination.ChatGpt || this == AiDestination.Claude || this == AiDestination.Gemini

private val ComposerShape = RoundedCornerShape(28.dp)
private const val MAX_INPUT_LINES = 5
