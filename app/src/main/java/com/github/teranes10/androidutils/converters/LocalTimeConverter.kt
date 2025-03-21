package com.github.teranes10.androidutils.converters

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.LocalTime

class LocalTimeConverter {
    @TypeConverter
    fun fromLocalTime(dateTime: LocalTime?): String? {
        return dateTime?.toString()
    }

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(value) }
    }
}