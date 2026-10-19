package com.naury.chageun.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.naury.chageun.core.designsystem.component.PressStyle
import com.naury.chageun.core.designsystem.component.pressable
import com.naury.chageun.core.designsystem.theme.ChageunTheme
import com.naury.chageun.core.designsystem.theme.ToneColors
import com.naury.kinetica.ui.KButton
import com.naury.kinetica.ui.KButtonSize
import com.naury.kinetica.ui.KDivider
import com.naury.kinetica.ui.KTone
import com.naury.kinetica.ui.KVariant

/** [ChageunDialog] 버튼의 강조 단계. */
enum class DialogActionStyle {
    /** 권장 동작. 대표 색으로 채운다. */
    Primary,

    /** 되돌리기 어려운 동작(삭제·로그아웃). 위험 색 바탕에 위험 색 글자로 표시한다. */
    Destructive,

    /** 취소·닫기처럼 그 밖의 선택. 바탕 없이 글자만 보인다. */
    Secondary,
}

/** [ChageunDialog]의 버튼 하나. 목록 순서대로 위에서 아래로 쌓인다. */
@Immutable
data class DialogAction(
    val label: String,
    val style: DialogActionStyle = DialogActionStyle.Primary,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/** 대화상자 안에서 고르는 항목 하나. [icon]이 있으면 [tone] 색 원 안에 그린다. */
@Immutable
data class DialogOption(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val tone: ToneColors? = null,
)

/**
 * 차근의 대화상자. 폰에서는 아래에서 올라오는 시트로 뜨고, 손잡이를 끌어 내리거나 바깥을 누르면 내려가며 닫힌다.
 * 가로 600dp 이상 창에서는 가운데 카드로 뜬다.
 *
 * 아이콘·제목·설명은 가운데 정렬이고, 버튼은 큰 글자에서도 잘리지 않도록 창 폭을 다 쓰는 알약 모양으로 세로로 쌓는다.
 * 내용이 길면 버튼은 아래에 둔 채 위쪽만 스크롤한다.
 *
 * @param onDismiss 바깥을 누르거나, 끌어 내리거나, 뒤로 가기를 눌렀을 때. 닫히는 애니메이션이 끝난 뒤 부른다.
 *   버튼을 누를 때는 부르지 않으므로 버튼의 [DialogAction.onClick]에서 창을 닫는다.
 * @param tone 아이콘 원의 색. 삭제처럼 되돌릴 수 없는 일에는 `ChageunTheme.colors.critical`을 넘긴다.
 * @param dismissible false면 진행 중 표시처럼 끝날 때까지 닫을 수 없는 창이다.
 * @param content 설명과 버튼 사이에 넣는 내용(입력 칸, 요약 등). 창 폭을 다 쓰고 왼쪽부터 놓인다.
 */
@Composable
fun ChageunDialog(
    title: String,
    onDismiss: () -> Unit,
    actions: List<DialogAction>,
    icon: ImageVector? = null,
    tone: ToneColors? = null,
    message: String? = null,
    dismissible: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    DialogFrame(
        paneTitle = title,
        onDismiss = onDismiss,
        dismissible = dismissible,
        horizontalPadding = ChageunTheme.spacing.lg,
    ) {
        Spacer(Modifier.height(HEADER_TOP_GAP))
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DialogHeader(title, message, icon, tone ?: defaultTone())
            if (content != null) {
                Spacer(Modifier.height(ChageunTheme.spacing.md))
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
                    content = content,
                )
            }
        }
        if (actions.isNotEmpty()) {
            Spacer(Modifier.height(ChageunTheme.spacing.lg))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xs)) {
                actions.forEach { DialogButton(it, Modifier.fillMaxWidth()) }
            }
        }
    }
}

/**
 * 몇 가지 중 하나를 고르는 [ChageunDialog]. 항목을 누르면 [DialogOption.onClick]만 부르므로 창은 호출한 쪽에서 닫는다.
 * 아래에는 [cancelLabel] 버튼 하나가 있다.
 */
@Composable
fun ChageunOptionDialog(
    title: String,
    options: List<DialogOption>,
    cancelLabel: String,
    onDismiss: () -> Unit,
    message: String? = null,
) {
    ChageunDialog(
        title = title,
        onDismiss = onDismiss,
        message = message,
        actions = listOf(DialogAction(cancelLabel, DialogActionStyle.Secondary, onClick = onDismiss)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(ChageunTheme.colors.inputFill),
        ) {
            options.forEachIndexed { index, option ->
                if (index > 0) {
                    KDivider(startInset = if (option.icon != null) OPTION_TEXT_START else 16.dp)
                }
                OptionRow(option)
            }
        }
    }
}

/**
 * 대화상자 안에서 지워질 것·바뀔 것을 묶어 보여 주는 회색 칸. [title]은 칸 위쪽의 작은 이름이다.
 * 백업 파일과 지금 기기처럼 둘을 비교할 때는 Row 안에 `weight(1f)`로 나란히 놓는다.
 */
@Composable
fun DialogInfoCard(modifier: Modifier = Modifier, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(ChageunTheme.colors.inputFill)
            .padding(horizontal = ChageunTheme.spacing.md, vertical = ChageunTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.xxs),
    ) {
        title?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ProvideTextStyle(MaterialTheme.typography.bodyMedium, content = { content() })
    }
}

/** 대화상자 버튼. 누르면 젤리처럼 눌린다. 날짜 선택 창처럼 버튼을 가로로 놓는 곳에서도 쓴다. */
@Composable
internal fun DialogButton(action: DialogAction, modifier: Modifier = Modifier) {
    val (variant, tone) = when (action.style) {
        DialogActionStyle.Primary -> KVariant.Filled to KTone.Accent
        DialogActionStyle.Destructive -> KVariant.Tonal to KTone.Destructive
        DialogActionStyle.Secondary -> KVariant.Text to KTone.Accent
    }
    KButton(
        text = action.label,
        onClick = action.onClick,
        modifier = modifier,
        variant = variant,
        tone = tone,
        size = KButtonSize.Large,
        enabled = action.enabled,
    )
}

@Composable
private fun DialogHeader(title: String, message: String?, icon: ImageVector?, tone: ToneColors) {
    if (icon != null) {
        Box(Modifier.size(ICON_CIRCLE).background(tone.container, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tone.content, modifier = Modifier.size(ICON_SIZE))
        }
        Spacer(Modifier.height(ChageunTheme.spacing.md))
    }
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    if (message != null) {
        Spacer(Modifier.height(ChageunTheme.spacing.xs))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OptionRow(option: DialogOption) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = OPTION_HEIGHT)
            .pressable(onClick = option.onClick, style = PressStyle.Highlight)
            .padding(horizontal = ChageunTheme.spacing.md, vertical = ChageunTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChageunTheme.spacing.sm),
    ) {
        option.icon?.let { ItemIconBadge(it, option.tone ?: defaultTone(), size = OPTION_ICON) }
        Text(option.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun defaultTone(): ToneColors =
    ToneColors(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)

// 시트 손잡이와 아이콘·제목 사이. 카드에서는 위 여백과 합쳐 예전 카드와 비슷한 높이가 된다.
private val HEADER_TOP_GAP = 4.dp
private val ICON_CIRCLE = 56.dp
private val ICON_SIZE = 28.dp
private val OPTION_HEIGHT = 56.dp
private val OPTION_ICON = 36.dp

// 행 왼쪽 여백(16) + 아이콘(36) + 간격(12). 구분선을 글자 시작에 맞춘다.
private val OPTION_TEXT_START = 64.dp
