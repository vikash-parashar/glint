package app.glint.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glint.launcher.HomeData
import app.glint.launcher.Overlays
import app.glint.launcher.data.AppEntry
import app.glint.theme.GlintPalette
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HomeActions(
    val launch: (AppEntry) -> Unit,
    val longPressApp: (AppEntry) -> Unit,
    val openDrawer: () -> Unit,
    val startFocus: () -> Unit,
    val askCalendar: () -> Unit,
    val askCity: () -> Unit,
    val openSettings: () -> Unit,
    /** User widgets (and the set-as-home banner), filling the free space on home. */
    val widgetArea: @Composable (Modifier) -> Unit = {},
)

private fun dateLine(now: Long, pattern: String) =
    Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern))

private fun eventLine(o: Overlays): Pair<String, String>? {
    val e = o.nextEvent ?: return null
    return (if (e.allDay) "Today" else hhmm(e.beginMillis)) to e.title
}

/* ---------------------------------- BENTO ---------------------------------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BentoHome(home: HomeData, o: Overlays, a: HomeActions) {
    val s = home.style
    val now = rememberNow(10_000)
    val gap = 8.dp
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)
            .combinedClickable(onClick = {}, onLongClick = a.openSettings),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        // Clock tile
        Tile(s, 0, Modifier.fillMaxWidth().height(128.dp)) {
            Label(dateLine(now, "EEEE d MMM").uppercase(), s)
            Text(clockText(now), color = s.onTile.c(), fontSize = 72.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp)
        }
        Row(Modifier.fillMaxWidth().height(124.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
            Tile(s, 1, Modifier.weight(1f).fillMaxHeight().clickable(onClick = a.askCity)) {
                val w = o.weather
                Label(w?.city ?: "Weather", s)
                Text(w?.let { "${it.tempC}°" } ?: "Set city", color = s.onTile.c(),
                    fontSize = if (w != null) 32.sp else 18.sp, fontWeight = FontWeight.Bold)
                Text(w?.let { "${it.condition}, ${it.highC}° high" } ?: "No location needed",
                    color = s.onTile.c(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Tile(s, 2, Modifier.weight(1f).fillMaxHeight()) {
                Label(if (home.battery?.charging == true) "Charging" else "Battery", s)
                Box(Modifier.align(Alignment.End).size(58.dp), contentAlignment = Alignment.Center) {
                    ProgressRing((home.battery?.percent ?: 0) / 100f, s.onTile.c(), s.onTile.c().copy(alpha = .14f), Modifier.matchParentSize(), 6.dp)
                    Text("${home.battery?.percent ?: "–"}", color = s.onTile.c(), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }
        }
        Tile(s, 3, Modifier.fillMaxWidth().height(64.dp).clickable { if (!o.calendarGranted) a.askCalendar() }) {
            val ev = eventLine(o)
            Label(ev?.let { "Next · ${it.first}" } ?: if (o.calendarGranted) "Calendar" else "Calendar · tap to connect", s)
            Text(ev?.second ?: if (o.calendarGranted) "Nothing in the next 24 h" else "Show your next event here",
                color = s.onTile.c(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
            Tile(s, 4, Modifier.weight(1f).fillMaxHeight().clickable(onClick = a.startFocus)) {
                Label("Focus", s)
                Text(if (home.stats.streakDays > 0) "${home.stats.streakDays}-day streak" else "Start a session",
                    color = s.onTile.c(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Tile(s, null, Modifier.weight(1f).fillMaxHeight().clickable(onClick = a.openDrawer)) {
                Text("ALL APPS", color = s.onBackgroundDim.c(), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text("${home.apps.size} apps", color = s.onBackground.c(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        a.widgetArea(Modifier.weight(1f))
        Dock(home, a)
    }
}

@Composable
private fun Tile(s: GlintPalette, index: Int?, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val color = index?.let { s.tiles.getOrNull(it) }
    Column(
        modifier.surface(s, color).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween, content = content,
    )
}

@Composable
private fun Label(text: String, s: GlintPalette) =
    Text(text.uppercase(), color = s.onTile.c().copy(alpha = .65f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

@Composable
fun Dock(home: HomeData, a: HomeActions) {
    val s = home.style
    Row(
        Modifier.fillMaxWidth().surface(s).padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        home.favorites.take(5).forEach { AppIcon(it, s, 46.dp, showLabel = false, onClick = { a.launch(it) }, onLongClick = { a.longPressApp(it) }) }
    }
}

/* ----------------------------------- INK ----------------------------------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InkHome(home: HomeData, o: Overlays, a: HomeActions) {
    val s = home.style
    val now = rememberNow(10_000)
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)
            .combinedClickable(onClick = {}, onLongClick = a.openSettings),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(dateLine(now, "EEEE, d MMMM"), color = s.onBackgroundDim.c(), fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp))
        Text(clockText(now), color = s.onBackground.c(), fontFamily = FontFamily.Serif, fontWeight = FontWeight.Light, fontSize = 92.sp, letterSpacing = (-3).sp)
        val w = o.weather
        InkLine(w?.let { "${it.tempC}° ${it.condition.lowercase()} in ${it.city}" } ?: "Set your city for weather", s, onClick = a.askCity)
        val ev = eventLine(o)
        if (ev != null) InkLine("${ev.first}  ${ev.second}", s, accentPrefix = ev.first) {}
        else if (!o.calendarGranted) InkLine("Show my next event", s, onClick = a.askCalendar)
        home.battery?.let { InkLine("${it.percent}% battery${if (it.charging) ", charging" else ""}", s) {} }

        a.widgetArea(Modifier.weight(1f))
        home.favorites.take(6).forEach { app ->
            Text(
                app.label, color = s.onBackground.c(), fontSize = 28.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.combinedClickable(onClick = { a.launch(app) }, onLongClick = { a.longPressApp(app) }).padding(vertical = 2.dp),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("All apps", color = s.onBackgroundDim.c(), fontSize = 13.sp, modifier = Modifier.clickable(onClick = a.openDrawer).padding(vertical = 8.dp))
            Row(Modifier.clickable(onClick = a.startFocus).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(s.accent.c()))
                Text("Focus", color = s.onBackground.c(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InkLine(text: String, s: GlintPalette, accentPrefix: String? = null, onClick: () -> Unit) {
    val mod = Modifier.clickable(onClick = onClick)
    if (accentPrefix != null && text.startsWith(accentPrefix)) {
        Row(mod, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(accentPrefix, color = s.accent.c(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text.removePrefix(accentPrefix).trim(), color = s.onBackground.c().copy(alpha = .8f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else Text(text, color = s.onBackground.c().copy(alpha = .8f), fontSize = 14.sp, modifier = mod)
}

/* ---------------------------------- LIQUID --------------------------------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiquidHome(home: HomeData, o: Overlays, a: HomeActions) {
    val s = home.style
    val now = rememberNow(10_000)
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)
            .combinedClickable(onClick = {}, onLongClick = a.openSettings),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.fillMaxWidth().surface(s).clickable(onClick = a.askCity).padding(18.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(dateLine(now, "EEEE, d MMM"), color = s.onBackgroundDim.c(), fontSize = 12.sp)
            Text(clockText(now), color = s.onBackground.c(), fontSize = 62.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-2).sp)
            Text(o.weather?.let { "${it.tempC}° ${it.condition.lowercase()} · ${it.city}" } ?: "Tap to set your city", color = s.onBackgroundDim.c(), fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val ev = eventLine(o)
            Column(Modifier.weight(1f).fillMaxHeight().surface(s).clickable { if (!o.calendarGranted) a.askCalendar() }.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text("NEXT", color = s.onBackgroundDim.c(), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text(ev?.let { "${it.first} ${it.second}" } ?: if (o.calendarGranted) "Free day" else "Connect calendar",
                    color = s.onBackground.c(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(Modifier.weight(1f).fillMaxHeight().surface(s).padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text("BATTERY", color = s.onBackgroundDim.c(), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text("${home.battery?.percent ?: "–"}%", color = s.onBackground.c(), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(
            Modifier.fillMaxWidth().surface(s.copy(cornerDp = 999f)).clickable(onClick = a.startFocus).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Start focus", color = s.onBackground.c(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(if (home.stats.streakDays > 0) "${home.stats.streakDays}-day streak" else "25 · 50 · 90", color = s.onBackgroundDim.c(), fontSize = 13.sp)
        }
        a.widgetArea(Modifier.weight(1f))
        Text("All apps", color = s.onBackground.c(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally).clickable(onClick = a.openDrawer).padding(8.dp))
        Dock(home, a)
    }
}
