package app.glint

import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.glint.focus.DndController
import app.glint.theme.GlintPalette
import app.glint.theme.GlintStyles
import app.glint.theme.StyleKind
import app.glint.theme.StyleRepository
import app.glint.ui.DockSettingsScreen
import app.glint.ui.WallpaperGallery
import app.glint.ui.WidgetGallery
import app.glint.wallpaper.Wallpapers
import app.glint.widgets.catalog.WidgetCatalog
import app.glint.widgets.engine.GlintWidget
import kotlinx.coroutines.launch

private enum class Screen { Home, Widgets, Wallpapers, Dock }

/** Setup and settings: home app, style, widget gallery, wallpapers, Dock and Focus. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val styles = StyleRepository(this)
        val dnd = DndController(this)
        setContent {
            val style by styles.active.collectAsStateWithLifecycle(GlintStyles.Bento)
            var isHome by remember { mutableStateOf(isDefaultHome()) }
            var dndOk by remember { mutableStateOf(dnd.hasAccess) }
            val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                isHome = isDefaultHome()
            }
            LifecycleResumeEffect(Unit) {
                isHome = isDefaultHome(); dndOk = dnd.hasAccess
                onPauseOrDispose { }
            }

            var screen by remember { mutableStateOf(runCatching { Screen.valueOf(intent.getStringExtra("screen") ?: "Home") }.getOrDefault(Screen.Home)) }
            // Opened straight to a screen from the home menu: Back returns to home.
            val direct = intent.getStringExtra("screen") != null
            val back: () -> Unit = { if (direct) finish() else screen = Screen.Home }
            BackHandler(enabled = screen != Screen.Home) { back() }

            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFFFB59E), onPrimary = Color(0xFF1A1110))) {
                when (screen) {
                    Screen.Widgets -> WidgetGallery(style, toGlintHome = isHome || direct, onAddedToHome = { finish() }, onBack = back)
                    Screen.Wallpapers -> WallpaperGallery(onBack = back)
                    Screen.Dock -> DockSettingsScreen(onBack = back)
                    Screen.Home -> Surface(Modifier.fillMaxSize(), color = Color(0xFF0F1013), contentColor = Color(0xFFE9EAEE)) {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).systemBarsPadding().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Text("Glint", fontSize = 34.sp, fontWeight = FontWeight.Bold)

                        if (!isHome) {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB59E), contentColor = Color(0xFF121316))) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Make Glint your home screen", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text("Needed for the home tiles and for Focus mode to hide distracting apps. You can switch back any time.", fontSize = 13.sp)
                                    Button(
                                        onClick = { requestHomeRole()?.let(roleLauncher::launch) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121316), contentColor = Color.White),
                                    ) { Text("Set as home") }
                                }
                            }
                        }

                        Section("Style")
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlintStyles.all.forEach { p ->
                                StyleCard(p, selected = p.kind == style.kind, modifier = Modifier.weight(1f)) {
                                    lifecycleScope.launch {
                                        styles.set(p.kind)
                                        GlintWidget.refreshAll(this@MainActivity)
                                    }
                                }
                            }
                        }

                        Section("Make it yours")
                        WidgetRow("Widgets", "${WidgetCatalog.all.size} designs: clocks, weather, calendar, music, steps, notes, checklist…", "Browse") { screen = Screen.Widgets }
                        WidgetRow("Wallpapers", "${Wallpapers.all.size} originals that match the styles", "Browse") { screen = Screen.Wallpapers }
                        WidgetRow("Dock", "Pro · Bedside clock when the phone charges on its side. Alarms only.", "Open") { screen = Screen.Dock }

                        Section("Focus mode")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Silence notifications during focus", fontSize = 15.sp)
                                Text(if (dndOk) "Allowed" else "Needs Do Not Disturb access", fontSize = 12.sp, color = Color(0xFF9AA0AB))
                            }
                            if (!dndOk) TextButton(onClick = { startActivity(dnd.accessSettingsIntent()) }) { Text("Allow") }
                        }

                        Section("Privacy")
                        Text(
                            "Glint asks for nothing at install. Calendar, step counting and media access are asked only when you add a widget that needs them. " +
                                "Weather uses the city you type, never your location. Notes, photos and steps never leave your phone; the only network call is the weather lookup.",
                            fontSize = 13.sp, color = Color(0xFF9AA0AB), lineHeight = 19.sp,
                        )
                    }
                }
                }
            }
        }
    }

    private fun isDefaultHome(): Boolean {
        val rm = getSystemService(RoleManager::class.java)
        return rm.isRoleAvailable(RoleManager.ROLE_HOME) && rm.isRoleHeld(RoleManager.ROLE_HOME)
    }

    private fun requestHomeRole(): Intent? {
        val rm = getSystemService(RoleManager::class.java)
        return if (rm.isRoleAvailable(RoleManager.ROLE_HOME)) rm.createRequestRoleIntent(RoleManager.ROLE_HOME)
        else Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
    }

}

@Composable
private fun Section(title: String) =
    Text(title.uppercase(), fontSize = 11.sp, letterSpacing = 1.2.sp, color = Color(0xFF9AA0AB), fontWeight = FontWeight.SemiBold)

@Composable
private fun StyleCard(p: GlintPalette, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = when (p.kind) {
        StyleKind.Bento -> Brush.verticalGradient(listOf(Color(p.tiles[0]), Color(p.tiles[1])))
        StyleKind.Ink -> Brush.verticalGradient(listOf(Color(p.background), Color(p.background)))
        StyleKind.Liquid -> Brush.linearGradient(p.wallpaper.take(3).map { Color(it) })
    }
    Box(
        modifier.height(120.dp).clip(RoundedCornerShape(20.dp)).background(bg)
            .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = .15f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).padding(12.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(p.name, color = if (p.kind == StyleKind.Bento) Color(p.onTile) else Color(p.onBackground), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WidgetRow(name: String, note: String, action: String, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF1D1F25)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(note, fontSize = 12.sp, color = Color(0xFF9AA0AB))
        }
        FilledTonalButton(onClick = onAdd) { Text(action) }
    }
}
