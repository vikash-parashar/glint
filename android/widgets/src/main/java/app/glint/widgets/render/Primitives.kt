package app.glint.widgets.render

import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.glint.widgets.R

@Composable
fun Txt(
    text: String, size: Float, color: Color, weight: FontWeight = FontWeight.Normal,
    family: FontFamily? = null, maxLines: Int = 1, align: TextAlign? = null, modifier: GlanceModifier = GlanceModifier,
) = Text(
    text, modifier,
    TextStyle(color = ColorProvider(color), fontSize = size.sp, fontWeight = weight, fontFamily = family, textAlign = align),
    maxLines,
)

@Composable
fun Label(text: String, color: Color) = Txt(text.uppercase(), 10f, color, FontWeight.Medium)

enum class ClockFont(val layout: Int) {
    Light(R.layout.rv_clock_light), Bold(R.layout.rv_clock_bold), Mono(R.layout.rv_clock_mono),
    Serif(R.layout.rv_clock_serif), Medium(R.layout.rv_clock_medium),
}

/**
 * A live clock. TextClock ticks inside the launcher process: no alarms, no wakeups,
 * no battery cost, and it's always on time.
 */
@Composable
fun LiveClock(
    font: ClockFont, f12: String, f24: String, sizeSp: Float, color: Color,
    zone: String? = null, modifier: GlanceModifier = GlanceModifier,
) {
    val ctx = LocalContext.current
    val rv = RemoteViews(ctx.packageName, font.layout).apply {
        setCharSequence(R.id.clock, "setFormat12Hour", f12)
        setCharSequence(R.id.clock, "setFormat24Hour", f24)
        setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(R.id.clock, color.toArgb())
        if (zone != null) setString(R.id.clock, "setTimeZone", zone)
    }
    AndroidRemoteViews(rv, modifier)
}

@Composable
fun LiveAnalog(dial: Bitmap, hour: Bitmap, minute: Bitmap, modifier: GlanceModifier = GlanceModifier) {
    val ctx = LocalContext.current
    val rv = RemoteViews(ctx.packageName, R.layout.rv_analog).apply {
        setIcon(R.id.analog, "setDial", Icon.createWithBitmap(dial))
        setIcon(R.id.analog, "setHourHand", Icon.createWithBitmap(hour))
        setIcon(R.id.analog, "setMinuteHand", Icon.createWithBitmap(minute))
    }
    AndroidRemoteViews(rv, modifier)
}

@Composable
fun Img(bmp: Bitmap, modifier: GlanceModifier = GlanceModifier) =
    Image(ImageProvider(bmp), null, modifier)
