package app.glint.focus

import kotlinx.serialization.Serializable

@Serializable
data class FocusSession(
    val id: Long,
    val intent: String,
    val startMillis: Long,
    /** null = "until I stop". */
    val plannedEndMillis: Long?,
    val allowedPackages: Set<String>,
    val endedMillis: Long? = null,
    val completed: Boolean = false,
    val blockedOpens: Int = 0,
) {
    val isActive: Boolean get() = endedMillis == null

    fun remainingMillis(now: Long): Long? = plannedEndMillis?.let { (it - now).coerceAtLeast(0) }

    fun progress(now: Long): Float = plannedEndMillis?.let {
        val total = (it - startMillis).coerceAtLeast(1)
        ((now - startMillis).toFloat() / total).coerceIn(0f, 1f)
    } ?: 0f

    fun focusedMinutes(now: Long): Int =
        (((endedMillis ?: now) - startMillis) / 60_000).toInt().coerceAtLeast(0)
}

/** A short pass to open one blocked app, earned by holding the button. */
@Serializable
data class TemporaryPass(val packageName: String, val untilMillis: Long)

data class FocusStats(
    val todayMinutes: Int,
    val streakDays: Int,
    /** Minutes per day, oldest first, last entry is today. */
    val lastSevenDays: List<Int>,
)

val FocusIntents = listOf("Deep work", "Study", "Reading", "Family time", "Workout")
val FocusDurations = listOf(25, 50, 90, 0) // 0 = until I stop
