package com.github.teranes10.androidutils.converters

import androidx.room.TypeConverter
import java.time.LocalDate

class LocalDateConverter {
    @TypeConverter
    fun fromLocalDateTime(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(value) }
    }
}