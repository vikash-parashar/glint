package app.glint.launcher.ui

import android.appwidget.AppWidgetHostView
import android.os.Bundle
import android.util.SizeF
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.glint.launcher.data.GlintWidgetHost
import app.glint.theme.GlintPalette
import app.glint.theme.GlintStyles
import app.glint.theme.StyleKind
import app.glint.widgets.catalog.WidgetKind
import app.glint.widgets.engine.GlintWidget

/** Small widgets sit two per row; everything else takes the full width. */
private val halfKinds = setOf(
    WidgetKind.ClockAnalog, WidgetKind.Battery, WidgetKind.Photo, WidgetKind.Steps, WidgetKind.Sky,
    WidgetKind.Focus, WidgetKind.Countdown, WidgetKind.Note, WidgetKind.Alarm,
)

private fun heightFor(kind: WidgetKind?): Dp = when (kind) {
    WidgetKind.Apps, WidgetKind.Search -> 76.dp
    WidgetKind.Checklist, WidgetKind.Agenda -> 180.dp
    else -> 150.dp
}

/**
 * The user's widgets on the Glint home, in a scrolling column. In edit mode each one
 * gets move and remove buttons; otherwise they behave like normal widgets.
 */
@Composable
fun HomeWidgets(
    ids: List<Int>,
    host: GlintWidgetHost,
    style: GlintPalette,
    editing: Boolean,
    onRemove: (Int) -> Unit,
    onMove: (Int, Boolean) -> Unit,
    onDoneEditing: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val kinds = remember(ids) { ids.associateWith { GlintWidget.kindOf(ctx, it) } }
    val live = ids.filter { host.manager.getAppWidgetInfo(it) != null }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val full = maxWidth
        val gap = 8.dp
        val half = (full - gap) / 2
        if (live.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "+ Add widgets", color = style.onBackgroundDim.c(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onAdd).padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            return@BoxWithConstraints
        }
        // Pack into rows: two halves side by side, wide ones alone.
        val rows = mutableListOf<List<Int>>()
        var pendingHalf: Int? = null
        live.forEach { id ->
            if (kinds[id] in halfKinds) {
                pendingHalf = pendingHalf?.let { rows += listOf(it, id); null } ?: id
            } else {
                pendingHalf?.let { rows += listOf(it); pendingHalf = null }
                rows += listOf(id)
            }
        }
        pendingHalf?.let { rows += listOf(it) }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(gap)) {
            if (editing) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onAdd) { Text("+ Add widget", color = style.onBackground.c()) }
                    Button(onClick = onDoneEditing) { Text("Done") }
                }
            }
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { id ->
                        val w = if (kinds[id] in halfKinds) half else full
                        HostedWidget(id, host, w, heightFor(kinds[id]), editing, onRemove = { onRemove(id) }, onMove = { up -> onMove(id, up) })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HostedWidget(id: Int, host: GlintWidgetHost, w: Dp, h: Dp, editing: Boolean, onRemove: () -> Unit, onMove: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    Box(Modifier.size(w, h)) {
        AndroidView(
            factory = {
                val info = host.manager.getAppWidgetInfo(id)
                host.host.createView(ctx.applicationContext, id, info).apply {
                    setPadding(0, 0, 0, 0)
                    updateAppWidgetSize(Bundle(), listOf(SizeF(w.value, h.value)))
                }
            },
            update = { v: AppWidgetHostView -> v.updateAppWidgetSize(Bundle(), listOf(SizeF(w.value, h.value))) },
            modifier = Modifier.fillMaxSize(),
        )
        if (editing) {
            Box(Modifier.matchParentSize().clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = .35f)))
            Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundButton("↑") { onMove(true) }
                RoundButton("↓") { onMove(false) }
                RoundButton("✕", Color(0xFFE5484D)) { onRemove() }
            }
        }
    }
}

@Composable
private fun RoundButton(label: String, bg: Color = Color.White, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (bg == Color.White) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

/** Shown on top of home until Glint is the default home app. */
@Composable
fun SetHomeBanner(style: GlintPalette, onSet: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFFFB59E)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Make Glint your home screen", color = Color(0xFF1A1110), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("So the Home button brings you here.", color = Color(0xFF1A1110).copy(alpha = .75f), fontSize = 12.sp)
        }
        Button(onClick = onSet, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1110), contentColor = Color.White)) { Text("Set") }
    }
}

/** Long-press on empty home: the one place for widgets, wallpaper, style and settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMenu(
    style: GlintPalette,
    hasWidgets: Boolean,
    onAddWidget: () -> Unit,
    onEditWidgets: () -> Unit,
    onWallpaper: () -> Unit,
    onStyle: (StyleKind) -> Unit,
    onDock: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF17181C)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Home", color = Color(0xFFF3F1EE), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            MenuRow("Add widget", "51 designs, all free", onAddWidget)
            if (hasWidgets) MenuRow("Edit widgets", "Move or remove", onEditWidgets)
            MenuRow("Wallpaper", "24 originals", onWallpaper)
            MenuRow("Dock mode", "Bedside clock while charging · Pro", onDock)
            MenuRow("Settings", "Focus, permissions, privacy", onSettings)
            Text("STYLE", color = Color(0xFF9AA0AB), fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlintStyles.all.forEach { p ->
                    FilterChip(selected = p.kind == style.kind, onClick = { onStyle(p.kind) }, label = { Text(p.name) })
                }
            }
        }
    }
}

@Composable
private fun MenuRow(title: String, sub: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp)) {
        Text(title, color = Color(0xFFF3F1EE), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(sub, color = Color(0xFFA7A6AC), fontSize = 12.sp)
    }
}
