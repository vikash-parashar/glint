package app.glint.widgets.render

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.app.SearchManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import app.glint.data.Astronomy
import app.glint.data.NowPlaying
import app.glint.data.WeatherCodes
import app.glint.widgets.catalog.WidgetKind
import app.glint.widgets.common.StyleSurface
import app.glint.widgets.common.WidgetInk
import app.glint.widgets.common.inkFor
import app.glint.widgets.engine.ActionKeys
import app.glint.widgets.engine.MusicAction
import app.glint.widgets.engine.StartFocus
import app.glint.widgets.engine.ToggleChecklistItem
import app.glint.widgets.engine.WidgetData
import app.glint.widgets.store.ParamKeys
import app.glint.widgets.store.Quotes
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val zone get() = ZoneId.systemDefault()
private fun fmt(millis: Long, p: String) = Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern(p))
private fun today() = LocalDate.now()

/** Opens the setup screen for this widget (pick design, grant a permission, set a city…). */
private fun configIntent(ctx: Context, id: Int) = Intent().setClassName(ctx.packageName, "app.glint.widgets.config.WidgetConfigActivity")
    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

private fun noteIntent(ctx: Context, id: Int) = Intent().setClassName(ctx.packageName, "app.glint.widgets.config.NoteEditorActivity")
    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

@Composable
fun RenderWidget(d: WidgetData) {
    val ctx = LocalContext.current
    val ink = inkFor(d.style)
    val tile = d.design.kind.ordinal
    if (d.design.kind == WidgetKind.Photo) { PhotoWidget(d, ink); return }
    StyleSurface(ctx, d.style, tile) {
        when (d.design.kind) {
            WidgetKind.ClockDigital -> DigitalClock(d, ink)
            WidgetKind.ClockAnalog -> AnalogClockWidget(d, ink)
            WidgetKind.WorldClock -> WorldClock(d, ink)
            WidgetKind.Date -> DateWidget(d, ink)
            WidgetKind.Agenda -> AgendaWidget(d, ink)
            WidgetKind.Weather -> WeatherWidget(d, ink)
            WidgetKind.Battery -> BatteryWidget(d, ink)
            WidgetKind.Steps -> StepsWidget(d, ink)
            WidgetKind.Music -> MusicWidget(d, ink)
            WidgetKind.Note -> NoteWidget(d, ink)
            WidgetKind.Checklist -> ChecklistWidget(d, ink)
            WidgetKind.Quote -> QuoteWidget(d, ink)
            WidgetKind.Countdown -> CountdownWidget(d, ink)
            WidgetKind.Apps -> AppsWidget(d, ink)
            WidgetKind.Search -> SearchWidget(ink)
            WidgetKind.Focus -> FocusWidgetView(d, ink)
            WidgetKind.Alarm -> AlarmWidget(d, ink)
            WidgetKind.Sky -> SkyWidget(d, ink)
            WidgetKind.Photo -> Unit
        }
    }
}

/* ------------------------------- Clocks ------------------------------- */

@Composable
private fun DigitalClock(d: WidgetData, ink: WidgetInk) {
    val h = LocalSize.current.height.value
    val big = (h * .48f).coerceIn(28f, 96f)
    val serif = d.style.serifClock
    val baseFont = if (serif) ClockFont.Serif else ClockFont.Light
    when (d.design.id) {
        "clock_stacked" -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            // One TextClock with a line break: hours over minutes. Two lines must fit the height.
            LiveClock(ClockFont.Stacked, "hh\nmm", "HH\nmm", (h * .30f).coerceIn(20f, 64f), ink.fg)
        }
        "clock_mono" -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            LiveClock(ClockFont.Mono, "HH:mm", "HH:mm", big * .9f, ink.fg)
            Txt(fmt(d.now, "EEE dd.MM").uppercase(), 12f, ink.dim, family = FontFamily.Monospace)
        }
        "clock_serif" -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            LiveClock(ClockFont.Serif, "h:mm", "H:mm", big, ink.fg)
            Txt(fmt(d.now, "EEEE, d MMMM"), 13f, ink.dim, family = FontFamily.Serif)
        }
        "clock_minimal" -> Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LiveClock(baseFont, "h:mm", "H:mm", big, ink.fg)
        }
        "clock_greeting" -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            val hour = Instant.ofEpochMilli(d.now).atZone(zone).hour
            Txt(when (hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }, 15f, ink.dim, FontWeight.Medium)
            LiveClock(baseFont, "h:mm", "H:mm", big, ink.fg)
        }
        "clock_pill" -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            LiveClock(ClockFont.Medium, "h:mm", "H:mm", (h * .42f).coerceIn(18f, 40f), ink.fg)
            Spacer(GlanceModifier.width(10.dp))
            Txt(fmt(d.now, "EEE d MMM"), 13f, ink.dim, FontWeight.Medium)
        }
        else -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) { // clock_big
            Label(fmt(d.now, "EEEE d MMM"), ink.dim)
            LiveClock(baseFont, "h:mm", "H:mm", big, ink.fg)
        }
    }
}

