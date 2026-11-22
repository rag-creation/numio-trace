package com.rr.numio.trace.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

fun dateOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

/** Every time you did one thing, newest first, plus what Trace can tell from them. */
class Thing(val key: String, val entries: List<Entry>) {
    val latest: Entry = entries.first()
    val title: String = latest.title
    val tag: String = entries.firstOrNull { it.tag.isNotBlank() }?.tag ?: ""
    val count: Int = entries.size
    val lastDate: LocalDate = dateOf(latest.at)

    /** gaps[i] = days between entries[i] and the older entries[i + 1]. */
    val gaps: List<Long> = entries.zipWithNext { newer, older ->
        ChronoUnit.DAYS.between(dateOf(older.at), dateOf(newer.at))
    }

    private val realGaps = gaps.filter { it > 0 }

    /** How many days apart you usually do it, once there's at least one real gap. */
    val usualDays: Double? = realGaps.takeIf { it.isNotEmpty() }?.average()

    val nextDue: LocalDate? = usualDays?.let { lastDate.plusDays(it.roundToLong()) }

    /** Only guess "coming round again" after at least two real gaps. */
    val hasPattern: Boolean = realGaps.size >= 2
}

fun thingsOf(entries: List<Entry>): List<Thing> =
    entries.groupBy { keyOf(it.title) }
        .map { (k, list) -> Thing(k, list.sortedByDescending { it.at }) }
        .sortedByDescending { it.latest.at }

/** Things with a pattern, soonest due first. */
fun comingRound(things: List<Thing>, today: LocalDate): List<Pair<Thing, Long>> =
    things.filter { it.hasPattern }
        .map { it to ChronoUnit.DAYS.between(today, it.nextDue!!) }
        .sortedBy { it.second }
