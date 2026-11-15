package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {

    private val dateTimeFormat = SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.US)
    private val shortDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return if (digitGroups == 0) {
            "$bytes B"
        } else {
            String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }

    fun formatDateTime(millis: Long): String {
        if (millis <= 0L) return "غير معروف"
        return dateTimeFormat.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        if (millis <= 0L) return "غير معروف"
        return shortDateFormat.format(Date(millis))
    }

    fun formatRelativeArabicDate(millis: Long): String {
        if (millis <= 0L) return "اليوم"
        val diff = (System.currentTimeMillis() - millis).coerceAtLeast(0L)
        val minutes = diff / 60_000L
        val hours = diff / 3_600_000L
        val days = diff / 86_400_000L
        return when {
            minutes < 5L -> "الآن"
            minutes < 60L -> "منذ $minutes د"
            hours == 1L -> "منذ ساعة"
            hours == 2L -> "منذ ساعتين"
            hours < 24L -> "منذ $hours س"
            days <= 1L -> "اليوم"
            days == 2L -> "أمس"
            else -> shortDateFormat.format(Date(millis))
        }
    }
}
