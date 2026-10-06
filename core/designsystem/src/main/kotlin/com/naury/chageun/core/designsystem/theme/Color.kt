package com.naury.chageun.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// 배경·표면·구분선은 iOS 그룹 목록(systemGroupedBackground)과 같은 값을 쓴다.
// 글자로 쓰이는 색은 바탕(흰색·배경·연한 컨테이너) 위에서 대비 4.5:1 이상이 되도록 골랐다.
internal object LightTokens {
    val Background = Color(0xFFF2F2F7)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFEBEBF0)
    val OnSurface = Color(0xFF1C1C1E)
    val OnSurfaceVariant = Color(0xFF5E5E66)
    val Outline = Color(0xFFC6C6C8)
    val Separator = Color(0xFFE0E0E5)
    val Primary = Color(0xFF2864F0)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFE3ECFF)
    val OnPrimaryContainer = Color(0xFF0B2E80)
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
    val OnSurface = Color(0xFFF2F2F7)
    val OnSurfaceVariant = Color(0xFFA1A1A6)
    val Outline = Color(0xFF48484A)
    val Separator = Color(0xFF38383A)
    val Primary = Color(0xFF8AB0FF)
    val OnPrimary = Color(0xFF0A2A73)
    val PrimaryContainer = Color(0xFF1C3A8F)
    val OnPrimaryContainer = Color(0xFFDCE6FF)
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
