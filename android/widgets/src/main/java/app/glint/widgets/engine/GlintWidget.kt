package app.glint.widgets.engine

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import app.glint.theme.StyleRepository
import app.glint.widgets.catalog.WidgetCatalog
import app.glint.widgets.catalog.WidgetKind
import app.glint.widgets.render.RenderWidget
import app.glint.widgets.store.WidgetConfig
import app.glint.widgets.store.WidgetConfigStore

/**
 * One GlanceAppWidget behind every picker entry. The widget's receiver tells us its
 * kind; the per-widget config tells us which of the 51 designs to draw.
 */
class GlintWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val kind = kindOf(context, appWidgetId) ?: WidgetKind.ClockDigital
        val store = WidgetConfigStore(context)
        val config = store.get(appWidgetId)?.takeIf { WidgetCatalog.byId(it.designId)?.kind == kind }
            ?: WidgetConfig(WidgetCatalog.default(kind).id)
        val design = WidgetCatalog.byId(config.designId) ?: WidgetCatalog.default(kind)
        val style = StyleRepository(context).current()
        val data = WidgetDataLoader.load(context, appWidgetId, design, config, style)
        provideContent { RenderWidget(data) }
    }

    companion object {
        fun kindOf(context: Context, appWidgetId: Int): WidgetKind? =
            WidgetKind.fromReceiverClass(AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider?.className)

        suspend fun refreshAll(context: Context) = GlintWidget().updateAll(context)

        suspend fun refresh(context: Context, appWidgetId: Int) {
            val gid = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            GlintWidget().update(context, gid)
        }

        suspend fun refreshKinds(context: Context, kinds: Set<WidgetKind>) {
            val mgr = GlanceAppWidgetManager(context)
            mgr.getGlanceIds(GlintWidget::class.java).forEach { gid ->
                if (kindOf(context, mgr.getAppWidgetId(gid)) in kinds) GlintWidget().update(context, gid)
            }
        }
    }
}
