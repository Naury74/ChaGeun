package com.naury.chageun.core.model

enum class ThemeMode { System, Light, Dark }

data class UserSettings(val themeMode: ThemeMode = ThemeMode.System, val isMaintenanceReminderEnabled: Boolean = true)
