package app.glint.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * 24 original wallpapers, drawn on the phone at its exact screen size (so they're
 * always sharp and add nothing to the download). 8 palettes × 3 patterns.
 */
enum class Pattern(val title: String) { Mesh("Mesh"), Dunes("Dunes"), Orbit("Orbit") }

data class WallPalette(val name: String, val base: Int, val c1: Int, val c2: Int, val c3: Int)

data class WallpaperSpec(val palette: WallPalette, val pattern: Pattern) {
    val id get() = "${palette.name.lowercase()}_${pattern.name.lowercase()}"
    val name get() = "${palette.name} ${pattern.title}"
}

enum class WallTarget(val flags: Int) {
    Home(WallpaperManager.FLAG_SYSTEM), Lock(WallpaperManager.FLAG_LOCK), Both(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
}

object Wallpapers {
    val palettes = listOf(
        WallPalette("Ember", 0xFF1A0F0C.toInt(), 0xFFFF8A5B.toInt(), 0xFFFFC76B.toInt(), 0xFFE0466B.toInt()),
        WallPalette("Lagoon", 0xFF071A22.toInt(), 0xFF2FD3B4.toInt(), 0xFF3A8DFF.toInt(), 0xFF9BF2E1.toInt()),
        WallPalette("Meadow", 0xFF0E1A10.toInt(), 0xFFB5E8C8.toInt(), 0xFF5DBB63.toInt(), 0xFFF2DCA6.toInt()),
        WallPalette("Dusk", 0xFF140F24.toInt(), 0xFF8E7CFF.toInt(), 0xFFFF9A6B.toInt(), 0xFFF3C9E0.toInt()),
        WallPalette("Sand", 0xFFF4EDE2.toInt(), 0xFFE9C9A0.toInt(), 0xFFD9A47A.toInt(), 0xFFF7E3C8.toInt()),
        WallPalette("Glacier", 0xFFEEF3F8.toInt(), 0xFFA9C8FF.toInt(), 0xFFD2E4FF.toInt(), 0xFF7FA6E6.toInt()),
        WallPalette("Bloom", 0xFFFFF1F3.toInt(), 0xFFFFB5C8.toInt(), 0xFFD2BDFF.toInt(), 0xFFFFD8B0.toInt()),
        WallPalette("Night", 0xFF050608.toInt(), 0xFF2F5BFF.toInt(), 0xFF6B3AFF.toInt(), 0xFF1E2A44.toInt()),
    )

    val all: List<WallpaperSpec> = palettes.flatMap { p -> Pattern.entries.map { WallpaperSpec(p, it) } }

    fun byId(id: String) = all.firstOrNull { it.id == id }

    fun render(spec: WallpaperSpec, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val rnd = Random(spec.id.hashCode())
        when (spec.pattern) {
            Pattern.Mesh -> mesh(c, spec.palette, w, h, rnd)
            Pattern.Dunes -> dunes(c, spec.palette, w, h)
            Pattern.Orbit -> orbit(c, spec.palette, w, h)
        }
        grain(c, w, h, rnd, light = isLight(spec.palette.base))
        return bmp
    }

    suspend fun apply(context: Context, spec: WallpaperSpec, target: WallTarget) = withContext(Dispatchers.Default) {
        val wm = WallpaperManager.getInstance(context)
        val metrics = context.resources.displayMetrics
        val w = maxOf(metrics.widthPixels, wm.desiredMinimumWidth.takeIf { it > 0 } ?: 0)
        val h = maxOf(metrics.heightPixels, wm.desiredMinimumHeight.takeIf { it > 0 } ?: 0)
        val bmp = render(spec, w, h)
        withContext(Dispatchers.IO) { wm.setBitmap(bmp, null, true, target.flags) }
        bmp.recycle()
    }

    private fun isLight(color: Int): Boolean {
        val r = (color shr 16) and 0xFF; val g = (color shr 8) and 0xFF; val b = color and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b) > 160
    }

    private fun alpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)

    /** Soft blobs of colour melting into each other. */
    private fun mesh(c: Canvas, p: WallPalette, w: Int, h: Int, rnd: Random) {
        c.drawColor(p.base)
        val colors = listOf(p.c1, p.c2, p.c3, p.c1)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        colors.forEachIndexed { i, col ->
            val cx = w * (0.15f + rnd.nextFloat() * 0.7f)
            val cy = h * (0.1f + i * 0.24f + rnd.nextFloat() * 0.08f)
            val r = maxOf(w, h) * (0.35f + rnd.nextFloat() * 0.2f)
            paint.shader = RadialGradient(cx, cy, r, intArrayOf(alpha(col, 0xD0), alpha(col, 0x00)), null, Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, r, paint)
        }
    }

    /** Layered rolling hills, lighter at the top. */
    private fun dunes(c: Canvas, p: WallPalette, w: Int, h: Int) {
        val sky = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), alpha(p.c3, 0xFF), p.base, Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), sky)
        val layers = listOf(p.c3, p.c1, p.c2, p.c1, p.base)
        layers.forEachIndexed { i, col ->
            val top = h * (0.38f + i * 0.12f)
            val amp = h * (0.035f - i * 0.004f)
            val path = Path().apply {
                moveTo(0f, h.toFloat())
                var x = 0f
                while (x <= w) {
                    val y = top + amp * sin((x / w) * 2 * PI * (1.1 + i * 0.35) + i * 1.7).toFloat()
                    lineTo(x, y); x += w / 60f
                }
                lineTo(w.toFloat(), h.toFloat()); close()
            }
            val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, top - amp, 0f, h.toFloat(), alpha(col, 0xF0), alpha(p.base, 0xFF), Shader.TileMode.CLAMP)
            }
            c.drawPath(path, shade)
        }
    }

    /** Thin rings around an off-screen glow, like a planet's orbit lines. */
    private fun orbit(c: Canvas, p: WallPalette, w: Int, h: Int) {
        c.drawColor(p.base)
        val cx = w * 0.82f; val cy = h * 0.22f
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, w * 0.9f, intArrayOf(alpha(p.c1, 0xC0), alpha(p.c2, 0x40), alpha(p.c2, 0)), floatArrayOf(0f, .45f, 1f), Shader.TileMode.CLAMP)
        }
        c.drawCircle(cx, cy, w * 0.9f, glow)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = w / 360f }
        val maxR = hypot(w.toFloat(), h.toFloat())
        var r = w * 0.18f; var i = 0
        while (r < maxR) {
            ring.color = alpha(if (i % 3 == 0) p.c3 else p.c1, (0x70 - i * 4).coerceAtLeast(0x14))
            c.drawCircle(cx, cy, r, ring)
            r *= 1.22f; i++
        }
        val planet = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.c3; maskFilter = BlurMaskFilter(w / 90f, BlurMaskFilter.Blur.NORMAL)
        }
        c.drawCircle(cx - w * 0.36f, cy + w * 0.44f, w / 70f, planet)
    }

    /** Fine film grain so gradients never band on OLED screens. */
    private fun grain(c: Canvas, w: Int, h: Int, rnd: Random, light: Boolean) {
        val dot = Paint().apply { color = if (light) 0x0A000000 else 0x0CFFFFFF }
        repeat((w * h) / 90) { c.drawPoint(rnd.nextFloat() * w, rnd.nextFloat() * h, dot) }
    }
}
