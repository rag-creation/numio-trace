package com.rr.numio.trace.ui

import com.rr.numio.trace.data.dateOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

object Fmt {
    private val timeFmt get() = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val dayMonth get() = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val dayMonthYear get() = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val weekdayDayMonth get() = DateTimeFormatter.ofPattern("EEE · d MMM", Locale.getDefault())
    private val full get() = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())

    fun time(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFmt)

    /** Heading for a day in the timeline. */
    fun dayLabel(d: LocalDate, today: LocalDate): String = when {
        d == today -> "Today · " + d.format(dayMonth)
        d == today.minusDays(1) -> "Yesterday"
        d.year == today.year -> d.format(weekdayDayMonth)
        else -> d.format(full)
    }

    fun fullDate(ms: Long): String = dateOf(ms).format(full)

    fun shortDate(d: LocalDate, today: LocalDate): String =
        if (d.year == today.year) d.format(dayMonth) else d.format(dayMonthYear)

    /** "Today", "Yesterday", "12 days ago", "3 months ago"… */
    fun ago(d: LocalDate, today: LocalDate): String {
        val days = ChronoUnit.DAYS.between(d, today)
        return when {
            days <= 0 -> "Today"
            days == 1L -> "Yesterday"
            days < 60 -> "$days days ago"
            days < 730 -> "${(days / 30.4).roundToInt()} months ago"
            else -> "${(days / 365.25).roundToInt()} years ago"
        }
    }

    /** Short form for "Last: …" lines. */
    fun agoShort(d: LocalDate, today: LocalDate): String {
        val days = ChronoUnit.DAYS.between(d, today)
        return if (days in 2..59) "$days days ago" else ago(d, today)
    }

    /** "~10 days", "~2 weeks", "~3 months", "~1 year". */
    fun usual(days: Double): String = when {
        days < 1.5 -> "~1 day"
        days < 14 -> "~${days.roundToInt()} days"
        days < 60 -> plural((days / 7).roundToInt(), "week")
        days < 365 -> plural((days / 30.4).roundToInt(), "month")
        else -> plural((days / 365.25).roundToInt(), "year")
    }

    private fun plural(n: Int, unit: String) = if (n == 1) "~1 $unit" else "~$n ${unit}s"

    fun gap(days: Long): String = when (days) {
        0L -> "Same day"
        1L -> "1 day later"
        else -> "$days days later"
    }

    fun times(n: Int): String = if (n == 1) "once" else "$n times"

    /** For the "Coming round again" cards. */
    fun due(daysUntil: Long): String = when {
        daysUntil < -1 -> "Due ${-daysUntil} days ago"
        daysUntil == -1L -> "Due yesterday"
        daysUntil == 0L -> "Due today"
        daysUntil == 1L -> "Due tomorrow"
        daysUntil < 60 -> "Due in $daysUntil days"
        else -> "In ${plural((daysUntil / 30.4).roundToInt(), "month").removePrefix("~")}"
    }
}
