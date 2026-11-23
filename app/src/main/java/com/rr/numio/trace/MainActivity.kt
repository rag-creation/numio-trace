package com.rr.numio.trace

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.rr.numio.trace.data.Entry
import com.rr.numio.trace.data.Prefs
import com.rr.numio.trace.data.ThemeMode
import com.rr.numio.trace.data.TraceStore
import com.rr.numio.trace.data.thingsOf
import com.rr.numio.trace.ui.AddSheet
import com.rr.numio.trace.ui.HomeScreen
import com.rr.numio.trace.ui.SettingsSheet
import com.rr.numio.trace.ui.ThingScreen
import com.rr.numio.trace.ui.TraceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = TraceStore(applicationContext)
        val prefs = Prefs(applicationContext)
        setContent { TraceApp(store, prefs) }
    }
}

@Composable
private fun TraceApp(store: TraceStore, prefs: Prefs) {
    var themeMode by remember { mutableStateOf(prefs.theme) }
    val dark = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val ctx = LocalContext.current
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }

    val entries = store.entries
    val things = remember(entries) { thingsOf(entries) }

    fun saveFailed() = Toast.makeText(ctx, "Couldn't save. Please try again.", Toast.LENGTH_SHORT).show()

    TraceTheme(dark) {
        val key = openKey
        val thing = key?.let { k -> things.firstOrNull { it.key == k } }
        if (thing != null) {
            BackHandler { openKey = null }
            ThingScreen(
                thing = thing,
                onBack = { openKey = null },
                onAgain = {
                    val e = Entry(store.newId(), thing.title, System.currentTimeMillis(), tag = thing.tag)
                    if (store.add(e)) e.id else null
                },
                onDelete = { id -> store.delete(id) },
                onEdit = { editId = it.id },
            )
        } else {
            if (key != null) LaunchedEffect(key) { openKey = null }
            HomeScreen(
                entries = entries,
                things = things,
                onOpen = { openKey = it },
                onAdd = { showAdd = true },
                onSettings = { showSettings = true },
                onEdit = { editId = it.id },
            )
        }

        val editing = editId?.let { id -> entries.firstOrNull { it.id == id } }
        if (editId != null && editing == null) LaunchedEffect(editId) { editId = null }
        if (editing != null) {
            AddSheet(
                things = things,
                editing = editing,
                onSave = { title, at, note, tag ->
                    if (store.update(editing.copy(title = title, at = at, note = note, tag = tag))) editId = null else saveFailed()
                },
                onDelete = {
                    if (store.delete(editing.id)) editId = null else saveFailed()
                },
                onDismiss = { editId = null },
            )
        } else if (showAdd) {
            AddSheet(
                things = things,
                onSave = { title, at, note, tag ->
                    if (store.add(Entry(store.newId(), title, at, note, tag))) showAdd = false else saveFailed()
                },
                onDismiss = { showAdd = false },
            )
        }
        if (showSettings) {
            SettingsSheet(
                themeMode = themeMode,
                onTheme = { themeMode = it; prefs.theme = it },
                onDismiss = { showSettings = false },
            )
        }
    }
}
