package app.glint.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glint.data.ProStore

/** Shown instead of Dock until Glint Pro is unlocked. */
@Composable
fun ProGate(pro: ProStore, onUnlocked: () -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0F1013)).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 460.dp).background(Color(0xFF17181C), RoundedCornerShape(28.dp)).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("GLINT PRO", color = Color(0xFFFFB59E), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text("Dock mode", color = Color(0xFFF3F1EE), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Set your phone on its side while it charges and it becomes a bedside clock with your calendar and alarms. Only alarms can ring.",
                color = Color(0xFFA7A6AC), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp,
            )
            Text("${ProStore.PRICE_LABEL} · the launcher and all widgets stay free", color = Color(0xFFA7A6AC), fontSize = 12.sp)
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                Text("Buying opens with the Play Store release")
            }
            if (pro.canTestUnlock) {
                OutlinedButton(onClick = { pro.setPro(true); onUnlocked() }, modifier = Modifier.fillMaxWidth()) { Text("Unlock for testing") }
            }
            TextButton(onClick = onClose) { Text("Not now", color = Color(0xFFA7A6AC)) }
        }
    }
}
