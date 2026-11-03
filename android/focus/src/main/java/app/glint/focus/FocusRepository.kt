package app.glint.focus

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.focusStore by preferencesDataStore(name = "glint_focus")

/** Persists the active session, history (last 120 sessions), passes and setup defaults. */
class FocusRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val kActive = stringPreferencesKey("active")
    private val kHistory = stringPreferencesKey("history")
    private val kPasses = stringPreferencesKey("passes")
    private val kAllowed = stringPreferencesKey("default_allowed")

    val active: Flow<FocusSession?> = context.focusStore.data.map { it.active() }
    val history: Flow<List<FocusSession>> = context.focusStore.data.map { it.history() }
    val defaultAllowed: Flow<Set<String>> = context.focusStore.data.map { p ->
        p[kAllowed]?.let { json.decodeFromString<Set<String>>(it) } ?: emptySet()
    }

    suspend fun activeNow(): FocusSession? = active.first()

    suspend fun start(intent: String, minutes: Int, allowed: Set<String>, now: Long): FocusSession {
        val s = FocusSession(
            id = now, intent = intent, startMillis = now,
            plannedEndMillis = if (minutes > 0) now + minutes * 60_000L else null,
            allowedPackages = allowed,
        )
        context.focusStore.edit {
            it[kActive] = json.encodeToString(s)
            it[kAllowed] = json.encodeToString(allowed)
            it.remove(kPasses)
        }
        return s
    }

    /** Ends the active session and moves it into history. Returns the finished session. */
    suspend fun end(now: Long): FocusSession? {
        var finished: FocusSession? = null
        context.focusStore.edit { p ->
            val s = p.active() ?: return@edit
            val done = s.copy(
                endedMillis = now,
                completed = s.plannedEndMillis == null || now >= s.plannedEndMillis - 30_000,
            )
            finished = done
            p[kHistory] = json.encodeToString((p.history() + done).takeLast(120))
            p.remove(kActive)
            p.remove(kPasses)
        }
        return finished
    }

    suspend fun recordBlockedOpen() = context.focusStore.edit { p ->
        p.active()?.let { p[kActive] = json.encodeToString(it.copy(blockedOpens = it.blockedOpens + 1)) }
    }

    suspend fun grantPass(pkg: String, now: Long, minutes: Int = 5) = context.focusStore.edit { p ->
        val passes = p.passes().filter { it.untilMillis > now } + TemporaryPass(pkg, now + minutes * 60_000L)
        p[kPasses] = json.encodeToString(passes)
    }

    suspend fun hasPass(pkg: String, now: Long): Boolean =
        context.focusStore.data.first().passes().any { it.packageName == pkg && it.untilMillis > now }

    private fun Preferences.active(): FocusSession? =
        this[kActive]?.let { runCatching { json.decodeFromString<FocusSession>(it) }.getOrNull() }

    private fun Preferences.history(): List<FocusSession> =
        this[kHistory]?.let { runCatching { json.decodeFromString<List<FocusSession>>(it) }.getOrNull() } ?: emptyList()

    private fun Preferences.passes(): List<TemporaryPass> =
        this[kPasses]?.let { runCatching { json.decodeFromString<List<TemporaryPass>>(it) }.getOrNull() } ?: emptyList()
}
