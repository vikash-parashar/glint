package app.glint.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cityStore by preferencesDataStore(name = "glint_city")

/** The weather city, shared by the launcher, widgets and Dock mode. */
class CityStore(private val context: Context) {
    private val key = stringPreferencesKey("city")
    val city: Flow<String?> = context.cityStore.data.map { it[key] }
    suspend fun get(): String? = city.first()
    suspend fun set(value: String) = context.cityStore.edit { it[key] = value.trim() }
}