@Composable
private fun AnalogClockWidget(d: WidgetData, ink: WidgetInk) {
    val size = LocalSize.current
    val density = LocalContext.current.resources.displayMetrics.density
    val px = Bitmaps.clampPx(minOf(size.width.value, size.height.value) - 28f, density, 520)
    val dialKind = when (d.design.id) {
        "analog_numerals" -> Bitmaps.Dial.Numerals
        "analog_dots" -> Bitmaps.Dial.Dots
        "analog_minimal" -> Bitmaps.Dial.Minimal
        else -> Bitmaps.Dial.Classic
    }
    val dial = Bitmaps.dial(px, dialKind, ink.fg.toArgb(), ink.dim.toArgb())
    val hour = Bitmaps.hand(px, .48f, .07f, ink.fg.toArgb())
    val minute = Bitmaps.hand(px, .74f, .045f, ink.accent.let { if (it == ink.fg) ink.fg.copy(alpha = .8f) else it }.toArgb())
    Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LiveAnalog(dial, hour, minute, GlanceModifier.size((px / density).dp))
    }
}

@Composable
private fun WorldClock(d: WidgetData, ink: WidgetInk) {
    val zones = listOf(zone.id) + d.config.param(ParamKeys.ZONES).orEmpty().split(',').filter { it.isNotBlank() }
        .take(if (d.design.id == "world_trio") 2 else 1)
    Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        zones.forEachIndexed { i, z ->
            Column(GlanceModifier.defaultWeight()) {
                Label(if (i == 0) "Here" else z.substringAfterLast('/').replace('_', ' '), ink.dim)
                LiveClock(ClockFont.Light, "h:mm", "H:mm", 30f, if (i == 0) ink.fg else ink.accent, zone = z)
            }
        }
        if (zones.size == 1) Txt("Tap to add a city", 12f, ink.dim, modifier = GlanceModifier.clickable(actionStartActivity(configIntent(LocalContext.current, d.appWidgetId))))
    }
}

/* -------------------------------- Date -------------------------------- */

@Composable
private fun DateWidget(d: WidgetData, ink: WidgetInk) {
    val t = today()
    when (d.design.id) {
        "date_month" -> MonthGrid(t, d.busyDays, ink)
        "date_week" -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Label(t.format(DateTimeFormatter.ofPattern("MMMM yyyy")), ink.dim)
            Spacer(GlanceModifier.height(8.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                val monday = t.with(DayOfWeek.MONDAY)
                (0..6).forEach { i ->
                    val day = monday.plusDays(i.toLong())
                    val isToday = day == t
                    Column(
                        GlanceModifier.defaultWeight().cornerRadius(10.dp)
                            .background(ColorProvider(if (isToday) ink.fg else Color.Transparent)).padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val c = if (isToday) contrastOn(ink.fg) else ink.fg
                        Txt(day.dayOfWeek.getDisplayName(JTextStyle.NARROW, Locale.getDefault()), 10f, if (isToday) c else ink.dim)
                        Txt(day.dayOfMonth.toString(), 16f, c, FontWeight.Bold)
                    }
                }
            }
        }
        "date_page" -> Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            Txt(t.format(DateTimeFormatter.ofPattern("MMMM")).uppercase(), 12f, ink.accent, FontWeight.Bold)
            Txt(t.dayOfMonth.toString(), (LocalSize.current.height.value * .32f).coerceIn(28f, 80f), ink.fg, FontWeight.Bold, FontFamily.Serif)
            Txt(t.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.getDefault()), 13f, ink.dim)
        }
        else -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) { // date_big
            Txt(t.dayOfMonth.toString(), (LocalSize.current.height.value * .5f).coerceIn(32f, 88f), ink.fg, FontWeight.Normal)
            Spacer(GlanceModifier.width(12.dp))
            Column {
                Txt(t.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.getDefault()).uppercase(), 13f, ink.accent, FontWeight.Bold)
                Txt(t.format(DateTimeFormatter.ofPattern("MMMM yyyy")), 13f, ink.dim)
            }
        }
    }
}

