package app.glint.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.styleStore by preferencesDataStore(name = "glint_style")

/** Single source of truth for the active style. Launcher and widgets both read it. */
class StyleRepository(private val context: Context) {
    private val key = stringPreferencesKey("style_kind")

    val active: Flow<GlintPalette> = context.styleStore.data.map { prefs ->
        val kind = prefs[key]?.let { runCatching { StyleKind.valueOf(it) }.getOrNull() } ?: StyleKind.Bento
        GlintStyles.of(kind)
    }

    suspend fun current(): GlintPalette = active.first()

    suspend fun set(kind: StyleKind) {
        context.styleStore.edit { it[key] = kind.name }
    }
}
