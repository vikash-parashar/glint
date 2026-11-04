package app.glint.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.glint.data.ProStore
import app.glint.dock.DockSettings
import app.glint.dock.DockTrigger
import app.glint.focus.DndController

/** Dock mode: the phone becomes a bedside clock when you set it on its side while charging. */
@Composable
fun DockSettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val s = remember { DockSettings(ctx) }
    val dnd = remember { DndController(ctx) }
    var autoStart by remember { mutableStateOf(s.autoStart) }
    var alarmsOnly by remember { mutableStateOf(s.alarmsOnly) }
    var night by remember { mutableStateOf(s.nightMode) }
    var exitUnplug by remember { mutableStateOf(s.exitOnUnplug) }
    var rotateOn by remember { mutableStateOf(DockTrigger.autoRotateOn(ctx)) }
    var dndOk by remember { mutableStateOf(dnd.hasAccess) }
    val pro = remember { ProStore(ctx) }
    var isPro by remember { mutableStateOf(pro.isPro) }
    LifecycleResumeEffect(Unit) { rotateOn = DockTrigger.autoRotateOn(ctx); dndOk = dnd.hasAccess; onPauseOrDispose { } }

    Column(Modifier.fillMaxSize().background(Color(0xFF0F1013)).systemBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Header("Dock", "Set your phone on its side while it charges and it turns into a bedside clock with your calendar and alarms. Only alarms can make a sound.", onBack)
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().background(Color(0xFF17181C), RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (isPro) "Glint Pro is on" else "Dock is a Glint Pro extra", color = Color(0xFFF3F1EE), fontWeight = FontWeight.SemiBold)
                    Text(if (isPro) "Thanks for supporting Glint." else "Launcher and widgets stay free. Pro is ${ProStore.PRICE_LABEL}.", color = Color(0xFFA7A6AC), fontSize = 13.sp)
                }
                if (!isPro && pro.canTestUnlock) FilledTonalButton(onClick = { pro.setPro(true); isPro = true }) { Text("Unlock (test)") }
            }
            Button(onClick = { ctx.startActivity(DockTrigger.launchIntent(ctx, auto = false)) },
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Try Dock now", fontWeight = FontWeight.SemiBold) }

            Group("How it starts")
            Toggle("Start when tilted", "Charging + auto-rotate on + phone on its side for 2 seconds, while Glint home is open.", autoStart) {
                autoStart = it; s.autoStart = it
            }
            if (autoStart && !rotateOn) Hint("Auto-rotate is off, so tilting won't start Dock. Turn it on in Quick Settings.")
            Action("Start on the lock screen too", "Pick Glint Dock as your screen saver and set it to start while charging. This is the only way Android lets an app appear over the lock screen by itself.", "Open") {
                ctx.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            Hint("You can also add the Dock tile to Quick Settings.")

            Group("While docked")
            Toggle("Alarms only", "Turns on Do Not Disturb so only alarms ring. Your previous setting comes back when you leave Dock.", alarmsOnly) {
                alarmsOnly = it; s.alarmsOnly = it
            }
            if (alarmsOnly && !dndOk) Action("Allow Do Not Disturb", "Android needs you to allow this once.", "Allow") {
                ctx.startActivity(dnd.accessSettingsIntent())
            }
            Toggle("Night mode", "Dim red clock from ${s.nightFromHour}:00 to ${s.nightToHour}:00, so it doesn't light up the room.", night) {
                night = it; s.nightMode = it
            }
            Toggle("Leave when unplugged", "Dock closes when you pull out the charger.", exitUnplug) {
                exitUnplug = it; s.exitOnUnplug = it
            }
            Hint("Swipe left or right in Dock to switch between Clock + calendar, Big clock and Agenda. The screen shifts slightly each minute to protect it from burn-in.")
        }
    }
}

@Composable private fun Group(t: String) =
    Text(t.uppercase(), fontSize = 11.sp, letterSpacing = 1.2.sp, color = Color(0xFF9AA0AB), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))

@Composable private fun Hint(t: String) = Text(t, fontSize = 12.sp, color = Color(0xFF9AA0AB), lineHeight = 17.sp)

@Composable
private fun Toggle(title: String, sub: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF17181C), RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color(0xFFF3F1EE), fontWeight = FontWeight.SemiBold)
            Text(sub, color = Color(0xFFA7A6AC), fontSize = 12.sp, lineHeight = 17.sp)
        }
        Switch(value, onChange)
    }
}

@Composable
private fun Action(title: String, sub: String, button: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF17181C), RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color(0xFFF3F1EE), fontWeight = FontWeight.SemiBold)
            Text(sub, color = Color(0xFFA7A6AC), fontSize = 12.sp, lineHeight = 17.sp)
        }
        FilledTonalButton(onClick = onClick) { Text(button) }
    }
}
