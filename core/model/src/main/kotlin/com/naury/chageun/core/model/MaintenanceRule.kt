package com.naury.chageun.core.model

enum class RuleSource { Generic, Manufacturer, User }

data class MaintenanceThresholds(
    val dueSoonKm: Long = 500,
    val dueSoonDays: Long = 14,
    val upcomingKm: Long = 2_000,
    val upcomingDays: Long = 60,
) {
    init {
        require(dueSoonKm <= upcomingKm && dueSoonDays <= upcomingDays) {
            "Due-soon thresholds must not exceed upcoming thresholds"
        }
    }
}

data class MaintenanceRule(
    val item: MaintenanceItem,
    val intervalKm: Long?,
    val intervalMonths: Long?,
    val thresholds: MaintenanceThresholds = MaintenanceThresholds(),
    val source: RuleSource = RuleSource.Generic,
    val sourceTitle: String? = null,
    val sourceUrl: String? = null,
    val isEnabled: Boolean = true,
) {
    init {
        require(intervalKm != null || intervalMonths != null) { "A rule needs a distance or time interval" }
        require((intervalKm ?: 1) > 0 && (intervalMonths ?: 1) > 0) { "Intervals must be positive" }
    }
}
