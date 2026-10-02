package com.naury.chageun.core.database.converter

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/** SQL에서 둘 다 올바르게 정렬되도록 Instant는 UTC epoch millis로, 날짜는 epoch day로 저장한다. */
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
