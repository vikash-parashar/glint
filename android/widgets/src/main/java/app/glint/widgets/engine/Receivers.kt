package app.glint.widgets.engine

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import app.glint.widgets.store.WidgetConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Base receiver; one subclass per picker entry. Names must match WidgetKind.receiver. */
abstract class GlintReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlintWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { appWidgetIds.forEach { WidgetConfigStore(context).remove(it) } } finally { pending.finish() }
        }
    }
}

class ClockDigitalReceiver : GlintReceiver()
class ClockAnalogReceiver : GlintReceiver()
class WorldClockReceiver : GlintReceiver()
class DateReceiver : GlintReceiver()
class AgendaReceiver : GlintReceiver()
class WeatherReceiver : GlintReceiver()
class BatteryReceiver : GlintReceiver()
class StepsReceiver : GlintReceiver()
class MusicReceiver : GlintReceiver()
class NoteReceiver : GlintReceiver()
class ChecklistReceiver : GlintReceiver()
class QuoteReceiver : GlintReceiver()
class CountdownReceiver : GlintReceiver()
class AppsReceiver : GlintReceiver()
class SearchReceiver : GlintReceiver()
class PhotoReceiver : GlintReceiver()
class FocusReceiver : GlintReceiver()
class AlarmReceiver : GlintReceiver()
class SkyReceiver : GlintReceiver()
