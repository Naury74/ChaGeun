package com.naury.chageun.core.model

enum class ThemeMode { System, Light, Dark }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val isMaintenanceReminderEnabled: Boolean = true,
    /** 기획서 14.3: 주행거리 입력 알림은 사용자가 켠 경우에만 보낸다. */
    val isMileageReminderEnabled: Boolean = false,
    /** 개인정보 없는 사용 이벤트(AnalyticsEvent)를 Firebase Analytics로 보낼지 여부. */
    val isUsageStatsEnabled: Boolean = true,
    /** 로그인한 사용자가 켜면 주 1회 Wi-Fi·충전 중에 클라우드에 백업한다. */
    val isCloudAutoBackupEnabled: Boolean = false,
)
