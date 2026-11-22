package com.rr.numio.trace.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.trace.R

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Text(
        text.uppercase(),
        modifier = modifier,
        color = p.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

/** Hollow amber dot used on the timeline. */
@Composable
fun RingDot(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(modifier.size(10.dp).border(2.dp, p.accent, CircleShape))
}

/** The official "Numio." wordmark: cream letters on dark screens, dark letters on light ones. */
@Composable
fun NumioWordmark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(
            if (LocalPalette.current.dark) R.drawable.numio_wordmark_on_dark else R.drawable.numio_wordmark_on_light,
        ),
        contentDescription = "Numio",
        modifier = modifier,
    )
}
