package app.glint.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.launcherStore by preferencesDataStore(name = "glint_launcher")

class LauncherPrefs(private val context: Context) {
    private val json = Json
    private val kFav = stringPreferencesKey("favorites")
    private val kCity = stringPreferencesKey("weather_city")
    private val kWidgets = stringPreferencesKey("home_widgets")
    private val kBannerHidden = stringPreferencesKey("home_banner_hidden")

    /** null = never set; caller falls back to AppRepository.defaultFavorites(). */
    val favorites: Flow<List<String>?> = context.launcherStore.data.map { p ->
        p[kFav]?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
    }
    val city: Flow<String?> = context.launcherStore.data.map { it[kCity] }

    /** App widget ids placed on the Glint home, top to bottom. */
    val widgets: Flow<List<Int>> = context.launcherStore.data.map { p ->
        p[kWidgets]?.split(',')?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    }
    suspend fun addWidget(id: Int) = context.launcherStore.edit { p ->
        val cur = p[kWidgets]?.split(',')?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        p[kWidgets] = (cur + id).joinToString(",")
    }
    suspend fun removeWidget(id: Int) = context.launcherStore.edit { p ->
        val cur = p[kWidgets]?.split(',')?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        p[kWidgets] = (cur - id).joinToString(",")
    }
    suspend fun moveWidget(id: Int, up: Boolean) = context.launcherStore.edit { p ->
        val cur = (p[kWidgets]?.split(',')?.mapNotNull { it.toIntOrNull() } ?: emptyList()).toMutableList()
        val i = cur.indexOf(id); val j = if (up) i - 1 else i + 1
        if (i >= 0 && j in cur.indices) { cur[i] = cur[j]; cur[j] = id }
        p[kWidgets] = cur.joinToString(",")
    }

    suspend fun setFavorites(list: List<String>) = context.launcherStore.edit { it[kFav] = json.encodeToString(list.take(8)) }
    suspend fun setCity(city: String) = context.launcherStore.edit { it[kCity] = city.trim() }
}
