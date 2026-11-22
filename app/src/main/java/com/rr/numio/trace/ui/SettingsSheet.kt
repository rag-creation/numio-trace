package com.rr.numio.trace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.trace.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(themeMode: ThemeMode, onTheme: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val version = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = p.surface,
        contentColor = p.text,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionLabel("Theme")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { m ->
                    val selected = m == themeMode
                    val shape = RoundedCornerShape(22.dp)
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(shape)
                            .background(if (selected) p.accent else p.surface)
                            .border(1.dp, if (selected) p.accent else p.line, shape)
                            .clickable { onTheme(m) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            m.label,
                            color = if (selected) p.onAccent else p.text,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }

            Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(1.dp).background(p.line))

            NumioWordmark(Modifier.padding(top = 6.dp).height(32.dp))
            Text("Trace $version", color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Remember when you last did the little things.\n" +
                    "No internet permission. Everything stays on this phone.",
                color = p.muted, fontSize = 14.sp, lineHeight = 20.sp,
            )
            Text("Made by RR 💛", color = p.muted, fontSize = 14.sp)
        }
    }
}
