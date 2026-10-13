package com.naury.chageun.feature.ai

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.PressStyle
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.theme.ChageunTheme

/** AI 화면 곳곳에 쓰는 파랑→보라→분홍 그라데이션. AI가 답하는 곳이라는 표시다. */
internal val AiGradient = Brush.linearGradient(listOf(Color(0xFF4F7DF3), Color(0xFF9B72CB), Color(0xFFD96570)))

/** 대화를 시작하기 전 화면. 큰 그라데이션 인사와 바로 물어볼 수 있는 제안 카드. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiWelcome(vehicleName: String?, suggestions: List<String>, onSuggestion: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ChageunTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.md),
    ) {
        GradientSparkle(size = 40.dp)
        Text(
            stringResource(R.string.ai_greeting),
            style = MaterialTheme.typography.headlineMedium.merge(TextStyle(brush = AiGradient)),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            vehicleName?.let { stringResource(R.string.ai_greeting_body, it) }
                ?: stringResource(R.string.ai_greeting_body_generic),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.padding(top = ChageunTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
            maxItemsInEachRow = 2,
        ) {
            suggestions.forEachIndexed { index, text ->
                SuggestionCard(
                    text = text,
                    icon = SUGGESTION_ICONS[index % SUGGESTION_ICONS.size],
                    onClick = { onSuggestion(text) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SuggestionCard(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.pressable(onClick = onClick, style = PressStyle.Scale),
    ) {
        Column(
            modifier = Modifier.padding(ChageunTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp).gradientTint())
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** 사용자 질문은 오른쪽 말풍선, AI 답은 왼쪽에 반짝이와 함께 말풍선 없이 둔다. */
@Composable
internal fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    when (message.role) {
        ChatRole.User -> Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.widthIn(max = USER_BUBBLE_MAX_WIDTH),
            ) {
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(
                        horizontal = ChageunTheme.spacing.md,
                        vertical = ChageunTheme.spacing.sm,
                    ),
                )
            }
        }
        ChatRole.Assistant -> Row(
            modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
        ) {
            GradientSparkle(size = 28.dp, spinning = message.isStreaming)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs),
            ) {
                when {
                    message.isFailed -> Text(
                        stringResource(R.string.ai_answer_failed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    message.text.isEmpty() -> ThinkingText()
                    else -> Text(answerText(message.text), style = MaterialTheme.typography.bodyLarge)
                }
                if (!message.isStreaming && !message.isFailed) {
                    Text(
                        stringResource(R.string.ai_answer_caption),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingText() {
    val alpha by rememberInfiniteTransition(label = "ai-thinking").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "ai-thinking-alpha",
    )
    Text(
        stringResource(R.string.ai_thinking),
        style = MaterialTheme.typography.bodyLarge.merge(TextStyle(brush = AiGradient)),
        modifier = Modifier.alpha(alpha),
    )
}

/** 그라데이션 반짝이. 답을 만드는 동안 천천히 돈다. */
@Composable
internal fun GradientSparkle(size: Dp, modifier: Modifier = Modifier, spinning: Boolean = false) {
    val rotation by rememberInfiniteTransition(label = "ai-sparkle").animateFloat(
        initialValue = 0f,
        targetValue = if (spinning) FULL_TURN else 0f,
        animationSpec = infiniteRepeatable(tween(SPIN_MILLIS, easing = LinearEasing)),
        label = "ai-sparkle-rotation",
    )
    Icon(
        Icons.Filled.AutoAwesome,
        contentDescription = null,
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
            .gradientTint(),
    )
}

/** 아이콘에 그라데이션을 입힌다. 아이콘 모양을 마스크로 써서 그 위에 그라데이션을 칠한다. */
internal fun Modifier.gradientTint(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRect(AiGradient, blendMode = BlendMode.SrcAtop)
        }
    }

/** 보내기 버튼 같은 동그란 그라데이션 바탕. */
internal fun Modifier.gradientCircle(enabled: Boolean, disabled: Color): Modifier =
    if (enabled) background(AiGradient, CircleShape) else background(disabled, CircleShape)

private val SUGGESTION_ICONS = listOf(
    Icons.AutoMirrored.Filled.Chat,
    Icons.Filled.Build,
    Icons.Filled.Route,
    Icons.Filled.Payments,
)
private val USER_BUBBLE_MAX_WIDTH = 320.dp
private const val PULSE_MILLIS = 700
private const val SPIN_MILLIS = 2_400
private const val FULL_TURN = 360f
