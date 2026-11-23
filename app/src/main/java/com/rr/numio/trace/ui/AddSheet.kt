package com.rr.numio.trace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.trace.data.Entry
import com.rr.numio.trace.data.Thing
import com.rr.numio.trace.data.keyOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** When the thing happened: now, yesterday, or a picked day. */
private sealed interface When {
    data object Now : When
    data object Yesterday : When
    data class Day(val date: LocalDate) : When
    /** Editing: keep the exact time it was saved with. */
    data class Original(val at: Long) : When
}

private fun When.toMillis(): Long {
    val now = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    return when (this) {
        When.Now -> now
        When.Yesterday -> now - 24L * 60 * 60 * 1000
        is When.Day -> {
            if (date == LocalDate.now()) now
            else date.atTime(LocalTime.now()).atZone(zone).toInstant().toEpochMilli()
        }
        is When.Original -> at
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(
    things: List<Thing>,
    onSave: (title: String, at: Long, note: String, tag: String) -> Unit,
    onDismiss: () -> Unit,
    /** When set, the sheet edits this trace instead of adding a new one. */
    editing: Entry? = null,
    onDelete: (() -> Unit)? = null,
) {
    val p = LocalPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable { mutableStateOf(editing?.title ?: "") }
    var note by rememberSaveable { mutableStateOf(editing?.note ?: "") }
    var tag by rememberSaveable { mutableStateOf(editing?.tag ?: "") }
    var showNote by rememberSaveable { mutableStateOf(!editing?.note.isNullOrBlank()) }
    var showTag by rememberSaveable { mutableStateOf(!editing?.tag.isNullOrBlank()) }
    var whenPick by remember { mutableStateOf<When>(editing?.let { When.Original(it.at) } ?: When.Now) }
    var pickingDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val today = LocalDate.now()

    val typed = keyOf(title)
    val suggestions = remember(things, typed) {
        if (typed.isEmpty()) things.take(3)
        else things.filter { it.key.contains(typed) && it.key != typed }.take(3)
    }
    val exact = things.firstOrNull { it.key == typed && typed.isNotEmpty() }

    LaunchedEffect(Unit) { if (editing == null) runCatching { focus.requestFocus() } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = p.surface,
        contentColor = p.text,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(if (editing != null) "Edit trace" else "What did you do?")
                Box(Modifier.fillMaxWidth()) {
                    if (title.isEmpty()) {
                        Text("e.g. Changed AC filter", color = p.muted.copy(alpha = 0.35f), fontSize = 26.sp, fontWeight = FontWeight.Medium)
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it.take(120) },
                        singleLine = true,
                        textStyle = TextStyle(color = p.text, fontSize = 26.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(p.accent),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(p.accent))
                if (exact != null && editing == null) {
                    Text(
                        "Last time: ${Fmt.agoShort(exact.lastDate, today)} · done ${Fmt.times(exact.count)}",
                        color = p.accentText, fontSize = 13.sp,
                    )
                }
            }

            if (suggestions.isNotEmpty() && editing == null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (typed.isEmpty()) "Recent" else "You've done this before",
                        color = p.muted, fontSize = 12.sp,
                    )
                    suggestions.forEach { t ->
                        SuggestionRow(t, today) {
                            title = t.title
                            if (tag.isBlank() && t.tag.isNotBlank()) tag = t.tag
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("When")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill("Now", whenPick == When.Now) { whenPick = When.Now }
                    Pill("Yesterday", whenPick == When.Yesterday) { whenPick = When.Yesterday }
                    val pickedDate = when (val w = whenPick) {
                        is When.Day -> w.date
                        is When.Original -> com.rr.numio.trace.data.dateOf(w.at)
                        else -> null
                    }
                    Pill(
                        pickedDate?.let { Fmt.shortDate(it, today) } ?: "Pick a day",
                        pickedDate != null,
                        icon = true,
                    ) { pickingDate = true }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!showNote) GhostButton("+ Note", Modifier.weight(1f)) { showNote = true }
                if (!showTag) GhostButton("+ Tag", Modifier.weight(1f)) { showTag = true }
            }
            if (showNote) {
                Field(note, { note = it.take(500) }, "Note (optional)", singleLine = false)
            }
            if (showTag) {
                Field(tag, { tag = it.take(40) }, "Tag, e.g. Home, Car, Bike")
            }

            Button(
                onClick = { onSave(title.trim().replace(Regex("\\s+"), " "), whenPick.toMillis(), note.trim(), tag.trim()) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = p.accent, contentColor = p.onAccent,
                    disabledContainerColor = p.line, disabledContentColor = p.muted,
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(if (editing != null) "Save changes" else "Save", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }

            if (editing != null && onDelete != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = p.muted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete this trace", color = p.muted, fontSize = 15.sp)
                }
            }
        }
    }

    if (confirmDelete && editing != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this trace?") },
            text = { Text("\u201C${editing.title}\u201D on ${Fmt.fullDate(editing.at)} at ${Fmt.time(editing.at)} will be deleted.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete", color = p.accentText) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = p.muted) }
            },
            containerColor = p.surface,
            titleContentColor = p.text,
            textContentColor = p.muted,
        )
    }

    if (pickingDate) {
        val todayUtc = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val initialDate = when (val w = whenPick) {
            is When.Day -> w.date
            is When.Original -> com.rr.numio.trace.data.dateOf(w.at)
            When.Yesterday -> today.minusDays(1)
            When.Now -> today
        }
        val initial = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        val orig = whenPick as? When.Original
                        whenPick = when {
                            orig != null && com.rr.numio.trace.data.dateOf(orig.at) == d -> orig
                            d == today -> When.Now
                            else -> When.Day(d)
                        }
                    }
                    pickingDate = false
                }) { Text("OK", color = p.accentText) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) { Text("Cancel", color = p.muted) }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun SuggestionRow(t: Thing, today: LocalDate, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(shape)
            .border(1.dp, p.line, shape).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(t.title, color = p.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Text("Last: ${Fmt.agoShort(t.lastDate, today)}", color = p.muted, fontSize = 13.sp)
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, icon: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.height(44.dp).clip(shape)
            .background(if (selected) p.accent else p.surface)
            .border(1.dp, if (selected) p.accent else p.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon) {
            Icon(Icons.Filled.DateRange, contentDescription = null, tint = if (selected) p.onAccent else p.muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            label,
            color = if (selected) p.onAccent else p.text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun GhostButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.height(48.dp).clip(shape).border(1.dp, p.line, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = p.muted, fontSize = 15.sp)
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = true) {
    val p = LocalPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = p.accent, unfocusedBorderColor = p.line,
            focusedLabelColor = p.accentText, unfocusedLabelColor = p.muted,
            cursorColor = p.accent, focusedTextColor = p.text, unfocusedTextColor = p.text,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
