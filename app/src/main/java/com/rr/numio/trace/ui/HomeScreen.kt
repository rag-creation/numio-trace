package com.rr.numio.trace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.trace.data.Entry
import com.rr.numio.trace.data.Thing
import com.rr.numio.trace.data.comingRound
import com.rr.numio.trace.data.dateOf
import com.rr.numio.trace.data.keyOf
import java.time.LocalDate

@Composable
fun HomeScreen(
    entries: List<Entry>,
    things: List<Thing>,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    onEdit: (Entry) -> Unit,
) {
    val p = LocalPalette.current
    var query by rememberSaveable { mutableStateOf("") }
    val today = LocalDate.now()

    val days = remember(entries) {
        entries.sortedByDescending { it.at }.groupBy { dateOf(it.at) }.toList()
    }
    val due = remember(things, today) { comingRound(things, today).take(2) }
    val q = keyOf(query)
    val hits = remember(things, q) {
        if (q.isEmpty()) emptyList() else things.filter { t ->
            t.key.contains(q) ||
                t.tag.lowercase().contains(q) ||
                t.entries.any { it.note.lowercase().contains(q) }
        }
    }

    Box(Modifier.fillMaxSize().background(p.bg).systemBarsPadding()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 112.dp)) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Trace", color = p.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.padding(top = 10.dp).size(8.dp).clip(CircleShape).background(p.accent))
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = p.muted)
                    }
                }
            }
            item {
                SearchField(query, { query = it }, Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }

            if (q.isNotEmpty()) {
                if (hits.isEmpty()) {
                    item {
                        Text(
                            "Nothing traced for “${query.trim()}” yet.",
                            color = p.muted, fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                } else {
                    items(hits, key = { "t" + it.key }) { t ->
                        ThingRow(t, today, onClick = { onOpen(t.key) })
                    }
                }
            } else {
                if (due.isNotEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            SectionLabel("Coming round again", Modifier.padding(start = 20.dp, top = 8.dp, bottom = 10.dp))
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                due.forEach { (t, daysUntil) ->
                                    DueCard(t, daysUntil, Modifier.weight(1f), onClick = { onOpen(t.key) })
                                }
                                if (due.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (entries.isEmpty()) {
                    item { EmptyHome() }
                }
                days.forEach { (date, list) ->
                    item(key = "d$date") {
                        SectionLabel(
                            Fmt.dayLabel(date, today),
                            Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
                        )
                    }
                    items(list, key = { it.id }) { e ->
                        EntryRow(e, onClick = { onOpen(keyOf(e.title)) }, onLongClick = { onEdit(e) })
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            containerColor = p.accent,
            contentColor = p.onAccent,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).size(64.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add a trace", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(
        modifier.fillMaxWidth().height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(p.surface)
            .border(1.dp, p.line, RoundedCornerShape(24.dp))
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = p.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("When did I last…", color = p.muted, fontSize = 15.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = p.text, fontSize = 15.sp),
                cursorBrush = SolidColor(p.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onChange("") }) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = p.muted)
            }
        }
    }
}

@Composable
private fun DueCard(t: Thing, daysUntil: Long, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(p.surface).border(1.dp, p.line, shape)
            .clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(t.title, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Every ${Fmt.usual(t.usualDays ?: 0.0)}", color = p.muted, fontSize = 13.sp)
        val soon = daysUntil <= 7
        Text(
            Fmt.due(daysUntil),
            color = if (soon) p.accentText else p.muted,
            fontSize = 13.sp,
            fontWeight = if (soon) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun EntryRow(e: Entry, onClick: () -> Unit, onLongClick: () -> Unit) {
    val p = LocalPalette.current
    val meta = listOf(e.tag, e.note).filter { it.isNotBlank() }.joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).heightIn(min = 56.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RingDot()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(e.title, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (meta.isNotEmpty()) {
                Text(meta, color = p.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(Fmt.time(e.at), color = p.muted, fontSize = 13.sp)
    }
}

@Composable
private fun ThingRow(t: Thing, today: LocalDate, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 64.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RingDot()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(
                "Last: ${Fmt.agoShort(t.lastDate, today)} · ${Fmt.times(t.count)}",
                color = p.muted, fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun EmptyHome() {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 48.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Nothing traced yet", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Did a small job today? Changed the AC filter, washed the bedsheets, gave something for repair? " +
                "Tap + and type it. Next time you'll know exactly when.",
            color = p.muted, fontSize = 15.sp, lineHeight = 22.sp,
        )
    }
}
