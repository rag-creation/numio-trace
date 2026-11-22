package com.rr.numio.trace.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * All traces live in one file in the app's private storage: files/trace.json.
 *
 * Saving never edits that file in place. The new list is written to a temp file,
 * flushed to disk, and only then swapped in, so a crash or a dead battery mid-save
 * leaves the previous version intact. The version before that is kept as trace.json.bak.
 * A file that can't be read is copied aside, never deleted.
 */
class TraceStore(context: Context) {

    private val dir = context.filesDir
    private val main = File(dir, "trace.json")
    private val tmp = File(dir, "trace.json.tmp")
    private val bak = File(dir, "trace.json.bak")

    var entries by mutableStateOf<List<Entry>>(emptyList())
        private set

    init {
        entries = load()
    }

    fun newId(): String = UUID.randomUUID().toString()

    /** Returns false if the change couldn't be saved; nothing changes in that case. */
    fun add(entry: Entry): Boolean = commit(entries + entry)

    fun delete(id: String): Boolean = commit(entries.filterNot { it.id == id })

    private fun commit(next: List<Entry>): Boolean {
        val ok = runCatching { save(next) }.isSuccess
        if (ok) entries = next
        return ok
    }

    private fun load(): List<Entry> {
        for (f in listOf(main, bak)) {
            if (!f.exists()) continue
            try {
                return parse(f.readText())
            } catch (e: Exception) {
                runCatching { f.copyTo(File(dir, "trace-unreadable-${System.currentTimeMillis()}.json")) }
            }
        }
        return emptyList()
    }

    private fun parse(text: String): List<Entry> {
        val arr = JSONObject(text).getJSONArray("entries")
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Entry(
                id = o.getString("id"),
                title = o.getString("title"),
                at = o.getLong("at"),
                note = o.optString("note", ""),
                tag = o.optString("tag", ""),
            )
        }
    }

    @Synchronized
    private fun save(list: List<Entry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("title", e.title)
                    .put("at", e.at)
                    .put("note", e.note)
                    .put("tag", e.tag),
            )
        }
        val json = JSONObject().put("version", 1).put("entries", arr).toString()
        FileOutputStream(tmp).use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
            out.flush()
            out.fd.sync()
        }
        if (main.exists()) main.copyTo(bak, overwrite = true)
        if (!tmp.renameTo(main)) throw IllegalStateException("Could not save")
    }
}
