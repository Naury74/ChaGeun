package com.naury.chageun.feature.ai

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.naury.chageun.core.designsystem.component.ChageunCard
import com.naury.chageun.core.designsystem.component.PressStyle
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.kinetica.ui.KLoadingIndicator
import com.naury.kinetica.ui.KLoadingStyle
import com.naury.kinetica.ui.KineticaTheme
import com.naury.kinetica.ui.LocalKineticaColors
import com.naury.kinetica.ui.staggerIn

/** AI 화면 곳곳에 쓰는 파랑→보라→분홍 그라데이션. AI가 답하는 곳이라는 표시다. */
internal val AiGradient = Brush.linearGradient(listOf(Color(0xFF4F7DF3), Color(0xFF9B72CB), Color(0xFFD96570)))

/** 그라데이션 가운데 보라. 보내기 버튼처럼 한 색으로 채우는 곳에 쓴다. */
internal val AiAccent = Color(0xFF9B72CB)

/** 안쪽 Kinetica 컨트롤(채운 버튼, 진행 막대)의 강조색을 AI 보라로 바꾼다. 흰 글자와 대비가 모자라면 Kinetica가 알맞게 진하게 칠한다. */
@Composable
internal fun AiAccentColors(content: @Composable () -> Unit) {
    val colors = KineticaTheme.colors.copy(accent = AiAccent, onAccent = Color.White)
    CompositionLocalProvider(LocalKineticaColors provides colors, content = content)
}

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
                    // 처음 열 때 인사 아래로 차례로 떠오른다.
                    modifier = Modifier
                        .weight(1f)
                        .staggerIn(index),
                )
            }
        }
    }
}

@Composable
private fun SuggestionCard(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ChageunCard(
        shape = SuggestionShape,
        modifier = modifier.pressable(onClick = onClick, style = PressStyle.Scale, shape = SuggestionShape),
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
            ChageunCard(
                shape = UserBubbleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
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
            GradientSparkle(size = 28.dp)
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
                    message.text.isEmpty() -> KLoadingIndicator(
                        style = KLoadingStyle.Dots,
                        color = AiAccent,
                        size = THINKING_SIZE,
                        label = stringResource(R.string.ai_thinking),
                    )
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

/** 그라데이션 반짝이. AI가 답하는 자리라는 표시다. */
@Composable
internal fun GradientSparkle(size: Dp, modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.AutoAwesome,
        contentDescription = null,
        modifier = modifier
            .size(size)
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

private val SUGGESTION_ICONS = listOf(
    Icons.AutoMirrored.Filled.Chat,
    Icons.Filled.Build,
    Icons.Filled.Route,
    Icons.Filled.Payments,
)
private val USER_BUBBLE_MAX_WIDTH = 320.dp
private val UserBubbleShape =
    RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
private val SuggestionShape = RoundedCornerShape(20.dp)
private val THINKING_SIZE = 28.dp
