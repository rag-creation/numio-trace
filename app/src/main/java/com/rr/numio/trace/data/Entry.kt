package com.rr.numio.trace.data

import java.util.Locale

/** One time you did something. */
data class Entry(
    val id: String,
    val title: String,
    val at: Long,
    val note: String = "",
    val tag: String = "",
)

/** Entries with the same name (ignoring case and extra spaces) belong to the same thing. */
fun keyOf(title: String): String =
    title.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
