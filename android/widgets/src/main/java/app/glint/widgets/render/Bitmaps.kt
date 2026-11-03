package app.glint.widgets.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Things RemoteViews can't draw natively, painted once per update. */
object Bitmaps {
    enum class Dial { Classic, Numerals, Dots, Minimal }

    fun dial(sizePx: Int, kind: Dial, fg: Int, dim: Int): Bitmap {
        val b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val r = sizePx / 2f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        when (kind) {
            Dial.Classic -> {
                p.strokeCap = Paint.Cap.ROUND
                for (i in 0 until 60) {
                    val major = i % 5 == 0
                    p.color = if (major) fg else dim
                    p.strokeWidth = if (major) r * .035f else r * .015f
                    val a = Math.toRadians(i * 6.0 - 90)
                    val outer = r * .92f
                    val inner = r * (if (major) .78f else .86f)
                    c.drawLine(r + outer * cos(a).toFloat(), r + outer * sin(a).toFloat(), r + inner * cos(a).toFloat(), r + inner * sin(a).toFloat(), p)
                }
            }
            Dial.Numerals -> {
                p.color = fg; p.textSize = r * .26f; p.textAlign = Paint.Align.CENTER
                p.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                listOf(12 to -90.0, 3 to 0.0, 6 to 90.0, 9 to 180.0).forEach { (n, deg) ->
                    val a = Math.toRadians(deg)
                    val x = r + r * .74f * cos(a).toFloat()
                    val y = r + r * .74f * sin(a).toFloat() - (p.descent() + p.ascent()) / 2
                    c.drawText(n.toString(), x, y, p)
                }
            }
            Dial.Dots -> {
                for (i in 0 until 12) {
                    p.color = if (i % 3 == 0) fg else dim
                    val a = Math.toRadians(i * 30.0 - 90)
                    c.drawCircle(r + r * .82f * cos(a).toFloat(), r + r * .82f * sin(a).toFloat(), r * (if (i % 3 == 0) .055f else .035f), p)
                }
            }
            Dial.Minimal -> Unit
        }
        return b
    }

    /** A hand pointing at 12; AnalogClock rotates it. */
    fun hand(sizePx: Int, lengthFrac: Float, widthFrac: Float, color: Int, tail: Boolean = true): Bitmap {
        val b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val r = sizePx / 2f
        val w = r * widthFrac
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val bottom = if (tail) r + r * .12f else r
        c.drawRoundRect(RectF(r - w / 2, r - r * lengthFrac, r + w / 2, bottom), w / 2, w / 2, p)
        c.drawCircle(r, r, w * 1.1f, p)
        return b
    }

    fun ring(sizePx: Int, progress: Float, color: Int, track: Int, strokeFrac: Float = .12f): Bitmap {
        val b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val sw = sizePx * strokeFrac
        val rect = RectF(sw / 2, sw / 2, sizePx - sw / 2, sizePx - sw / 2)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = sw; strokeCap = Paint.Cap.ROUND }
        p.color = track; c.drawArc(rect, 0f, 360f, false, p)
        p.color = color; c.drawArc(rect, -90f, 360f * progress.coerceIn(0f, 1f), false, p)
        return b
    }

    /** Moon disc lit according to [phase] (0 new → 0.5 full → 1 new). */
    fun moon(sizePx: Int, phase: Double, light: Int, dark: Int): Bitmap {
        val b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val r = sizePx / 2f * .92f
        val cx = sizePx / 2f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = dark; c.drawCircle(cx, cx, r, p)
        // Lit part = half disc + an ellipse whose width follows the phase.
        val waxing = phase < 0.5
        val k = cos(phase * 2 * Math.PI).toFloat() // 1 new, -1 full
        val path = Path()
        val half = RectF(cx - r, cx - r, cx + r, cx + r)
        path.addArc(half, if (waxing) -90f else 90f, 180f)
        val ew = r * kotlin.math.abs(k)
        val ell = RectF(cx - ew, cx - r, cx + ew, cx + r)
        val litPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = light }
        c.drawPath(path, litPaint)
        // Gibbous adds the ellipse in light; crescent cuts it in dark.
        c.drawOval(ell, if (k < 0) litPaint else p)
        return b
    }

    fun clampPx(dp: Float, density: Float, max: Int = 640) = min((dp * density).toInt(), max).coerceAtLeast(32)
}
