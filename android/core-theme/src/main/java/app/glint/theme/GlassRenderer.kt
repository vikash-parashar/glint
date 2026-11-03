package app.glint.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import kotlin.random.Random

/**
 * RemoteViews/Glance cannot blur what is behind a widget, so the glass panel is
 * baked into a bitmap: tint at opacity + vertical sheen + grain + top-edge highlight.
 * The result is used as the widget background ImageProvider.
 */
object GlassRenderer {

    /** Liquid style: light frosted panel. Other styles don't need a bitmap. */
    fun panel(style: GlintPalette, widthPx: Int, heightPx: Int, density: Float): Bitmap {
        val theme = BakeSpec(
            seed = style.name.hashCode(), tint = style.surface, opacity = style.glassOpacity,
            cornerDp = style.cornerDp, strokeAlpha = 0.7f, noise = 0.04f,
        )
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = theme.cornerDp * density
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())

        // 1. Tinted body.
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ColorUtils.setAlphaComponent(theme.tint, (theme.opacity * 255).toInt())
        }
        c.drawRoundRect(rect, r, r, body)

        // 2. Sheen: light from the top fading out by mid-height.
        val sheen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h * 0.6f,
                ColorUtils.setAlphaComponent(0xFFFFFFFF.toInt(), 28), 0x00FFFFFF, Shader.TileMode.CLAMP,
            )
        }
        c.drawRoundRect(rect, r, r, sheen)

        // 3. Grain. Deterministic seed so the panel doesn't shimmer between updates.
        if (theme.noise > 0f) {
            val rnd = Random(theme.seed)
            val dot = Paint()
            val count = (w * h * theme.noise / 40).toInt()
            repeat(count) {
                val x = rnd.nextInt(w).toFloat()
                val y = rnd.nextInt(h).toFloat()
                if (rect.contains(x, y)) {
                    dot.color = if (rnd.nextBoolean()) 0x10FFFFFF else 0x10000000
                    c.drawPoint(x, y, dot)
                }
            }
        }

        // 4. Hairline stroke, brighter on top edge.
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 1f * density
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                ColorUtils.setAlphaComponent(0xFFFFFFFF.toInt(), (theme.strokeAlpha * 255).toInt()),
                ColorUtils.setAlphaComponent(0xFFFFFFFF.toInt(), (theme.strokeAlpha * 60).toInt()),
                Shader.TileMode.CLAMP,
            )
        }
        val half = stroke.strokeWidth / 2
        c.drawRoundRect(RectF(half, half, w - half, h - half), r, r, stroke)
        return bmp
    }

    private class BakeSpec(
        val seed: Int, val tint: Int, val opacity: Float, val cornerDp: Float,
        val strokeAlpha: Float, val noise: Float,
    )
}
