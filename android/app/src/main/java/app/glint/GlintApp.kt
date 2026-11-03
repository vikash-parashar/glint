package app.glint

import android.app.Application
import app.glint.data.NowPlaying
import app.glint.widgets.catalog.WidgetKind
import app.glint.widgets.engine.GlintWidget
import app.glint.widgets.engine.WidgetRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GlintApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Music widgets redraw when the song or play state changes, not on a timer.
        NowPlaying.onChanged = { ctx -> scope.launch { GlintWidget.refreshKinds(ctx, setOf(WidgetKind.Music)) } }
        // Weather, steps, calendar and the rest refresh every 30 minutes (clocks tick on their own).
        WidgetRefreshWorker.schedule(this)
    }
}
