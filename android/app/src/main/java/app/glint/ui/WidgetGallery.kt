package app.glint.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glint.theme.GlintPalette
import app.glint.theme.StyleKind
import app.glint.widgets.catalog.Needs
import app.glint.widgets.catalog.WidgetCatalog
import app.glint.widgets.catalog.WidgetDesign
import app.glint.widgets.catalog.WidgetKind
import app.glint.widgets.engine.WidgetPinning
import kotlinx.coroutines.launch

/** All 51 designs grouped by type. "Add" asks the launcher to place it with that exact design. */
@Composable
fun WidgetGallery(style: GlintPalette, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val pinSupported = remember { WidgetPinning.supported(ctx) }
    val groups = remember { WidgetCatalog.all.groupBy { it.kind } }

    Scaffold(containerColor = Color(0xFF0F1013), snackbarHost = { SnackbarHost(snack) }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(vertical = 16.dp)) {
            item {
                Header("Widgets", "${WidgetCatalog.all.size} designs in ${groups.size} types. They follow your style: ${style.name}.", onBack)
                if (!pinSupported) Text(
                    "Your launcher can't add widgets from apps. Long-press your home screen, tap Widgets and look for Glint.",
                    color = Color(0xFFFFB59E), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            groups.forEach { (kind, designs) ->
                item(key = kind.name) {
                    Text(kind.title, color = Color(0xFFF3F1EE), fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                        modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(designs, key = { it.id }) { d ->
                            DesignCard(d, style, enabled = pinSupported) {
                                scope.launch {
                                    val ok = WidgetPinning.pin(ctx, d)
                                    snack.showSnackbar(if (ok) "Confirm on your home screen to place it" else "Couldn't add. Try from your launcher's widget list.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesignCard(d: WidgetDesign, style: GlintPalette, enabled: Boolean, onAdd: () -> Unit) {
    val tileIndex = d.kind.ordinal % style.tiles.size
    val (bg, fg) = when (style.kind) {
        StyleKind.Bento -> Color(style.tiles[tileIndex]) to Color(style.onTile)
        StyleKind.Ink -> Color.White to Color(0xFF15171C)
        StyleKind.Liquid -> Color.White.copy(alpha = .16f) to Color.White
    }
    Column(Modifier.width(168.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF17181C)).padding(10.dp)) {
        Box(
            Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(style.cornerDp.dp))
                .background(if (style.kind == StyleKind.Liquid) Color(style.wallpaper.first()) else Color(0xFF0F1013)),
        ) {
            Box(Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(style.cornerDp.dp)).background(bg).padding(12.dp)) {
                Text(sample(d), color = fg, fontSize = sampleSize(d).sp, fontWeight = FontWeight.Bold, lineHeight = (sampleSize(d) + 4).sp,
                    fontFamily = if (d.id.contains("serif") || (style.serifClock && d.kind == WidgetKind.ClockDigital)) FontFamily.Serif
                    else if (d.id.contains("mono")) FontFamily.Monospace else FontFamily.SansSerif,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(d.name, color = Color(0xFFF3F1EE), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
            if (d.pro) Text("PRO", color = Color(0xFF1A1110), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(Color(0xFFFFB59E), RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 1.dp))
        }
        Text(d.blurb + needsNote(d.needs), color = Color(0xFFA7A6AC), fontSize = 12.sp, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
        FilledTonalButton(onClick = onAdd, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { Text("Add") }
    }
}

private fun needsNote(n: Needs) = when (n) {
    Needs.Calendar -> " · asks for calendar"
    Needs.Steps -> " · asks for activity"
    Needs.MediaAccess -> " · needs media access"
    else -> ""
}

private fun sampleSize(d: WidgetDesign) = when (d.kind) {
    WidgetKind.ClockDigital, WidgetKind.ClockAnalog, WidgetKind.Battery, WidgetKind.Steps, WidgetKind.Weather -> 26
    WidgetKind.Date -> 22
    else -> 14
}

/** A short line of sample content so each card reads at a glance. */
private fun sample(d: WidgetDesign): String = when (d.id) {
    "clock_big" -> "10:42\nTue, 3 Nov"
    "clock_stacked" -> "10\n42"
    "clock_mono" -> "22:42"
    "clock_serif" -> "10:42"
    "clock_minimal" -> "10:42"
    "clock_greeting" -> "Good morning\n10:42"
    "clock_pill" -> "10:42 · Tue 3"
    "analog_classic", "analog_numerals", "analog_dots", "analog_minimal" -> "◷"
    "world_dual" -> "Jaipur 10:42\nLondon 05:12"
    "world_trio" -> "Jaipur · London\nNew York"
    "date_big" -> "3\nTuesday"
    "date_month" -> "NOV\n1 2 [3] 4 5"
    "date_week" -> "M T [W] T F"
    "date_page" -> "NOV\n3"
    "agenda_next" -> "11:30\nDesign review"
    "agenda_list" -> "11:30 Review\n14:00 Gym\n19:00 Dinner"
    "weather_now" -> "24° ☀"
    "weather_big" -> "24°"
    "weather_range" -> "H 29° L 18°"
    "weather_pill" -> "☀ 24° Jaipur"
    "weather_days" -> "Wed ☀ 29°\nThu ⛅ 27°"
    "battery_percent" -> "86%"
    "battery_ring" -> "◔ 86"
    "battery_bar" -> "▰▰▰▰▱ 86%"
    "battery_pill" -> "⚡ 86%"
    "steps_count" -> "6,240"
    "steps_ring" -> "◕ 78%"
    "steps_bar" -> "6,240 steps"
    "music_player" -> "Song title\nArtist  ⏮ ⏯ ⏭"
    "music_compact" -> "Song title  ⏯"
    "music_controls" -> "⏮   ⏯   ⏭"
    "note_sticky" -> "Buy milk, call Riya"
    "checklist" -> "☑ Milk\n☐ Eggs\n☐ Bread"
    "quote_serif" -> "“Well begun is half done.”"
    "quote_bold" -> "STAY\nCURIOUS"
    "countdown_days" -> "42 days\nGoa trip"
    "progress_year" -> "2026 · 84%"
    "progress_day" -> "Today · 44%"
    "apps_row" -> "◉ ◉ ◉ ◉"
    "apps_grid" -> "◉ ◉ ◉ ◉\n◉ ◉ ◉ ◉"
    "search_pill" -> "⌕  Search"
    "photo_frame" -> "▣ Your photo"
    "photo_date" -> "▣ 3 Nov"
    "focus_start" -> "◎ Focus\n25 min"
    "focus_streak" -> "5 day streak"
    "alarm_next" -> "⏰ 6:30\nin 7 h"
    "moon_phase" -> "◐ Waxing"
    "storage" -> "64 GB free"
    else -> d.name
}

@Composable
fun Header(title: String, subtitle: String, onBack: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("‹ Back", color = Color(0xFFA7A6AC)) }
        Text(title, color = Color(0xFFF3F1EE), fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color(0xFFA7A6AC), fontSize = 14.sp)
    }
}
