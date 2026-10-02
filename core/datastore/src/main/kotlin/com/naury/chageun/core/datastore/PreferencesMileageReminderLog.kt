package com.naury.chageun.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.naury.chageun.core.domain.reminder.MileageReminderLog
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

internal class PreferencesMileageReminderLog @Inject constructor(private val dataStore: DataStore<Preferences>) :
    MileageReminderLog {

    override suspend fun lastNotifiedOn(): LocalDate? =
        dataStore.data.first()[LAST_NOTIFIED_EPOCH_DAY]?.let(LocalDate::ofEpochDay)

    override suspend fun markNotified(date: LocalDate) {
        dataStore.edit { it[LAST_NOTIFIED_EPOCH_DAY] = date.toEpochDay() }
    }

    private companion object {
        val LAST_NOTIFIED_EPOCH_DAY = longPreferencesKey("mileage_reminder_last_notified")
    }
}
