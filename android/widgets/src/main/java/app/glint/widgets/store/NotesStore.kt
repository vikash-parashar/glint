package app.glint.widgets.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.notesStore by preferencesDataStore(name = "glint_notes")

@Serializable
data class ChecklistItem(val text: String, val done: Boolean = false)

/** Note text and checklist items, one per widget id. Never leaves the phone. */
@Serializable
data class NoteContent(val text: String = "", val items: List<ChecklistItem> = emptyList())

class NotesStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private fun key(id: Int) = stringPreferencesKey("n_$id")

    suspend fun get(id: Int): NoteContent =
        context.notesStore.data.first()[key(id)]?.let { runCatching { json.decodeFromString<NoteContent>(it) }.getOrNull() } ?: NoteContent()

    suspend fun put(id: Int, note: NoteContent) {
        context.notesStore.edit { it[key(id)] = json.encodeToString(note) }
    }

    suspend fun toggle(id: Int, index: Int) {
        val n = get(id)
        if (index !in n.items.indices) return
        put(id, n.copy(items = n.items.mapIndexed { i, it -> if (i == index) it.copy(done = !it.done) else it }))
    }
}
