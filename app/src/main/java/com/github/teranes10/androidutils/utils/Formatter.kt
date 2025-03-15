package com.github.teranes10.androidutils.utils

import android.annotation.SuppressLint
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Formatter {
    private const val TAG = "Formatter"
    private val UTC_ZONE_ID: ZoneId = ZoneId.of("UTC")

    val HH_mm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val HH_mm_ss: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val yyyyMMdd: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    val DOT_NET_DATETIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    val DOT_NET_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val DOT_NET_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val FILE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_'T'_HH_mm_ss")
    val DATETIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd - HH:mm")
    val MAIN_DATETIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd HH:mm")
    val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val SIMPLE_DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-M-yyyy HH:mm:ss")
    val DAY_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM")

    fun LocalDateTime.isWeekEnd(): Boolean {
        val dayOfWeek = this.dayOfWeek
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
    }

    fun LocalDate.isWeekEnd(): Boolean {
        val dayOfWeek = this.dayOfWeek
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
    }

    fun Instant.format(pattern: String): String {
        return this.atZone(UTC_ZONE_ID).format(DateTimeFormatter.ofPattern(pattern))
    }

    fun parseToInstant(str: String): Instant {
        val adjustedStr = if (!str.endsWith("Z")) str + "Z" else str
        return Instant.parse(adjustedStr)
    }

    @SuppressLint("DefaultLocale")
    fun formatTime(millis: Long, includeHours: Boolean = true): String {
        if (millis <= 0) {
            return if (includeHours) "00:00:00" else "00:00"
        }

        val seconds = (millis / 1000).toInt() % 60
        val minutes = ((millis / (1000 * 60)) % 60).toInt()
        val hours = ((millis / (1000 * 60 * 60)) % 24).toInt()

        return if (includeHours) String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }
}