@Composable
private fun MonthGrid(t: LocalDate, busy: Set<Int>, ink: WidgetInk) {
    val first = t.withDayOfMonth(1)
    val offset = first.dayOfWeek.value - 1 // Monday first
    val cells = (0 until 42).map { i -> (i - offset + 1).takeIf { it in 1..t.lengthOfMonth() } }
    val rows = cells.chunked(7).filter { r -> r.any { it != null } }
    Column(GlanceModifier.fillMaxSize()) {
        Txt(t.format(DateTimeFormatter.ofPattern("MMMM")), 13f, ink.fg, FontWeight.Bold)
        Spacer(GlanceModifier.height(4.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Txt(it, 9f, ink.dim, align = TextAlign.Center, modifier = GlanceModifier.defaultWeight())
            }
        }
        rows.forEach { r ->
            Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                r.forEach { day ->
                    val isToday = day == t.dayOfMonth
                    Box(
                        GlanceModifier.defaultWeight().cornerRadius(8.dp)
                            .background(ColorProvider(if (isToday) ink.fg else Color.Transparent)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Txt(
                            day?.toString() ?: "", 11f,
                            when { isToday -> contrastOn(ink.fg); day in busy -> ink.accent; else -> ink.fg },
                            if (isToday || day in busy) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

private fun contrastOn(c: Color): Color = if (0.299f * c.red + 0.587f * c.green + 0.114f * c.blue > .5f) Color(0xFF111418) else Color.White

/* ------------------------------- Agenda ------------------------------- */

@Composable
private fun AgendaWidget(d: WidgetData, ink: WidgetInk) {
    val ctx = LocalContext.current
    if (!d.calendarGranted) {
        Column(GlanceModifier.fillMaxSize().clickable(actionStartActivity(configIntent(ctx, d.appWidgetId))), verticalAlignment = Alignment.CenterVertically) {
            Label("Calendar", ink.dim)
            Txt("Tap to show your events", 15f, ink.fg, FontWeight.Bold, maxLines = 2)
        }
        return
    }
    val calIntent = Intent(Intent.ACTION_VIEW).setData(android.provider.CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    Column(GlanceModifier.fillMaxSize().clickable(actionStartActivity(calIntent))) {
        if (d.events.isEmpty()) {
            Label("Next 24 hours", ink.dim)
            Txt("Nothing planned", 16f, ink.fg, FontWeight.Bold)
            return@Column
        }
        val list = if (d.design.id == "agenda_list") d.events else d.events.take(1)
        list.forEachIndexed { i, e ->
            val time = if (e.allDay) "Today" else fmt(e.beginMillis, "H:mm")
            if (d.design.id == "agenda_next") {
                Label("Next · $time", ink.dim)
                Txt(e.title, 17f, ink.fg, FontWeight.Bold, maxLines = 2)
                val mins = ChronoUnit.MINUTES.between(Instant.ofEpochMilli(d.now), Instant.ofEpochMilli(e.beginMillis))
                if (!e.allDay && mins > 0) Txt(if (mins < 60) "in $mins min" else "in ${mins / 60} h ${mins % 60} m", 12f, ink.accent)
            } else {
                Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Txt(time, 12f, ink.accent, FontWeight.Bold, family = FontFamily.Monospace, modifier = GlanceModifier.width(52.dp))
                    Txt(e.title, 13f, ink.fg, FontWeight.Medium, modifier = GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

/* ------------------------------- Weather ------------------------------ */

@Composable
private fun WeatherWidget(d: WidgetData, ink: WidgetInk) {
    val ctx = LocalContext.current
    val w = d.weather
    if (w == null) {
        Column(GlanceModifier.fillMaxSize().clickable(actionStartActivity(configIntent(ctx, d.appWidgetId))), verticalAlignment = Alignment.CenterVertically) {
            Label("Weather", ink.dim)
            Txt(if (d.city == null) "Tap to set your city" else "Can't reach weather", 14f, ink.fg, FontWeight.Bold, maxLines = 2)
        }
        return
    }
    val h = LocalSize.current.height.value
    when (d.design.id) {
        "weather_big" -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
            Label(w.city, ink.dim)
            Txt("${w.tempC}°", (h * .5f).coerceIn(36f, 96f), ink.fg, FontWeight.Normal)
        }
        "weather_range" -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Label(w.city, ink.dim)
            Txt("${w.tempC}° ${WeatherCodes.glyph(w.code)}", 30f, ink.fg, FontWeight.Bold)
            Txt("H ${w.highC}°  L ${w.lowC}°  · feels ${w.feelsC}°", 12f, ink.dim)
        }
        "weather_pill" -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Txt(WeatherCodes.glyph(w.code), 20f, ink.accent)
            Spacer(GlanceModifier.width(8.dp))
            Txt("${w.tempC}° ${w.condition}", 15f, ink.fg, FontWeight.Bold)
            Spacer(GlanceModifier.width(8.dp))
            Txt(w.city, 12f, ink.dim)
        }
        "weather_days" -> Column(GlanceModifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("${w.tempC}°", 26f, ink.fg, FontWeight.Bold)
                Spacer(GlanceModifier.width(8.dp))
                Txt("${w.condition} · ${w.city}", 12f, ink.dim)
            }
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                w.days.drop(1).take(3).forEach { day ->
                    Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Txt(day.date.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.getDefault()), 11f, ink.dim)
                        Txt(WeatherCodes.glyph(day.code), 16f, ink.accent)
                        Txt("${day.highC}° ${day.lowC}°", 11f, ink.fg, FontWeight.Medium)
                    }
                }
            }
        }
        else -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) { // weather_now
            Label(w.city, ink.dim)
            Txt("${w.tempC}°", 34f, ink.fg, FontWeight.Bold)
            Txt("${w.condition}, ${w.highC}° high", 12f, ink.fg, FontWeight.Medium)
        }
    }
}

/* ------------------------------- Battery ------------------------------ */

@Composable
private fun BatteryWidget(d: WidgetData, ink: WidgetInk) {
    val pct = d.battery?.percent ?: 0
    val status = if (d.battery?.charging == true) "Charging" else "On battery"
    val density = LocalContext.current.resources.displayMetrics.density
    when (d.design.id) {
        "battery_ring" -> Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val px = Bitmaps.clampPx(minOf(LocalSize.current.width.value, LocalSize.current.height.value) - 28f, density, 400)
            Img(Bitmaps.ring(px, pct / 100f, ink.fg.toArgb(), ink.track.toArgb()), GlanceModifier.size((px / density).dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Txt("$pct", 22f, ink.fg, FontWeight.Bold)
                Txt(if (d.battery?.charging == true) "⚡" else "%", 10f, ink.dim)
            }
        }
        "battery_bar" -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Label(status, ink.dim)
            Txt("$pct%", 30f, ink.fg, FontWeight.Bold)
            Spacer(GlanceModifier.height(8.dp))
            LinearProgressIndicator(pct / 100f, GlanceModifier.fillMaxWidth().height(8.dp), ColorProvider(ink.accent), ColorProvider(ink.track))
        }
        "battery_pill" -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(pct / 100f, GlanceModifier.defaultWeight().height(10.dp), ColorProvider(ink.accent), ColorProvider(ink.track))
            Spacer(GlanceModifier.width(10.dp))
            Txt("$pct%", 15f, ink.fg, FontWeight.Bold)
        }
        else -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) { // battery_percent
            Label(status, ink.dim)
            val sz = minOf(LocalSize.current.height.value * .42f, LocalSize.current.width.value * .26f)
            Txt("$pct%", sz.coerceIn(22f, 72f), ink.fg, FontWeight.Bold, maxLines = 1)
        }
    }
}

/* -------------------------------- Steps ------------------------------- */

@Composable
private fun StepsWidget(d: WidgetData, ink: WidgetInk) {
    val ctx = LocalContext.current
    if (!d.stepsPermitted || d.steps == null) {
        Column(GlanceModifier.fillMaxSize().clickable(actionStartActivity(configIntent(ctx, d.appWidgetId))), verticalAlignment = Alignment.CenterVertically) {
            Label("Steps", ink.dim)
            Txt(if (!d.stepsPermitted) "Tap to start counting" else "No step sensor", 14f, ink.fg, FontWeight.Bold, maxLines = 2)
        }
        return
    }
    val steps = d.steps
    val progress = steps / d.stepsGoal.toFloat()
    val density = ctx.resources.displayMetrics.density
    when (d.design.id) {
        "steps_ring" -> Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val px = Bitmaps.clampPx(minOf(LocalSize.current.width.value, LocalSize.current.height.value) - 28f, density, 400)
            Img(Bitmaps.ring(px, progress, ink.fg.toArgb(), ink.track.toArgb()), GlanceModifier.size((px / density).dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Txt("%,d".format(steps), 18f, ink.fg, FontWeight.Bold)
                Txt("of %,d".format(d.stepsGoal), 10f, ink.dim)
            }
        }
        "steps_bar" -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Label("Steps today", ink.dim)
            Txt("%,d".format(steps), 28f, ink.fg, FontWeight.Bold)
            Txt("≈ %.1f km".format(steps * 0.00075), 12f, ink.dim)
            Spacer(GlanceModifier.height(6.dp))
            LinearProgressIndicator(progress.coerceIn(0f, 1f), GlanceModifier.fillMaxWidth().height(8.dp), ColorProvider(ink.accent), ColorProvider(ink.track))
        }
        else -> Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
            Label("Steps", ink.dim)
            Txt("%,d".format(steps), 32f, ink.fg, FontWeight.Bold)
        }
    }
}

