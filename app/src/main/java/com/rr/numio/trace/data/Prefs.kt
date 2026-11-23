package com.rr.numio.trace.data

import android.content.Context

enum class ThemeMode(val label: String) { System("Follow system"), Light("Light"), Dark("Dark") }

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var theme: ThemeMode
        get() = runCatching { ThemeMode.valueOf(sp.getString("theme", null) ?: "System") }
            .getOrDefault(ThemeMode.System)
        set(value) = sp.edit().putString("theme", value.name).apply()
}
