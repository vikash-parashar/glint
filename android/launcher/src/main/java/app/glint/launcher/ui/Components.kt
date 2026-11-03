package app.glint.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glint.launcher.data.AppEntry
import app.glint.theme.GlintPalette
import app.glint.theme.StyleKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun Int.c() = Color(this)

/** Page background for a style: solid for Bento/Ink, colour mesh for Liquid (muted during focus). */
@Composable
fun StyleBackground(style: GlintPalette, muted: Boolean, content: @Composable BoxScope.() -> Unit) {
    val mod = if (style.kind == StyleKind.Liquid) {
        val stops = style.wallpaper.map { if (muted) desaturate(it.c()) else it.c() }
        Modifier.background(stops.last()).background(
            Brush.radialGradient(listOf(stops[0], Color.Transparent), center = Offset(80f, 120f), radius = 900f),
        ).background(
            Brush.radialGradient(listOf(stops[1], Color.Transparent), center = Offset(1100f, 700f), radius = 900f),
        ).background(
            Brush.radialGradient(listOf(stops[2], Color.Transparent), center = Offset(300f, 2200f), radius = 1200f),
        )
    } else Modifier.background(style.background.c())
    Box(Modifier.fillMaxSize().then(mod), content = content)
}

private fun desaturate(c: Color): Color {
    val l = 0.3f * c.red + 0.59f * c.green + 0.11f * c.blue
    return Color(red = (c.red + l) / 2, green = (c.green + l) / 2, blue = (c.blue + l) / 2)
}

/** The one surface treatment per style: solid tile, plain text, or frosted glass. */
fun Modifier.surface(style: GlintPalette, tile: Int? = null): Modifier = when (style.kind) {
    StyleKind.Bento -> clip(RoundedCornerShape(style.cornerDp.dp)).background((tile ?: style.surface).c())
    StyleKind.Ink -> this
    StyleKind.Liquid -> clip(RoundedCornerShape(style.cornerDp.dp))
        .background(Color.White.copy(alpha = style.glassOpacity))
        .border(1.dp, Color.White.copy(alpha = .7f), RoundedCornerShape(style.cornerDp.dp))
}

fun GlintPalette.textOn(tile: Boolean) = if (kind == StyleKind.Bento && tile) onTile.c() else onBackground.c()

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppEntry, style: GlintPalette, size: Dp = 52.dp, showLabel: Boolean = true,
    dimmed: Boolean = false, onClick: () -> Unit, onLongClick: (() -> Unit)? = null,
) {
    val bmp = remember(app.packageName) { app.icon.asImageBitmap() }
    Column(
        Modifier.width(size + 22.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(bmp, app.label, Modifier.size(size), alpha = if (dimmed) .35f else 1f)
        if (showLabel) Text(
            app.label, color = style.onBackground.c(), fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Ticks once a second while visible. */
@Composable
fun rememberNow(periodMs: Long = 1000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMs) { while (true) { now = System.currentTimeMillis(); delay(periodMs) } }
    return now
}

fun clockText(now: Long): String =
    java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault())
        .toLocalTime().format(DateTimeFormatter.ofPattern("h:mm"))

fun hhmm(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
        .toLocalTime().format(DateTimeFormatter.ofPattern("H:mm"))

fun mmss(ms: Long): String {
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun ProgressRing(progress: Float, color: Color, track: Color, modifier: Modifier = Modifier, stroke: Dp = 6.dp) {
    Canvas(modifier) {
        val w = stroke.toPx()
        drawArc(track, 0f, 360f, false, style = Stroke(w))
        drawArc(color, -90f, 360f * progress, false, style = Stroke(w, cap = StrokeCap.Round))
    }
}

/**
 * Press and hold for [holdMs] to confirm. Used to end focus early and to open a
 * blocked app: the pause is the feature, it breaks the reflex.
 */
@Composable
fun HoldToConfirm(
    label: String, holdingLabel: String, fg: Color, track: Color,
    modifier: Modifier = Modifier, holdMs: Int = 3000, onConfirmed: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, track, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    val job = scope.launch {
                        progress.animateTo(1f, tween(holdMs, easing = LinearEasing))
                        onConfirmed()
                        progress.snapTo(0f)
                    }
                    waitForUpOrCancellation()
                    if (progress.value < 1f) { job.cancel(); scope.launch { progress.animateTo(0f, tween(200)) } }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.matchParentSize()) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress.value).background(track))
        }
        Text(
            if (progress.value > 0f) holdingLabel else label, color = fg,
            fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(vertical = 14.dp),
        )
    }
}

fun greetingFor(time: LocalTime = LocalTime.now()) = when (time.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
