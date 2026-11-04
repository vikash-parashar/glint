package app.glint.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glint.focus.FocusDurations
import app.glint.focus.FocusIntents
import app.glint.focus.FocusSession
import app.glint.focus.FocusStats
import app.glint.launcher.HomeData
import app.glint.launcher.data.AppEntry
import app.glint.theme.GlintPalette
import app.glint.theme.StyleKind

/** Colours for the calm focus home: same style family, everything else turned down. */
private data class FocusColors(val bg: Color, val fg: Color, val dim: Color, val accent: Color, val track: Color)

private fun focusColors(s: GlintPalette) = when (s.kind) {
    StyleKind.Ink -> FocusColors(Color(0xFFF7F8FA), s.onBackground.c(), Color(0xFF8A909A), s.accent.c(), Color(0xFFDDE1E7))
    StyleKind.Bento -> FocusColors(Color(0xFF0F1013), Color(0xFFE9EAEE), Color(0xFF9AA0AB), s.tiles[0].c(), Color(0xFF26282E))
    StyleKind.Liquid -> FocusColors(Color.Transparent, s.onBackground.c(), s.onBackgroundDim.c(), s.onBackground.c(), Color.White.copy(alpha = .45f))
}

@Composable
fun FocusHome(home: HomeData, session: FocusSession, onLaunch: (AppEntry) -> Unit, onEnd: () -> Unit) {
    val s = home.style
    val fc = focusColors(s)
    val now = rememberNow()
    val remaining = session.remainingMillis(now)
    val allowed = home.apps.filter { it.packageName in session.allowedPackages }

    Column(
        Modifier.fillMaxSize().background(fc.bg).systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(session.intent.uppercase(), color = fc.dim, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            if (s.kind != StyleKind.Ink) {
                ProgressRing(session.progress(now), fc.accent, fc.track, Modifier.matchParentSize(), 6.dp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    remaining?.let { mmss(it) } ?: mmss(now - session.startMillis),
                    color = fc.fg, fontSize = 60.sp, fontWeight = FontWeight.Light,
                    fontFamily = if (s.serifClock) FontFamily.Serif else FontFamily.Default,
                )
                Text(
                    session.plannedEndMillis?.let { "ends ${hhmm(it)}" } ?: "focused so far",
                    color = fc.dim, fontSize = 12.sp,
                )
            }
        }
        if (s.kind == StyleKind.Ink) {
            Box(Modifier.fillMaxWidth().height(3.dp).background(fc.track)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(session.progress(now)).background(fc.accent))
            }
        }
        if (session.blockedOpens > 0) {
            Text("${session.blockedOpens} distraction${if (session.blockedOpens == 1) "" else "s"} skipped", color = fc.dim, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        if (allowed.isEmpty()) {
            Text("No apps allowed. Phone calls and alarms still work.", color = fc.dim, fontSize = 12.sp, textAlign = TextAlign.Center)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                allowed.take(4).forEach { AppIcon(it, s.copy(onBackground = fc.fg.toArgbInt()), 48.dp, onClick = { onLaunch(it) }) }
            }
        }
        HoldToConfirm(
            label = "Hold to end focus", holdingLabel = "Keep holding…",
            fg = fc.fg, track = fc.track, modifier = Modifier.fillMaxWidth(), onConfirmed = onEnd,
        )
    }
}

