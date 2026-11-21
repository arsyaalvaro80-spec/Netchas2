package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Formatters {
    private val indonesiaLocale = Locale("id", "ID")

    private val monthNamesId = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    private val shortMonthNamesId = listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )

    fun formatRupiah(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(indonesiaLocale)
        val absFormatted = formatter.format(abs(amount))
        return if (amount < 0) "-Rp $absFormatted" else "Rp $absFormatted"
    }

    fun formatRupiahCompact(amount: Long): String {
        val sign = if (amount < 0) "-" else ""
        val absVal = abs(amount).toDouble()
        return when {
            absVal >= 1_000_000_000.0 -> String.format(indonesiaLocale, "%sRp %.1f M", sign, absVal / 1_000_000_000.0)
            absVal >= 1_000_000.0 -> String.format(indonesiaLocale, "%sRp %.1f jt", sign, absVal / 1_000_000.0)
            absVal >= 1_000.0 -> String.format(indonesiaLocale, "%sRp %.0f rb", sign, absVal / 1_000.0)
            else -> "${sign}Rp ${abs(amount)}"
        }
    }

    fun currentYearMonth(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

    fun parseYearMonth(yearMonth: String): Pair<Int, Int> {
        val parts = yearMonth.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1))
            .coerceIn(1, 12)
        return year to month
    }

    fun shiftYearMonth(yearMonth: String, deltaMonths: Int): String {
        val (year, month) = parseYearMonth(yearMonth)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, deltaMonths)
        }
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun lastSixMonthsEndingAt(yearMonth: String): List<String> =
        (-5..0).map { offset -> shiftYearMonth(yearMonth, offset) }

    fun formatYearMonthFull(yearMonth: String): String {
        val (year, month) = parseYearMonth(yearMonth)
        return "${monthNamesId[month - 1]} $year"
    }

    fun formatYearMonthShort(yearMonth: String): String {
        val (year, month) = parseYearMonth(yearMonth)
        return "${shortMonthNamesId[month - 1]} ${year.toString().takeLast(2)}"
    }

    fun formatDateFull(millis: Long): String {
        if (millis <= 0L) return "-"
        val sdf = SimpleDateFormat("dd MMM yyyy", indonesiaLocale)
        return sdf.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        if (millis <= 0L) return "-"
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", indonesiaLocale)
        return sdf.format(Date(millis))
    }

    fun defaultDateMillisForMonth(yearMonth: String): Long {
        val current = currentYearMonth()
        if (yearMonth == current) return System.currentTimeMillis()
        val (year, month) = parseYearMonth(yearMonth)
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, minOf(15, maxDay))
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun daysUntilDueDate(yearMonth: String, dueDay: Int): Int {
        val (year, month) = parseYearMonth(yearMonth)
        val dueCal = Calendar.getInstance().apply {
            clear()
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, dueDay.coerceIn(1, maxDay))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = dueCal.timeInMillis - todayCal.timeInMillis
        return Math.round(diffMillis.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)).toInt()
    }

    fun normalizeIndonesianPhoneForWhatsApp(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.startsWith("62") -> digits
            digits.startsWith("0") -> "62" + digits.drop(1)
            digits.startsWith("8") -> "62$digits"
            else -> digits
        }
    }
}
