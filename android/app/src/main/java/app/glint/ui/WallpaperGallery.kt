package app.glint.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glint.wallpaper.WallTarget
import app.glint.wallpaper.WallpaperSpec
import app.glint.wallpaper.Wallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 24 wallpapers drawn on the phone. Tap one, choose home, lock or both. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperGallery(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var picked by remember { mutableStateOf<WallpaperSpec?>(null) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(containerColor = Color(0xFF0F1013), snackbarHost = { SnackbarHost(snack) }) { pad ->
        LazyVerticalGrid(
            GridCells.Fixed(3), Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(3) }) {
                Box(Modifier.padding(bottom = 6.dp)) { Header("Wallpapers", "${Wallpapers.all.size} originals, drawn at your screen's exact size.", onBack) }
            }
            items(Wallpapers.all, key = { it.id }) { spec ->
                Column(Modifier.clickable { picked = spec }) {
                    Thumb(spec)
                    Text(spec.name, color = Color(0xFFA7A6AC), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }

    picked?.let { spec ->
        ModalBottomSheet(onDismissRequest = { if (!busy) picked = null }, containerColor = Color(0xFF17181C)) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(spec.name, color = Color(0xFFF3F1EE), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                listOf(WallTarget.Home to "Home screen", WallTarget.Lock to "Lock screen", WallTarget.Both to "Both").forEach { (t, label) ->
                    Button(
                        enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                        onClick = {
                            busy = true
                            scope.launch {
                                val ok = runCatching { Wallpapers.apply(ctx, spec, t) }.isSuccess
                                busy = false; picked = null
                                snack.showSnackbar(if (ok) "Wallpaper set" else "Couldn't set the wallpaper")
                            }
                        },
                    ) { Text(if (busy) "Setting…" else label) }
                }
            }
        }
    }
}

@Composable
private fun Thumb(spec: WallpaperSpec) {
    var bmp by remember(spec.id) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(spec.id) { bmp = withContext(Dispatchers.Default) { Wallpapers.render(spec, 216, 468) } }
    Box(Modifier.fillMaxWidth().aspectRatio(9f / 19.5f).clip(RoundedCornerShape(16.dp)).background(Color(spec.palette.base))) {
        bmp?.let { Image(it.asImageBitmap(), spec.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}
