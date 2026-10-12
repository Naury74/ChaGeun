package com.naury.chageun.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naury.chageun.core.designsystem.R

/**
 * 로고와 이름을 나란히 둔 차근 브랜드 제목. 홈 맨 위처럼 앱을 소개하는 자리에 둔다.
 *
 * 로고 크기를 sp로 정해 글자 크기를 키우면 로고도 함께 커진다. 로고는 꾸밈이라 읽지 않고,
 * TalkBack은 [name]을 제목으로 한 번만 읽는다.
 */
@Composable
fun ChageunBrandTitle(name: String, modifier: Modifier = Modifier) {
    val markSize = with(LocalDensity.current) { MARK_SIZE.toDp() }
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.chageun_brand_mark),
            contentDescription = null,
            modifier = Modifier.size(markSize),
        )
        Text(text = name, style = BrandWordmarkStyle, color = MaterialTheme.colorScheme.primary)
    }
}

/** 이름 글자. 굵게 쓰고 자간을 살짝 좁혀 로고처럼 한 덩어리로 보이게 한다. */
private val BrandWordmarkStyle = TextStyle(
    fontSize = 22.sp,
    lineHeight = 28.sp,
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = (-0.6).sp,
)

private val MARK_SIZE = 28.sp
