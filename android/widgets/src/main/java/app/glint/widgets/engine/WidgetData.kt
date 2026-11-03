package app.glint.widgets.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toBitmap
import app.glint.data.AgendaRepository
import app.glint.data.BatteryState
import app.glint.data.CityStore
import app.glint.data.DeviceInfo
import app.glint.data.NextEvent
import app.glint.data.NowPlaying
import app.glint.data.StepsRepository
import app.glint.data.Track
import app.glint.data.Weather
import app.glint.data.WeatherRepository
import app.glint.data.batteryNow
import app.glint.focus.FocusController
import app.glint.focus.FocusSession
import app.glint.focus.FocusStats
import app.glint.focus.FocusStatsCalculator
import app.glint.theme.GlintPalette
import app.glint.widgets.catalog.WidgetDesign
import app.glint.widgets.catalog.WidgetKind
import app.glint.widgets.store.NoteContent
import app.glint.widgets.store.NotesStore
import app.glint.widgets.store.ParamKeys
import app.glint.widgets.store.WidgetConfig
import kotlinx.coroutines.flow.first

data class QuickApp(val label: String, val icon: Bitmap, val launch: Intent)

/** Everything a renderer may need. Only the fields for the widget's kind are loaded. */
data class WidgetData(
    val appWidgetId: Int,
    val design: WidgetDesign,
    val config: WidgetConfig,
    val style: GlintPalette,
    val now: Long = System.currentTimeMillis(),
    val battery: BatteryState? = null,
    val city: String? = null,
    val weather: Weather? = null,
    val calendarGranted: Boolean = false,
    val events: List<NextEvent> = emptyList(),
    val busyDays: Set<Int> = emptySet(),
    val steps: Int? = null,
    val stepsGoal: Int = 8000,
    val stepsPermitted: Boolean = false,
    val mediaAccess: Boolean = false,
    val track: Track? = null,
    val note: NoteContent = NoteContent(),
    val apps: List<QuickApp> = emptyList(),
    val photo: Bitmap? = null,
    val focus: FocusSession? = null,
    val focusStats: FocusStats? = null,
    val nextAlarm: Long? = null,
    val storage: DeviceInfo.Storage? = null,
)

object WidgetDataLoader {
    suspend fun load(context: Context, id: Int, design: WidgetDesign, config: WidgetConfig, style: GlintPalette): WidgetData {
        var d = WidgetData(id, design, config, style)
        when (design.kind) {
            WidgetKind.Battery -> d = d.copy(battery = batteryNow(context))
            WidgetKind.Weather -> {
                val city = CityStore(context).get()
                d = d.copy(city = city, weather = city?.let { WeatherRepository.forCity(it) })
            }
            WidgetKind.Agenda, WidgetKind.Date -> {
                val a = AgendaRepository(context)
                d = d.copy(
                    calendarGranted = a.hasPermission(),
                    events = if (design.kind == WidgetKind.Agenda) a.upcoming(limit = 4) else emptyList(),
                    busyDays = if (design.id == "date_month") a.busyDaysThisMonth() else emptySet(),
                )
            }
            WidgetKind.Steps -> {
                val s = StepsRepository(context)
                d = d.copy(steps = s.today(), stepsGoal = s.goal, stepsPermitted = s.hasPermission())
            }
            WidgetKind.Music -> d = d.copy(mediaAccess = NowPlaying.hasAccess(context), track = NowPlaying.current(context))
            WidgetKind.Note, WidgetKind.Checklist -> d = d.copy(note = NotesStore(context).get(id))
            WidgetKind.Apps -> d = d.copy(apps = loadApps(context, config.param(ParamKeys.APPS)))
            WidgetKind.Photo -> d = d.copy(photo = config.param(ParamKeys.PHOTO)?.let { decodeScaled(it, 720) })
            WidgetKind.Focus -> {
                val fc = FocusController(context)
                fc.endIfOverdue()
                d = d.copy(
                    focus = fc.repo.activeNow(),
                    focusStats = FocusStatsCalculator.compute(fc.repo.history.first(), System.currentTimeMillis()),
                )
            }
            WidgetKind.Alarm -> d = d.copy(nextAlarm = DeviceInfo.nextAlarmMillis(context))
            WidgetKind.Sky -> d = d.copy(storage = DeviceInfo.storage())
            else -> Unit
        }
        return d
    }

    private fun loadApps(context: Context, csv: String?): List<QuickApp> {
        val pm = context.packageManager
        return csv.orEmpty().split(',').filter { it.isNotBlank() }.mapNotNull { pkg ->
            val launch = pm.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null
            runCatching {
                val info = pm.getApplicationInfo(pkg, 0)
                QuickApp(pm.getApplicationLabel(info).toString(), pm.getApplicationIcon(pkg).toBitmap(96, 96), launch)
            }.getOrNull()
        }
    }

    private fun decodeScaled(path: String, maxPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
