package app.glint.debug

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import android.util.SizeF
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import app.glint.data.CityStore
import app.glint.widgets.catalog.WidgetCatalog
import app.glint.widgets.catalog.WidgetKind
import app.glint.widgets.engine.GlintWidget
import app.glint.widgets.store.ChecklistItem
import app.glint.widgets.store.NoteContent
import app.glint.widgets.store.NotesStore
import app.glint.widgets.store.ParamKeys
import app.glint.widgets.store.WidgetConfig
import app.glint.widgets.store.WidgetConfigStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Debug-only: hosts 8 widget designs per page (extra "page") so the CI emulator test can
 * screenshot every design. Needs `adb shell appwidget grantbind --package app.glint`.
 */
class WidgetTestActivity : ComponentActivity() {
    private lateinit var host: AppWidgetHost
    private val ids = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        host = AppWidgetHost(this, 0x7E57)
        host.startListening()
        val mgr = AppWidgetManager.getInstance(this)
        val page = intent.getIntExtra("page", 0)
        val designs = WidgetCatalog.all.drop(page * PER_PAGE).take(PER_PAGE)
        val d = resources.displayMetrics.density

        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding((12 * d).toInt(), (28 * d).toInt(), (12 * d).toInt(), 0) }
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.rgb(15, 16, 19)); addView(column) })

        lifecycleScope.launch {
            CityStore(this@WidgetTestActivity).set("Jaipur")
            val row = LinearLayout(this@WidgetTestActivity)
            var rowWidth = 0
            column.addView(row)
            var currentRow = row
            for (design in designs) {
                val id = host.allocateAppWidgetId()
                val provider = ComponentName(packageName, "app.glint.widgets.engine.${design.kind.receiver}")
                if (!mgr.bindAppWidgetIdIfAllowed(id, provider)) {
                    column.addView(TextView(this@WidgetTestActivity).apply { text = "BIND FAILED ${design.id}"; setTextColor(Color.RED) })
                    continue
                }
                ids += id
                WidgetConfigStore(this@WidgetTestActivity).put(id, WidgetConfig(design.id, sampleParams(design.id)))
                if (design.kind == WidgetKind.Note || design.kind == WidgetKind.Checklist) {
                    NotesStore(this@WidgetTestActivity).put(id, NoteContent(
                        if (design.kind == WidgetKind.Note) "Call Riya at 6, buy milk and bread" else "Groceries",
                        listOf(ChecklistItem("Milk", true), ChecklistItem("Eggs"), ChecklistItem("Bread"), ChecklistItem("Coffee")),
                    ))
                }
                val wide = design.kind !in setOf(WidgetKind.ClockAnalog, WidgetKind.Battery, WidgetKind.Photo, WidgetKind.Steps, WidgetKind.Sky, WidgetKind.Focus, WidgetKind.Countdown, WidgetKind.Note)
                val wDp = if (wide) 330f else 160f
                val hDp = if (design.kind == WidgetKind.Apps || design.kind == WidgetKind.Search) 80f else 160f
                if (rowWidth + wDp > 340f) {
                    currentRow = LinearLayout(this@WidgetTestActivity); column.addView(currentRow); rowWidth = 0
                }
                rowWidth += wDp.toInt() + 10
                val view = host.createView(this@WidgetTestActivity, id, mgr.getAppWidgetInfo(id))
                view.updateAppWidgetSize(Bundle(), listOf(SizeF(wDp, hDp)))
                val cell = LinearLayout(this@WidgetTestActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, (10 * d).toInt(), (8 * d).toInt()) }
                cell.addView(TextView(this@WidgetTestActivity).apply { text = design.id; setTextColor(Color.GRAY); textSize = 9f })
                cell.addView(view, LinearLayout.LayoutParams((wDp * d).toInt(), (hDp * d).toInt()))
                currentRow.addView(cell)
                GlintWidget.refresh(this@WidgetTestActivity, id)
            }
            delay(1500)
            ids.forEach { GlintWidget.refresh(this@WidgetTestActivity, it) }
        }
    }

    private fun sampleParams(id: String): Map<String, String> = when (id) {
        "world_dual" -> mapOf(ParamKeys.ZONES to "Europe/London")
        "world_trio" -> mapOf(ParamKeys.ZONES to "Europe/London,America/New_York")
        "countdown_days" -> mapOf(ParamKeys.TITLE to "Goa trip", ParamKeys.DATE to LocalDate.now().plusDays(42).toString())
        "apps_row", "apps_grid" -> mapOf(ParamKeys.APPS to "com.android.chrome,com.google.android.dialer,com.google.android.apps.messaging,com.android.settings,com.google.android.youtube,com.google.android.apps.photos,com.android.camera2,com.google.android.calendar")
        else -> emptyMap()
    }

    override fun onDestroy() {
        ids.forEach { host.deleteAppWidgetId(it) }
        host.stopListening()
        super.onDestroy()
    }

    companion object { const val PER_PAGE = 8 }
}
