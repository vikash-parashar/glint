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

    /** null = never set; caller falls back to AppRepository.defaultFavorites(). */
    val favorites: Flow<List<String>?> = context.launcherStore.data.map { p ->
        p[kFav]?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
    }
    val city: Flow<String?> = context.launcherStore.data.map { it[kCity] }

    suspend fun setFavorites(list: List<String>) = context.launcherStore.edit { it[kFav] = json.encodeToString(list.take(8)) }
    suspend fun setCity(city: String) = context.launcherStore.edit { it[kCity] = city.trim() }
}