/* -------------------------------- Music ------------------------------- */

@Composable
private fun MusicWidget(d: WidgetData, ink: WidgetInk) {
    if (!d.mediaAccess) {
        Column(GlanceModifier.fillMaxSize().clickable(actionStartActivity(NowPlaying.accessIntent())), verticalAlignment = Alignment.CenterVertically) {
            Label("Music", ink.dim)
            Txt("Tap, then allow Glint to see what's playing", 13f, ink.fg, FontWeight.Bold, maxLines = 3)
        }
        return
    }
    val t = d.track
    val cmd = { c: NowPlaying.Command -> actionRunCallback<MusicAction>(actionParametersOf(ActionKeys.Command to c.name)) }
    @Composable fun Btn(symbol: String, c: NowPlaying.Command, size: Float = 18f, filled: Boolean = false) {
        Box(
            GlanceModifier.size((size * 2.2f).dp).cornerRadius((size * 1.1f).dp)
                .background(ColorProvider(if (filled) ink.fg else Color.Transparent)).clickable(cmd(c)),
            contentAlignment = Alignment.Center,
        ) { Txt(symbol, size, if (filled) contrastOn(ink.fg) else ink.fg, FontWeight.Bold) }
    }
    val playSymbol = if (t?.playing == true) "❚❚" else "▶"
    when (d.design.id) {
        "music_controls" -> Row(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
            Btn("⏮", NowPlaying.Command.Previous); Spacer(GlanceModifier.width(10.dp))
            Btn(playSymbol, NowPlaying.Command.PlayPause, 20f, filled = true); Spacer(GlanceModifier.width(10.dp))
            Btn("⏭", NowPlaying.Command.Next)
        }
        "music_compact" -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Txt(t?.title ?: "Nothing playing", 14f, ink.fg, FontWeight.Bold)
                Txt(t?.artist ?: "Open a music app", 11f, ink.dim)
            }
            Btn(playSymbol, NowPlaying.Command.PlayPause, 16f, filled = true)
        }
        else -> Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) { // music_player
            val art = t?.art
            if (art != null) {
                Image(ImageProvider(art), null, GlanceModifier.size(72.dp).cornerRadius(14.dp), contentScale = ContentScale.Crop)
                Spacer(GlanceModifier.width(12.dp))
            }
            Column(GlanceModifier.defaultWeight()) {
                Txt(t?.title ?: "Nothing playing", 15f, ink.fg, FontWeight.Bold)
                Txt(t?.artist ?: "Start music in any app", 12f, ink.dim)
                Spacer(GlanceModifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Btn("⏮", NowPlaying.Command.Previous, 13f)
                    Btn(playSymbol, NowPlaying.Command.PlayPause, 14f, filled = true)
                    Btn("⏭", NowPlaying.Command.Next, 13f)
                }
            }
        }
    }
}

