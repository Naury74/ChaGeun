package com.naury.chageun.core.database.converter

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/** Instants are stored as UTC epoch millis and calendar dates as epoch days so both sort correctly in SQL. */
internal class TimeConverters {
    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
}
