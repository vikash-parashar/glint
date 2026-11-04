package app.glint.dock

import android.app.NotificationManager
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glint.data.AgendaRepository
import app.glint.data.BatteryState
import app.glint.data.CityStore
import app.glint.data.DeviceInfo
import app.glint.data.NextEvent
import app.glint.data.NowPlaying
import app.glint.data.Track
import app.glint.data.Weather
import app.glint.data.WeatherCodes
import app.glint.data.WeatherRepository
import app.glint.data.batteryFlow
import app.glint.theme.GlintStyles
import app.glint.theme.StyleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class DockState(
    val now: Long = System.currentTimeMillis(),
    val battery: BatteryState? = null,
    val nextAlarm: Long? = null,
    val events: List<NextEvent> = emptyList(),
    val busyDays: Set<Int> = emptySet(),
    val weather: Weather? = null,
    val track: Track? = null,
    val alarmsOnly: Boolean = false,
)

/** Collects everything the dock shows. Clock ticks every second, the rest every few minutes. */
@Composable
fun rememberDockState(context: Context): DockState {
    var state by remember { mutableStateOf(DockState()) }
    val battery by remember { batteryFlow(context) }.collectAsState(initial = null)
    LaunchedEffect(Unit) {
        while (isActive) {
            state = state.copy(now = System.currentTimeMillis())
            delay(1_000 - System.currentTimeMillis() % 1_000)
        }
    }
    LaunchedEffect(Unit) {
        val agenda = AgendaRepository(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        var loops = 0
        while (isActive) {
            state = state.copy(
                nextAlarm = DeviceInfo.nextAlarmMillis(context),
                events = agenda.upcoming(limit = 4, hours = 36),
                busyDays = agenda.busyDaysThisMonth(),
                track = NowPlaying.current(context),
                alarmsOnly = nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS,
            )
            if (loops++ % 6 == 0) CityStore(context).get()?.let { c -> WeatherRepository.forCity(c)?.let { state = state.copy(weather = it) } }
            delay(5 * 60_000L)
        }
    }
    return state.copy(battery = battery)
}

private class DockInk(val bg: Color, val fg: Color, val dim: Color, val accent: Color, val card: Color)

/**
 * StandBy-style bedside screen: three pages you swipe between.
 * Split (clock + month), Big clock, and Agenda (alarm, events, weather, music).
 * At night everything turns dim red so it doesn't light up the room.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockScreen(state: DockState, settings: DockSettings, onTap: () -> Unit = {}, locked: Boolean = false) {
    val context = LocalContext.current
    val style by remember { StyleRepository(context).active }.collectAsState(initial = GlintStyles.of(app.glint.theme.StyleKind.Bento))
    val hour = Instant.ofEpochMilli(state.now).atZone(ZoneId.systemDefault()).hour
    val night = settings.isNight(hour)
    val ink = if (night) DockInk(Color.Black, Color(0xFFB3261E), Color(0xFF6E1A15), Color(0xFFB3261E), Color(0xFF140504))
    else DockInk(Color.Black, Color(0xFFF4F1EC), Color(0xFF8E8C93), Color(style.tiles.first()), Color(0xFF16171B))

    // Pixel shift: move everything a few dp each minute so an OLED screen doesn't burn in.
    val minute = (state.now / 60_000).toInt()
    val shift = IntOffset(((minute % 5) - 2) * 3, (((minute / 5) % 5) - 2) * 3)

    val pager = rememberPagerState(initialPage = settings.clockFace.ordinal) { DockFace.entries.size }
    LaunchedEffect(pager.currentPage) { settings.clockFace = DockFace.entries[pager.currentPage] }

    BoxWithConstraints(Modifier.fillMaxSize().background(ink.bg)) {
        if (locked || maxHeight > maxWidth) {
            // Upright (screen saver on a phone that isn't on its side): a calm stacked clock.
            Column(Modifier.fillMaxSize().offset { shift }, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                val d = Instant.ofEpochMilli(state.now).atZone(ZoneId.systemDefault())
                Text(d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), color = ink.accent, fontSize = 18.sp)
                Clock(state.now, ink, 96.sp)
                AlarmLine(state, ink, center = true)
                StatusBar(state, ink, Modifier.padding(top = 24.dp))
                if (locked) Text("Full Dock mode is part of Glint Pro", color = ink.dim, fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp))
            }
        } else {
        val scope = rememberCoroutineScope()
        // Swipe between pages, or tap to go to the next one.
        HorizontalPager(pager, Modifier.fillMaxSize().offset { shift }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                scope.launch { pager.animateScrollToPage((pager.currentPage + 1) % DockFace.entries.size) }
            }) { page ->
            Box(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp)) {
                when (DockFace.entries[page]) {
                    DockFace.Split -> SplitPage(state, ink, night)
                    DockFace.BigClock -> BigClockPage(state, ink)
                    DockFace.Agenda -> AgendaPage(state, ink, night)
                }
            }
        }
        StatusBar(state, ink, Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 14.dp).offset { shift })
        PageDots(pager.currentPage, ink, Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp))
        }
    }
}

@Composable
private fun Clock(now: Long, ink: DockInk, size: TextUnit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val is24 = DateFormat.is24HourFormat(ctx)
    val t = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    val hm = t.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm"))
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(hm, color = ink.fg, fontSize = size, fontWeight = FontWeight.Light, letterSpacing = (-2).sp,
            fontFamily = FontFamily.SansSerif, maxLines = 1, softWrap = false)
        if (!is24) Text(t.format(DateTimeFormatter.ofPattern("a")), color = ink.dim, fontSize = size / 5,
            modifier = Modifier.padding(start = 6.dp, bottom = (size.value / 6).dp))
    }
}

@Composable
private fun SplitPage(s: DockState, ink: DockInk, night: Boolean) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1.15f)) {
            val d = Instant.ofEpochMilli(s.now).atZone(ZoneId.systemDefault())
            Text(d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), color = ink.accent, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Clock(s.now, ink, 112.sp)
            AlarmLine(s, ink)
        }
        if (!night) MonthGrid(s, ink, Modifier.weight(1f))
    }
}

@Composable
private fun BigClockPage(s: DockState, ink: DockInk) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Clock(s.now, ink, 168.sp)
        AlarmLine(s, ink, center = true)
    }
}

@Composable
private fun AgendaPage(s: DockState, ink: DockInk, night: Boolean) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(ink, Modifier.weight(1f).fillMaxWidth()) {
                Text("Now", color = ink.dim, fontSize = 13.sp)
                Clock(s.now, ink, 64.sp)
                AlarmLine(s, ink)
            }
            if (!night) Card(ink, Modifier.weight(1f).fillMaxWidth()) {
                val w = s.weather
                Text(w?.city ?: "Weather", color = ink.dim, fontSize = 13.sp)
                if (w == null) Text("Set a city in Glint settings", color = ink.fg, fontSize = 16.sp)
                else {
                    Text("${WeatherCodes.glyph(w.code)} ${w.tempC}°", color = ink.fg, fontSize = 40.sp, fontWeight = FontWeight.Light)
                    Text("${w.condition} · H ${w.highC}° L ${w.lowC}°", color = ink.dim, fontSize = 14.sp)
                }
            }
        }
        Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(ink, Modifier.weight(1f).fillMaxWidth()) {
                Text("Up next", color = ink.dim, fontSize = 13.sp)
                if (s.events.isEmpty()) Text("Nothing on your calendar", color = ink.fg, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
                s.events.take(if (night) 2 else 4).forEach { e ->
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(ink.accent))
                        Text(if (e.allDay) "All day" else time(e.beginMillis), color = ink.dim, fontSize = 14.sp,
                            modifier = Modifier.padding(start = 10.dp).width(72.dp))
                        Text(e.title, color = ink.fg, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            s.track?.takeIf { !night }?.let { t ->
                Card(ink, Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        t.art?.let { Image(it.asImageBitmap(), null, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop) }
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(t.title, color = ink.fg, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.artist, color = ink.dim, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(ink: DockInk, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(28.dp)).background(ink.card).padding(20.dp), content = content)
}

@Composable
private fun AlarmLine(s: DockState, ink: DockInk, center: Boolean = false) {
    val text = s.nextAlarm?.let { a ->
        val mins = ((a - s.now) / 60_000).coerceAtLeast(0)
        "⏰  ${time(a)}  ·  in ${mins / 60} h ${mins % 60} min"
    } ?: "No alarm set"
    Text(text, color = if (s.nextAlarm != null) ink.fg else ink.dim, fontSize = 18.sp,
        textAlign = if (center) TextAlign.Center else TextAlign.Start, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun MonthGrid(s: DockState, ink: DockInk, modifier: Modifier) {
    val today = LocalDate.now()
    val first = today.withDayOfMonth(1)
    val lead = first.dayOfWeek.value % 7 // Sunday first
    val cells = List(lead) { 0 } + (1..today.lengthOfMonth()).toList()
    Column(modifier.clip(RoundedCornerShape(28.dp)).background(ink.card).padding(18.dp)) {
        Text(today.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(), color = ink.accent,
            fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(Modifier.padding(top = 6.dp)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(it, color = ink.dim, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.padding(top = 2.dp)) {
                (0 until 7).forEach { i ->
                    val day = week.getOrNull(i) ?: 0
                    Box(Modifier.weight(1f).height(26.dp), contentAlignment = Alignment.Center) {
                        if (day > 0) {
                            val isToday = day == today.dayOfMonth
                            Box(Modifier.size(26.dp).clip(CircleShape).background(if (isToday) ink.accent else Color.Transparent),
                                contentAlignment = Alignment.Center) {
                                Text("$day", color = if (isToday) Color.Black else ink.fg, fontSize = 13.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                            }
                            if (!isToday && day in s.busyDays) Box(Modifier.align(Alignment.BottomCenter).size(4.dp).clip(CircleShape).background(ink.accent))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBar(s: DockState, ink: DockInk, modifier: Modifier) {
    val parts = buildList {
        s.battery?.let { add("${if (it.charging) "⚡" else ""}${it.percent}%") }
        if (s.alarmsOnly) add("Alarms only")
    }
    Text(parts.joinToString("   ·   "), color = ink.dim, fontSize = 13.sp, modifier = modifier)
}

@Composable
private fun PageDots(current: Int, ink: DockInk, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(DockFace.entries.size) { i ->
            Box(Modifier.size(if (i == current) 7.dp else 5.dp).clip(CircleShape).background(if (i == current) ink.fg else ink.dim))
        }
    }
}

@Composable
private fun time(millis: Long): String {
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a"))
}
