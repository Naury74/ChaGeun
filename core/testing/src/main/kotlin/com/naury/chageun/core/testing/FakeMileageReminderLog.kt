package com.naury.chageun.core.testing

import com.naury.chageun.core.domain.reminder.MileageReminderLog
import java.time.LocalDate

class FakeMileageReminderLog(var lastNotified: LocalDate? = null) : MileageReminderLog {
    override suspend fun lastNotifiedOn() = lastNotified

    override suspend fun markNotified(date: LocalDate) {
        lastNotified = date
    }
}
