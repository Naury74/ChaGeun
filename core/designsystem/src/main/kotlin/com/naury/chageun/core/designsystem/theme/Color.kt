package com.naury.chageun.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// 배경·표면·구분선은 iOS 그룹 목록(systemGroupedBackground)과 같은 값을 쓴다.
// 글자로 쓰이는 색은 바탕(흰색·배경·연한 컨테이너) 위에서 대비 4.5:1 이상이 되도록 골랐다.
internal object LightTokens {
    val Background = Color(0xFFF2F2F7)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFEBEBF0)
    val SurfaceRaised = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFF1C1C1E)
    val OnSurfaceVariant = Color(0xFF5E5E66)
    val Outline = Color(0xFFC6C6C8)
    val Separator = Color(0xFFE0E0E5)

    // 대표 색은 Hero 요약 카드와 같은 계열의 남색이다.
    val Primary = Color(0xFF24345A)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFE4E9F3)
    val OnPrimaryContainer = Color(0xFF16213B)
    val TonalContainer = PrimaryContainer
    val OnTonalContainer = OnPrimaryContainer
    val AiAccent = Color(0xFF6B4EE6)
    val AiContainer = Color(0xFFF0ECFF)
    val Good = Color(0xFF17784D)
    val GoodContainer = Color(0xFFE3F6EC)
    val Upcoming = Color(0xFFA15C00)
    val UpcomingContainer = Color(0xFFFFF3DC)
    val Critical = Color(0xFFC62828)
    val CriticalContainer = Color(0xFFFDE8E8)
    val Unknown = Color(0xFF5E5E66)
    val UnknownContainer = Color(0xFFEBEBF0)
}

internal object DarkTokens {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF1C1C1E)
    val SurfaceVariant = Color(0xFF2C2C2E)

    // 회색 트랙 위에 떠 있는 선택 막대. 다크에서는 트랙보다 밝아야 선택이 보인다.
    val SurfaceRaised = Color(0xFF636366)
    val OnSurface = Color(0xFFF2F2F7)
    val OnSurfaceVariant = Color(0xFFA1A1A6)
    val Outline = Color(0xFF48484A)
    val Separator = Color(0xFF38383A)

    // 검은 바탕에서 읽히도록 남색을 밝힌 색을 쓴다.
    val Primary = Color(0xFFA8BBE8)
    val OnPrimary = Color(0xFF14203A)
    val PrimaryContainer = Color(0xFF1F2C4A)
    val OnPrimaryContainer = Color(0xFFDCE4F7)

    // 버튼·선택된 칩처럼 누르는 것은 검은 바탕에서 묻히지 않도록 선택 배경보다 밝은 남색에 흰 글자를 쓴다.
    // 선택된 카드 배경(PrimaryContainer)은 회색 보조 글자의 대비를 지키려고 어둡게 둔다.
    val TonalContainer = Color(0xFF2E4170)
    val OnTonalContainer = Color(0xFFFFFFFF)
    val AiAccent = Color(0xFFC5B6FF)
    val AiContainer = Color(0xFF2E2650)
    val Good = Color(0xFF6FD9A6)
    val GoodContainer = Color(0xFF173828)
    val Upcoming = Color(0xFFF5C46B)
    val UpcomingContainer = Color(0xFF3A2D10)
    val Critical = Color(0xFFFF9C94)
    val CriticalContainer = Color(0xFF4D1B1B)
    val Unknown = Color(0xFFA1A1A6)
    val UnknownContainer = Color(0xFF2C2C2E)
}
