package app.glint.launcher.data

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context

/**
 * Hosts Glint widgets AND any third-party widget. Bind flow:
 * allocateId -> bindAppWidgetIdIfAllowed (or ACTION_APPWIDGET_BIND for consent)
 * -> configure activity if the provider has one -> createView.
 */
class GlintWidgetHost(context: Context) {
    val host = AppWidgetHost(context.applicationContext, HOST_ID)
    val manager: AppWidgetManager = AppWidgetManager.getInstance(context)

    fun start() = host.startListening()
    fun stop() = host.stopListening()

    fun providers(): List<AppWidgetProviderInfo> = manager.installedProviders

    /** Returns the new id, or null if the user must grant bind permission via ACTION_APPWIDGET_BIND. */
    fun tryBind(provider: ComponentName): Int? {
        val id = host.allocateAppWidgetId()
        return if (manager.bindAppWidgetIdIfAllowed(id, provider)) id else {
            host.deleteAppWidgetId(id); null
        }
    }

    companion object { const val HOST_ID = 0x6117 }
}