/* ------------------------------- Notes -------------------------------- */

@Composable
private fun NoteWidget(d: WidgetData, ink: WidgetInk) {
    val ctx = LocalContext.current
    Column(GlanceModifier.fillMaxSize().clickable(actionStartActivity(noteIntent(ctx, d.appWidgetId)))) {
        Label("Note", ink.dim)
        Spacer(GlanceModifier.height(4.dp))
        Txt(d.note.text.ifBlank { "Tap to write a note" }, 15f, if (d.note.text.isBlank()) ink.dim else ink.fg, FontWeight.Medium, maxLines = 8)
    }
}

@Composable
private fun ChecklistWidget(d: WidgetData, ink: WidgetInk) {
    val ctx = LocalContext.current
    Column(GlanceModifier.fillMaxSize()) {
        Row(GlanceModifier.fillMaxWidth().clickable(actionStartActivity(noteIntent(ctx, d.appWidgetId))), verticalAlignment = Alignment.CenterVertically) {
            Txt(d.note.text.ifBlank { "To do" }, 14f, ink.fg, FontWeight.Bold, modifier = GlanceModifier.defaultWeight())
            Txt("${d.note.items.count { it.done }}/${d.note.items.size}  ✎", 11f, ink.dim)
        }
        Spacer(GlanceModifier.height(4.dp))
        if (d.note.items.isEmpty()) Txt("Tap the title to add items", 12f, ink.dim)
        d.note.items.take(7).forEachIndexed { i, item ->
            Row(
                GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)
                    .clickable(actionRunCallback<ToggleChecklistItem>(actionParametersOf(ActionKeys.Index to i))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt(if (item.done) "●" else "○", 13f, if (item.done) ink.accent else ink.dim)
                Spacer(GlanceModifier.width(8.dp))
                Txt(item.text, 13f, if (item.done) ink.dim else ink.fg, modifier = GlanceModifier.defaultWeight())
            }
        }
    }
}

