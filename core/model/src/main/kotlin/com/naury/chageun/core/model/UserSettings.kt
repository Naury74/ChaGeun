package com.naury.chageun.core.model

enum class ThemeMode { System, Light, Dark }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    /** 소모품·기간형 정비 알림(기획 §16.2). */
    val isMaintenanceReminderEnabled: Boolean = true,
    /** 자동차 검사 기한 알림. 정비 알림과 따로 끌 수 있다(기획 §16.4). */
    val isInspectionReminderEnabled: Boolean = true,
    /** 기획서 14.3: 주행거리 입력 알림은 사용자가 켠 경우에만 보낸다. */
    val isMileageReminderEnabled: Boolean = false,
    /** 개인정보 없는 사용 이벤트(AnalyticsEvent)를 Firebase Analytics로 보낼지 여부. */
    val isUsageStatsEnabled: Boolean = true,
    /** 로그인한 사용자가 켜면 주 1회 Wi-Fi·충전 중에 클라우드에 백업한다. */
    val isCloudAutoBackupEnabled: Boolean = false,
)
