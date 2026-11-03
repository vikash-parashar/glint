package app.glint.widgets.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.widgetStore by preferencesDataStore(name = "glint_widgets")

/** What one placed widget shows: its design plus any settings (title, apps, time zones…). */
@Serializable
data class WidgetConfig(
    val designId: String,
    val params: Map<String, String> = emptyMap(),
) {
    fun param(key: String) = params[key]
}

object ParamKeys {
    const val TITLE = "title"
    const val DATE = "date"        // ISO yyyy-MM-dd
    const val APPS = "apps"        // comma-separated package names
    const val ZONES = "zones"      // comma-separated ZoneId strings
    const val PHOTO = "photo"      // absolute path in app storage
}

class WidgetConfigStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private fun key(id: Int) = stringPreferencesKey("w_$id")
    private val pendingKey = stringPreferencesKey("pending_design")

    suspend fun get(appWidgetId: Int): WidgetConfig? =
        context.widgetStore.data.first()[key(appWidgetId)]?.let { runCatching { json.decodeFromString<WidgetConfig>(it) }.getOrNull() }

    suspend fun put(appWidgetId: Int, config: WidgetConfig) {
        context.widgetStore.edit { it[key(appWidgetId)] = json.encodeToString(config) }
    }

    suspend fun remove(appWidgetId: Int) {
        context.widgetStore.edit { it.remove(key(appWidgetId)) }
    }

    /** Design chosen in the in-app gallery, applied when the pin callback arrives. */
    suspend fun setPending(config: WidgetConfig) = context.widgetStore.edit { it[pendingKey] = json.encodeToString(config) }
    suspend fun takePending(): WidgetConfig? {
        var out: WidgetConfig? = null
        context.widgetStore.edit { p ->
            out = p[pendingKey]?.let { runCatching { json.decodeFromString<WidgetConfig>(it) }.getOrNull() }
            p.remove(pendingKey)
        }
        return out
    }
}