/* --------------------------- Quote / Countdown ------------------------ */

@Composable
private fun QuoteWidget(d: WidgetData, ink: WidgetInk) {
    val q = Quotes.forDate()
    val bold = d.design.id == "quote_bold"
    Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Txt(
            if (bold) q.text else "“${q.text}”", if (bold) 19f else 16f, ink.fg,
            if (bold) FontWeight.Bold else FontWeight.Normal, if (bold) null else FontFamily.Serif, maxLines = 5,
        )
        Spacer(GlanceModifier.height(6.dp))
        Txt("— ${q.author}", 12f, ink.accent, FontWeight.Medium)
    }
}

@Composable
private fun CountdownWidget(d: WidgetData, ink: WidgetInk) {
    val ctx = LocalContext.current
    when (d.design.id) {
        "progress_year" -> {
            val (day, len) = Astronomy.dayOfYear()
            Progress("${today().year}", "${day * 100 / len}%", "Day $day of $len", day / len.toFloat(), ink)
        }
        "progress_day" -> {
            val p = Astronomy.dayProgress()
            Progress("Today", "${(p * 100).toInt()}%", "${((1 - p) * 24).toInt()} h left", p, ink)
        }
        else -> {
            val target = d.config.param(ParamKeys.DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val title = d.config.param(ParamKeys.TITLE).orEmpty().ifBlank { "Countdown" }
            Column(
                GlanceModifier.fillMaxSize().clickable(actionStartActivity(configIntent(ctx, d.appWidgetId))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Label(title, ink.dim)
                if (target == null) Txt("Tap to pick a date", 15f, ink.fg, FontWeight.Bold)
                else {
                    val days = ChronoUnit.DAYS.between(today(), target)
                    Txt(if (days >= 0) "$days" else "${-days}", 40f, ink.fg, FontWeight.Bold)
                    Txt(when { days == 0L -> "It's today"; days > 0 -> if (days == 1L) "day to go" else "days to go"; else -> "days ago" }, 12f, ink.dim)
                }
            }
        }
    }
}

@Composable
private fun Progress(label: String, big: String, small: String, p: Float, ink: WidgetInk) =
    Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Label(label, ink.dim)
        Txt(big, 32f, ink.fg, FontWeight.Bold)
        Spacer(GlanceModifier.height(6.dp))
        LinearProgressIndicator(p.coerceIn(0f, 1f), GlanceModifier.fillMaxWidth().height(8.dp), ColorProvider(ink.accent), ColorProvider(ink.track))
        Spacer(GlanceModifier.height(4.dp))
        Txt(small, 11f, ink.dim)
    }

/* --------------------------- Apps / Search ---------------------------- */

@Composable
private fun AppsWidget(d: WidgetData, ink: WidgetInk) {
    val ctx = LocalContext.current
    if (d.apps.isEmpty()) {
        Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity(configIntent(ctx, d.appWidgetId))), contentAlignment = Alignment.Center) {
            Txt("Tap to choose apps", 14f, ink.fg, FontWeight.Bold)
        }
        return
    }
    val perRow = 4
    Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        d.apps.take(if (d.design.id == "apps_grid") 8 else 4).chunked(perRow).forEach { row ->
            Row(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                row.forEach { app ->
                    Box(GlanceModifier.defaultWeight().clickable(actionStartActivity(app.launch)), contentAlignment = Alignment.Center) {
                        Img(app.icon, GlanceModifier.size(44.dp))
                    }
                }
                repeat(perRow - row.size) { Spacer(GlanceModifier.defaultWeight()) }
            }
        }
    }
}

