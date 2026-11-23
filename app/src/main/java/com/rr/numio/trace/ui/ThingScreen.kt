package com.rr.numio.trace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.trace.data.Entry
import com.rr.numio.trace.data.Thing
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ThingScreen(
    thing: Thing,
    onBack: () -> Unit,
    /** Logs it again right now; returns the new entry's id, or null if saving failed. */
    onAgain: () -> String?,
    onDelete: (String) -> Boolean,
    onEdit: (Entry) -> Unit,
) {
    val p = LocalPalette.current
    val today = LocalDate.now()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = p.bg,
        contentColor = p.text,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().background(p.bg).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Button(
                    onClick = {
                        val id = onAgain()
                        scope.launch {
                            snack.currentSnackbarData?.dismiss()
                            if (id == null) {
                                snack.showSnackbar("Couldn't save. Please try again.")
                            } else {
                                val r = snack.showSnackbar("Logged for today", actionLabel = "Undo", duration = SnackbarDuration.Short)
                                if (r == SnackbarResult.ActionPerformed) onDelete(id)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Did it again", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) { inner ->
        LazyColumn(Modifier.padding(inner)) {
            item {
                IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = p.text)
                }
            }
            item {
                Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (thing.tag.isNotBlank()) Text(thing.tag, color = p.muted, fontSize = 13.sp)
                    Text(thing.title, color = p.text, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
                }
            }
            item {
                val shape = RoundedCornerShape(20.dp)
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)
                        .clip(shape).background(p.surface).border(1.dp, p.line, shape).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Row {
                        Stat("Last time", Fmt.ago(thing.lastDate, today), Modifier.weight(1f))
                        Stat("Usually", thing.usualDays?.let { Fmt.usual(it) } ?: "—", Modifier.weight(1f))
                    }
                    Row {
                        Stat(
                            "Next, roughly",
                            thing.nextDue?.let { Fmt.shortDate(it, today) } ?: "—",
                            Modifier.weight(1f),
                            highlight = thing.nextDue != null,
                        )
                        Stat("Times", thing.count.toString(), Modifier.weight(1f))
                    }
                    if (thing.usualDays == null) {
                        Text(
                            "Log it again next time and Trace will show how often you do it.",
                            color = p.muted, fontSize = 13.sp, lineHeight = 18.sp,
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("Every time", Modifier.weight(1f))
                    Text("Tap one to edit", color = p.muted, fontSize = 12.sp)
                }
            }
            itemsIndexed(thing.entries, key = { _, e -> e.id }) { i, e ->
                HistoryRow(
                    e = e,
                    first = i == 0,
                    gap = thing.gaps.getOrNull(i),
                    onClick = { onEdit(e) },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, highlight: Boolean = false) {
    val p = LocalPalette.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(label)
        Text(
            value,
            color = if (highlight) p.accentText else p.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HistoryRow(e: Entry, first: Boolean, gap: Long?, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
    ) {
        Column(Modifier.width(14.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.padding(top = 5.dp).size(12.dp).clip(CircleShape)
                    .background(if (first) p.accent else p.bg)
                    .border(2.dp, p.accent, CircleShape),
            )
            if (gap != null) {
                Box(Modifier.padding(vertical = 4.dp).width(2.dp).weight(1f).background(p.line))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).padding(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${Fmt.fullDate(e.at)} · ${Fmt.time(e.at)}", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (e.note.isNotBlank()) Text(e.note, color = p.muted, fontSize = 13.sp)
            if (gap != null) {
                val shape = RoundedCornerShape(10.dp)
                Text(
                    Fmt.gap(gap),
                    color = p.muted, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp).border(1.dp, p.line, shape)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
    }
}
