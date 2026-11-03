package app.glint.widgets.config

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Calm dark theme used by every Glint setup screen. */
@Composable
fun GlintSetupTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFFB59E),
            onPrimary = Color(0xFF1A1110),
            background = Color(0xFF0F1013),
            surface = Color(0xFF17181C),
            surfaceVariant = Color(0xFF202127),
            onSurface = Color(0xFFF3F1EE),
            onSurfaceVariant = Color(0xFFA7A6AC),
        ),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
}

@Composable
fun ChoiceCard(title: String, subtitle: String, selected: Boolean, badge: String? = null, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(if (selected) c.surfaceVariant else c.surface, RoundedCornerShape(16.dp))
            .border(1.5.dp, if (selected) c.primary else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.onSurface, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = c.onSurfaceVariant, fontSize = 13.sp)
        }
        if (badge != null) {
            Text(badge, color = c.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(c.primary, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp))
        }
    }
}

@Composable
fun PermissionRow(title: String, why: String, granted: Boolean, action: String, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.onSurface, fontWeight = FontWeight.SemiBold)
            Text(why, color = c.onSurfaceVariant, fontSize = 13.sp)
        }
        if (granted) Text("Allowed", color = c.primary, fontWeight = FontWeight.SemiBold)
        else FilledTonalButton(onClick = onClick) { Text(action) }
    }
}

data class PickableApp(val pkg: String, val label: String, val icon: Bitmap)

suspend fun loadLaunchableApps(context: Context): List<PickableApp> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map { PickableApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm).toBitmap(72, 72)) }
        .sortedBy { it.label.lowercase() }
}

/** A short list of time zones people actually pick; the rest is behind search. */
val CommonZones = listOf(
    "America/Los_Angeles" to "Los Angeles", "America/New_York" to "New York", "America/Sao_Paulo" to "São Paulo",
    "Europe/London" to "London", "Europe/Paris" to "Paris", "Europe/Berlin" to "Berlin", "Africa/Lagos" to "Lagos",
    "Asia/Dubai" to "Dubai", "Asia/Kolkata" to "India", "Asia/Dhaka" to "Dhaka", "Asia/Singapore" to "Singapore",
    "Asia/Shanghai" to "Shanghai", "Asia/Tokyo" to "Tokyo", "Australia/Sydney" to "Sydney", "Pacific/Auckland" to "Auckland",
)
