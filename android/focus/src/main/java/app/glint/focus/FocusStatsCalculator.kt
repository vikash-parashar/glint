package app.glint.focus

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Pure functions so the numbers on the summary screen are unit-testable. */
object FocusStatsCalculator {

    /** A day counts toward the streak if the user focused at least this long. */
    const val STREAK_MIN_MINUTES = 10

    fun compute(sessions: List<FocusSession>, now: Long, zone: ZoneId = ZoneId.systemDefault()): FocusStats {
        val byDay = HashMap<LocalDate, Int>()
        for (s in sessions) {
            val day = Instant.ofEpochMilli(s.startMillis).atZone(zone).toLocalDate()
            byDay[day] = (byDay[day] ?: 0) + s.focusedMinutes(now)
        }
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val week = (6 downTo 0).map { byDay[today.minusDays(it.toLong())] ?: 0 }

        // Streak: consecutive qualifying days ending today, or ending yesterday if today
        // has no focus yet (so the streak doesn't look broken first thing in the morning).
        var day = if ((byDay[today] ?: 0) >= STREAK_MIN_MINUTES) today else today.minusDays(1)
        var streak = 0
        while ((byDay[day] ?: 0) >= STREAK_MIN_MINUTES) {
            streak++
            day = day.minusDays(1)
        }
        return FocusStats(todayMinutes = byDay[today] ?: 0, streakDays = streak, lastSevenDays = week)
    }
}