private fun Color.toArgbInt(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FocusSetupSheet(
    home: HomeData,
    hasDndAccess: Boolean,
    onGrantDnd: () -> Unit,
    onStart: (intent: String, minutes: Int, allowed: Set<String>, silence: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var intent by remember { mutableStateOf(FocusIntents.first()) }
    var minutes by remember { mutableIntStateOf(50) }
    var silence by remember { mutableStateOf(true) }
    var allowed by remember {
        mutableStateOf(home.defaultAllowed.ifEmpty { home.favorites.take(2).map { it.packageName }.toSet() })
    }
    var pickingApps by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF16171C), contentColor = Color(0xFFECEEF2)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Start focus", fontSize = 24.sp, fontWeight = FontWeight.Medium)
            if (pickingApps) {
                AppPicker(home.apps, allowed, onChange = { allowed = it }, onDone = { pickingApps = false })
                return@Column
            }
            SheetLabel("What are you working on?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusIntents.forEach { FilterChip(selected = intent == it, onClick = { intent = it }, label = { Text(it) }) }
            }
            SheetLabel("For how long")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusDurations.forEach { m ->
                    FilterChip(selected = minutes == m, onClick = { minutes = m }, label = { Text(if (m == 0) "Until I stop" else "$m min") })
                }
            }
            SheetLabel("Apps you can still open")
            Row(Modifier.fillMaxWidth().clickable { pickingApps = true }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                val names = home.apps.filter { it.packageName in allowed }.joinToString { it.label }
                Text(names.ifEmpty { "None. Calls and alarms always work." }, Modifier.weight(1f), fontSize = 14.sp, maxLines = 2)
                Text("Change", color = Color(0xFFFFB59E), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Silence notifications", fontSize = 14.sp)
                    if (silence && !hasDndAccess) Text("Needs Do Not Disturb access, one time", fontSize = 12.sp, color = Color(0xFF9AA0AB))
                }
                Switch(checked = silence, onCheckedChange = { silence = it })
            }
            if (silence && !hasDndAccess) {
                OutlinedButton(onClick = onGrantDnd, modifier = Modifier.fillMaxWidth()) { Text("Allow Do Not Disturb access") }
            }
            Button(
                onClick = { onStart(intent, minutes, allowed, silence) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB59E), contentColor = Color(0xFF121316)),
            ) { Text(if (minutes == 0) "Start focus" else "Start $minutes min focus", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SheetLabel(t: String) =
    Text(t.uppercase(), fontSize = 10.sp, letterSpacing = 1.2.sp, color = Color(0xFF9AA0AB), fontWeight = FontWeight.SemiBold)

@Composable
private fun AppPicker(apps: List<AppEntry>, selected: Set<String>, onChange: (Set<String>) -> Unit, onDone: () -> Unit) {
    var query by remember { mutableStateOf("") }
    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search apps") }, singleLine = true)
    LazyColumn(Modifier.heightIn(max = 380.dp)) {
        items(apps.filter { it.label.contains(query, ignoreCase = true) }, key = { it.key }) { app ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    onChange(if (app.packageName in selected) selected - app.packageName else selected + app.packageName)
                }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.Image(
                    remember(app.packageName) { app.icon.asImageBitmap() }, null, Modifier.size(32.dp),
                )
                Text(app.label, Modifier.weight(1f).padding(start = 12.dp), fontSize = 14.sp)
                Checkbox(checked = app.packageName in selected, onCheckedChange = null)
            }
        }
    }
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done (${selected.size})") }
}


@Composable
fun BlockedAppSheet(
    app: AppEntry, session: FocusSession, onBack: () -> Unit, onOpenAnyway: () -> Unit,
) {
    val now = rememberNow()
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f)).clickable(onClick = onBack), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.padding(12.dp).navigationBarsPadding().fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)).background(Color(0xF0181A1F)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("${app.label.uppercase()} IS PAUSED", color = Color(0xFF8A8F99), fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)
            val left = session.remainingMillis(now)
            Text(
                if (left != null) "You have ${(left / 60_000) + 1} min of focus left." else "You're in a focus session.",
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium,
            )
            Text(
                "You've reached for a blocked app ${session.blockedOpens} time${if (session.blockedOpens == 1) "" else "s"} this session.",
                color = Color(0xFFA6ABB5), fontSize = 13.sp,
            )
            Button(
                onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB59E), contentColor = Color(0xFF121316)),
            ) { Text("Back to focus", fontWeight = FontWeight.Bold) }
            HoldToConfirm(
                label = "Hold 3 s to open for 5 min", holdingLabel = "Keep holding…",
                fg = Color(0xFFE7E9EE), track = Color.White.copy(alpha = .14f),
                modifier = Modifier.fillMaxWidth(), onConfirmed = onOpenAnyway,
            )
        }
    }
}

@Composable
fun FocusSummaryDialog(session: FocusSession, stats: FocusStats, onClose: () -> Unit) {
    val minutes = session.focusedMinutes(session.endedMillis ?: System.currentTimeMillis())
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("Back to home") } },
        title = { Text(if (session.completed) "Session done" else "Session ended early") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$minutes min", fontSize = 48.sp, fontWeight = FontWeight.Light)
                Text("${session.intent} · ${hhmm(session.startMillis)} to ${hhmm(session.endedMillis ?: 0)}", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column { Text("Streak", fontSize = 11.sp); Text("${stats.streakDays} days", fontWeight = FontWeight.Bold) }
                    Column { Text("Skipped", fontSize = 11.sp); Text("${session.blockedOpens} opens", fontWeight = FontWeight.Bold) }
                    Column { Text("Today", fontSize = 11.sp); Text("${stats.todayMinutes} min", fontWeight = FontWeight.Bold) }
                }
                WeekBars(stats.lastSevenDays)
            }
        },
    )
}

@Composable
private fun WeekBars(days: List<Int>) {
    val max = (days.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        days.forEachIndexed { i, m ->
            Box(
                Modifier.weight(1f).fillMaxHeight(m.toFloat() / max).heightIn(min = 3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (i == days.lastIndex) Color(0xFFFFB59E) else Color(0x33FFFFFF)),
            )
        }
    }
}
