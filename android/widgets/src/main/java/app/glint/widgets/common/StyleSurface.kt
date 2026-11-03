package app.glint.widgets.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import app.glint.theme.GlassRenderer
import app.glint.theme.GlintPalette
import app.glint.theme.StyleKind

/** Colours a widget's content should use on top of its surface. */
data class WidgetInk(val fg: Color, val dim: Color, val accent: Color, val track: Color)

fun inkFor(style: GlintPalette): WidgetInk = when (style.kind) {
    StyleKind.Bento -> Color(style.onTile).let { WidgetInk(it, it.copy(alpha = .62f), it, it.copy(alpha = .14f)) }
    StyleKind.Ink -> WidgetInk(Color(style.onBackground), Color(style.onBackgroundDim), Color(style.accent), Color(0xFFDDE1E7))
    StyleKind.Liquid -> Color(style.onBackground).let { WidgetInk(it, Color(style.onBackgroundDim), it, it.copy(alpha = .14f)) }
}

/**
 * The same three looks as the launcher, rendered in RemoteViews:
 * Bento = solid colour tile, Ink = paper card, Liquid = baked frosted glass.
 * [tile] picks the Bento colour so each widget kind keeps its own colour.
 */
@Composable
fun StyleSurface(context: Context, style: GlintPalette, tile: Int, padding: Dp = 14.dp, content: @Composable () -> Unit) {
    val base = GlanceModifier.fillMaxSize()
    when (style.kind) {
        StyleKind.Bento -> Box(
            base.cornerRadius(style.cornerDp.dp).background(ColorProvider(Color(style.tiles[tile % style.tiles.size]))),
        ) { Box(GlanceModifier.fillMaxSize().padding(padding)) { content() } }

        StyleKind.Ink -> Box(
            base.cornerRadius(16.dp).background(ColorProvider(Color(style.background).copy(alpha = .95f))),
        ) { Box(GlanceModifier.fillMaxSize().padding(padding + 2.dp)) { content() } }

        StyleKind.Liquid -> {
            val size = LocalSize.current
            val d = context.resources.displayMetrics.density
            val bmp = GlassRenderer.panel(
                style, (size.width.value * d).toInt().coerceIn(1, 1200), (size.height.value * d).toInt().coerceIn(1, 1200), d,
            )
            Box(base) {
                Image(ImageProvider(bmp), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                Box(GlanceModifier.fillMaxSize().padding(padding + 2.dp)) { content() }
            }
        }
    }
}
