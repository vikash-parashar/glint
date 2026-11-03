package app.glint.widgets.engine

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import app.glint.widgets.catalog.WidgetDesign
import app.glint.widgets.store.WidgetConfig
import app.glint.widgets.store.WidgetConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Adds a widget from the in-app gallery with the exact design the user picked. */
object WidgetPinning {
    fun supported(context: Context) = AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    suspend fun pin(context: Context, design: WidgetDesign, params: Map<String, String> = emptyMap()): Boolean {
        val mgr = AppWidgetManager.getInstance(context)
        if (!mgr.isRequestPinAppWidgetSupported) return false
        WidgetConfigStore(context).setPending(WidgetConfig(design.id, params))
        val provider = ComponentName(context.packageName, "app.glint.widgets.engine.${design.kind.receiver}")
        val callback = PendingIntent.getBroadcast(
            context, design.id.hashCode(),
            Intent(context, PinResultReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE, // system adds EXTRA_APPWIDGET_ID
        )
        return mgr.requestPinAppWidget(provider, null, callback)
    }
}

class PinResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val store = WidgetConfigStore(context)
                store.takePending()?.let { store.put(id, it) }
                GlintWidget.refresh(context, id)
            } finally { pending.finish() }
        }
    }
}
