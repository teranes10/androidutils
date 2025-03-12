package com.example.mytaxy.converters

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime

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