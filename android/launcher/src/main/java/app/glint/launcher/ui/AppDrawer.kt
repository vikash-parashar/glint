package app.glint.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glint.launcher.HomeData
import app.glint.launcher.data.AppEntry
import app.glint.theme.StyleKind

/** All apps with instant search. Enter launches the top match. Long-press pins to the dock. */
@Composable
fun AppDrawer(home: HomeData, onLaunch: (AppEntry) -> Unit, onLongPress: (AppEntry) -> Unit) {
    val s = home.style
    var query by remember { mutableStateOf("") }
    val results = remember(query, home.apps) {
        if (query.isBlank()) home.apps
        else home.apps.filter { it.label.contains(query, true) }
            .sortedBy { if (it.label.startsWith(query, true)) 0 else 1 }
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val sheetBg = when (s.kind) {
        StyleKind.Ink -> s.background.c()
        StyleKind.Bento -> s.background.c()
        StyleKind.Liquid -> Color.White.copy(alpha = .78f)
    }
    Column(Modifier.fillMaxSize().background(sheetBg).systemBarsPadding().imePadding().padding(16.dp)) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            placeholder = { Text("Search ${home.apps.size} apps") },
            singleLine = true, shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { results.firstOrNull()?.let(onLaunch) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = s.onBackground.c(), unfocusedTextColor = s.onBackground.c(),
                focusedBorderColor = s.onBackground.c().copy(alpha = .4f), unfocusedBorderColor = s.onBackground.c().copy(alpha = .2f),
            ),
        )
        Spacer(Modifier.height(16.dp))
        if (s.kind == StyleKind.Ink) {
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(results.size, key = { results[it].key }) { i ->
                    val app = results[i]
                    Text(app.label, color = s.onBackground.c(), fontSize = 22.sp,
                        modifier = Modifier.fillMaxWidth()
                            .then(Modifier.padding(vertical = 6.dp))
                            .combinedClickableCompat(onClick = { onLaunch(app) }, onLongClick = { onLongPress(app) }))
                }
            }
        } else {
            LazyVerticalGrid(GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                items(results, key = { it.key }) { app ->
                    AppIcon(app, s, 50.dp, onClick = { onLaunch(app) }, onLongClick = { onLongPress(app) })
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit, onLongClick: () -> Unit) =
    this.then(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick))
