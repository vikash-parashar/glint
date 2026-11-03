package app.glint.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import app.glint.launcher.HomeData
import app.glint.launcher.LauncherViewModel
import app.glint.launcher.Overlays
import app.glint.launcher.data.AppEntry
import app.glint.theme.StyleKind

@Composable
fun LauncherRoot(
    home: HomeData,
    overlays: Overlays,
    vm: LauncherViewModel,
    onLaunch: (AppEntry) -> Unit,
    onForceLaunch: (AppEntry) -> Unit,
    onAskCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onGrantDnd: () -> Unit,
    hasDndAccess: () -> Boolean,
) {
    var askCity by remember { mutableStateOf(false) }
    val session = home.session
    val actions = HomeActions(
        launch = onLaunch,
        longPressApp = { vm.toggleFavorite(it.packageName) },
        openDrawer = { vm.openDrawer(true) },
        startFocus = { vm.openFocusSetup(true) },
        askCalendar = onAskCalendar,
        askCity = { askCity = true },
        openSettings = onOpenSettings,
    )

    BackHandler(overlays.drawerOpen) { vm.openDrawer(false) }

    MaterialTheme(colorScheme = if (home.style.kind == StyleKind.Ink) lightColorScheme() else darkColorScheme()) {
        StyleBackground(home.style, muted = session != null) {
            Box(
                Modifier.fillMaxSize().pointerInput(session == null) {
                    if (session == null) detectVerticalDragGestures { _, drag -> if (drag < -40) vm.openDrawer(true) }
                },
            ) {
                if (session != null) {
                    FocusHome(home, session, onLaunch = onLaunch, onEnd = { vm.stopFocus() })
                } else when (home.style.kind) {
                    StyleKind.Bento -> BentoHome(home, overlays, actions)
                    StyleKind.Ink -> InkHome(home, overlays, actions)
                    StyleKind.Liquid -> LiquidHome(home, overlays, actions)
                }
            }

            AnimatedVisibility(
                overlays.drawerOpen && session == null,
                enter = slideInVertically { it / 3 } + fadeIn(), exit = slideOutVertically { it / 3 } + fadeOut(),
            ) {
                AppDrawer(home, onLaunch = onLaunch, onLongPress = { vm.toggleFavorite(it.packageName) })
            }

            if (overlays.focusSetupOpen && session == null) {
                FocusSetupSheet(
                    home = home, hasDndAccess = hasDndAccess(), onGrantDnd = onGrantDnd,
                    onStart = { i, m, a, sil -> vm.startFocus(i, m, a, sil) },
                    onDismiss = { vm.openFocusSetup(false) },
                )
            }

            val blocked = overlays.blocked
            if (blocked != null && session != null) {
                BlockedAppSheet(blocked, session, onBack = { vm.dismissBlocked() }, onOpenAnyway = { onForceLaunch(blocked) })
            }

            overlays.summary?.let { FocusSummaryDialog(it, home.stats, onClose = { vm.dismissSummary() }) }

            if (askCity) CityDialog(overlays.city ?: "", onSave = { vm.setCity(it); askCity = false }, onDismiss = { askCity = false })
        }
    }
}

@Composable
private fun CityDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var city by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weather city") },
        text = {
            OutlinedTextField(city, { city = it }, singleLine = true, label = { Text("City name, e.g. Jaipur") })
        },
        confirmButton = { TextButton(onClick = { if (city.isNotBlank()) onSave(city) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
