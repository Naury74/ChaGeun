package com.naury.chageun.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.domain.ai.OnDeviceAiStatus

/**
 * Gemini처럼 아래에 붙은 둥근 입력창. 위에 질문 칸, 아래에 보낼 정보 칩과 보내기(답하는 중에는 멈추기) 버튼.
 * 외부 AI를 고르면 어디서 열리는지 한 줄로 알려 준다.
 */
@Composable
internal fun AiComposer(uiState: AiHubUiState, actions: AiHubActions, modifier: Modifier = Modifier) {
    val destinationName = destinationTitle(uiState.destination)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(
                    start = ChageunTheme.spacing.md,
                    end = ChageunTheme.spacing.xs,
                    top = ChageunTheme.spacing.sm,
                ),
            ) {
                QuestionField(
                    value = uiState.question,
                    onValueChange = actions.onQuestionChanged,
                    hint = stringResource(R.string.ai_composer_hint, destinationName),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = actions.onOpenContext,
                        label = { Text(stringResource(R.string.ai_context_chip)) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        shape = CircleShape,
                    )
                    Box(Modifier.weight(1f))
                    SendButton(
                        isAnswering = uiState.isAnswering,
                        enabled = (uiState.question.isNotBlank() && !uiState.needsOnDeviceSetup) || uiState.isAnswering,
                        onSend = actions.onSend,
                        onStop = actions.onStop,
                    )
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
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
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

@Composable
private fun SendButton(isAnswering: Boolean, enabled: Boolean, onSend: () -> Unit, onStop: () -> Unit) {
    IconButton(
        onClick = if (isAnswering) onStop else onSend,
        enabled = enabled,
        modifier = Modifier
            .padding(ChageunTheme.spacing.xxs)
            .gradientCircle(enabled = enabled, disabled = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Icon(
            if (isAnswering) Icons.Filled.Stop else Icons.AutoMirrored.Filled.Send,
            contentDescription = stringResource(if (isAnswering) R.string.ai_stop else R.string.ai_send),
            tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 위쪽 가운데 "차근 AI ▾" 처럼 답할 AI를 고르는 버튼과 메뉴. */
@Composable
internal fun DestinationPicker(uiState: AiHubUiState, onSelect: (AiDestination) -> Unit) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    val pickerLabel = stringResource(R.string.ai_destination_picker)
    Box {
        TextButton(
            onClick = { isOpen = true },
            modifier = Modifier.semantics { contentDescription = pickerLabel },
        ) {
            Text(destinationTitle(uiState.destination), style = MaterialTheme.typography.titleMedium)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            AiDestination.entries.forEach { destination ->
                val onDeviceStatus = uiState.onDevice.status
                val enabled = destination != AiDestination.OnDevice ||
                    (onDeviceStatus != null && onDeviceStatus != OnDeviceAiStatus.Unavailable)
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(destinationTitle(destination), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                destinationBody(destination, onDeviceStatus),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    leadingIcon = { DestinationIcon(destination) },
                    enabled = enabled,
                    onClick = {
                        isOpen = false
                        onSelect(destination)
                    },
                )
            }
        }
    }
}

@Composable
private fun DestinationIcon(destination: AiDestination) {
    when (destination) {
        AiDestination.OnDevice -> GradientSparkle(size = 24.dp)
        AiDestination.OtherApp -> Icon(Icons.Filled.Share, contentDescription = null)
        else -> Icon(Icons.Filled.Public, contentDescription = null)
    }
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

@Composable
private fun destinationBody(destination: AiDestination, onDeviceStatus: OnDeviceAiStatus?): String = stringResource(
    when (destination) {
        AiDestination.OnDevice -> when (onDeviceStatus) {
            null -> R.string.ai_destination_on_device_checking
            OnDeviceAiStatus.Available -> R.string.ai_destination_on_device_body
            OnDeviceAiStatus.Downloadable, OnDeviceAiStatus.Downloading -> R.string.ai_destination_on_device_download
            OnDeviceAiStatus.Unavailable -> R.string.ai_destination_on_device_unavailable
        }
        AiDestination.ChatGpt, AiDestination.Claude -> R.string.ai_destination_web_body
        AiDestination.Gemini -> R.string.ai_destination_gemini_body
        AiDestination.OtherApp -> R.string.ai_destination_other_body
    },
)

internal val AiDestination.opensInBrowser: Boolean
    get() = this == AiDestination.ChatGpt || this == AiDestination.Claude || this == AiDestination.Gemini

private const val MAX_INPUT_LINES = 5