@Composable
private fun SearchWidget(ink: WidgetInk) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, "").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    Row(GlanceModifier.fillMaxSize().clickable(actionStartActivity(intent)), verticalAlignment = Alignment.CenterVertically) {
        Txt("⌕", 20f, ink.fg, FontWeight.Bold)
        Spacer(GlanceModifier.width(10.dp))
        Txt("Search", 15f, ink.dim, FontWeight.Medium)
    }
}

/* -------------------------------- Photo ------------------------------- */

@Composable
private fun PhotoWidget(d: WidgetData, ink: WidgetInk) {
    val ctx = LocalContext.current
    val photo = d.photo
    val corner = if (d.style.cornerDp > 0f) d.style.cornerDp.dp else 16.dp
    if (photo == null) {
        StyleSurface(ctx, d.style, d.design.kind.ordinal) {
            Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity(configIntent(ctx, d.appWidgetId))), contentAlignment = Alignment.Center) {
                Txt("Tap to add a photo", 14f, ink.fg, FontWeight.Bold, maxLines = 2)
            }
        }
        return
    }
    Box(GlanceModifier.fillMaxSize().cornerRadius(corner).clickable(actionStartActivity(configIntent(ctx, d.appWidgetId)))) {
        Image(ImageProvider(photo), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (d.design.id == "photo_date") {
            Column(GlanceModifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.Bottom) {
                Txt(today().dayOfMonth.toString(), 34f, Color.White, FontWeight.Bold)
                Txt(today().format(DateTimeFormatter.ofPattern("EEEE, MMMM")), 12f, Color.White.copy(alpha = .9f), FontWeight.Medium)
            }
        }
    }
}

/* ----------------------------- Focus / misc --------------------------- */

@Composable
private fun FocusWidgetView(d: WidgetData, ink: WidgetInk) {
    val s = d.focus
    if (d.design.id == "focus_streak") {
        val st = d.focusStats
        Column(GlanceModifier.fillMaxSize().clickable(actionRunCallback<StartFocus>()), verticalAlignment = Alignment.CenterVertically) {
            Label("Focus streak", ink.dim)
            Txt("${st?.streakDays ?: 0} days", 26f, ink.fg, FontWeight.Bold)
            Row(GlanceModifier.fillMaxWidth().height(28.dp).padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                val week = st?.lastSevenDays ?: List(7) { 0 }
                val max = (week.maxOrNull() ?: 0).coerceAtLeast(1)
                week.forEachIndexed { i, m ->
                    Box(GlanceModifier.defaultWeight().padding(horizontal = 2.dp)) {
                        Box(
                            GlanceModifier.fillMaxWidth().height((4 + 18 * m / max).dp).cornerRadius(3.dp)
                                .background(ColorProvider(if (i == week.lastIndex) ink.accent else ink.track)),
                        ) {}
                    }
                }
            }
        }
        return
    }
    Column(GlanceModifier.fillMaxSize().clickable(actionRunCallback<StartFocus>()), verticalAlignment = Alignment.CenterVertically) {
        Label("Focus", ink.dim)
        if (s == null) {
            Txt("Start 25 min", 19f, ink.fg, FontWeight.Bold)
            Txt("Tap to begin", 12f, ink.dim)
        } else {
            Txt(s.intent, 19f, ink.fg, FontWeight.Bold)
            Txt(s.plannedEndMillis?.let { "until ${fmt(it, "H:mm")}" } ?: "running", 12f, ink.accent)
        }
    }
}

@Composable
private fun AlarmWidget(d: WidgetData, ink: WidgetInk) {
    val clockIntent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    Column(GlanceModifier.fillMaxSize().clickable(actionStartActivity(clockIntent)), verticalAlignment = Alignment.CenterVertically) {
        Label("Next alarm", ink.dim)
        val a = d.nextAlarm
        if (a == null) Txt("No alarm set", 16f, ink.fg, FontWeight.Bold)
        else {
            Txt(fmt(a, "H:mm"), 32f, ink.fg, FontWeight.Bold)
            val mins = ChronoUnit.MINUTES.between(Instant.ofEpochMilli(d.now), Instant.ofEpochMilli(a))
            Txt("${fmt(a, "EEE")} · in ${mins / 60} h ${mins % 60} m", 12f, ink.dim)
        }
    }
}

@Composable
private fun SkyWidget(d: WidgetData, ink: WidgetInk) {
    if (d.design.id == "moon_phase") {
        val phase = Astronomy.moonPhase(d.now)
        val density = LocalContext.current.resources.displayMetrics.density
        Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Img(Bitmaps.moon((44 * density).toInt(), phase, ink.fg.toArgb(), ink.track.toArgb()), GlanceModifier.size(44.dp))
            Spacer(GlanceModifier.width(10.dp))
            Column {
                Label("Tonight", ink.dim)
                Txt(Astronomy.moonName(phase), 13f, ink.fg, FontWeight.Bold, maxLines = 2)
                Txt("${(((1 - kotlin.math.cos(phase * 2 * Math.PI)) / 2) * 100).toInt()}% lit", 12f, ink.dim)
            }
        }
        return
    }
    val s = d.storage ?: return
    Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Label("Storage", ink.dim)
        Txt("%.0f GB free".format(s.freeGb), 22f, ink.fg, FontWeight.Bold)
        Spacer(GlanceModifier.height(6.dp))
        LinearProgressIndicator(s.usedFraction, GlanceModifier.fillMaxWidth().height(8.dp), ColorProvider(ink.accent), ColorProvider(ink.track))
        Txt("of %.0f GB".format(s.totalGb), 11f, ink.dim)
    }
}
