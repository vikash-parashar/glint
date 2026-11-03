package app.glint.focus

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class FocusStatsCalculatorTest {
    private val zone = ZoneOffset.UTC
    private fun at(day: Int, hour: Int) = LocalDateTime.of(2026, 11, day, hour, 0).toInstant(zone).toEpochMilli()
    private fun session(day: Int, minutes: Int) = FocusSession(
        id = at(day, 9), intent = "Deep work", startMillis = at(day, 9),
        plannedEndMillis = null, allowedPackages = emptySet(), endedMillis = at(day, 9) + minutes * 60_000L,
    )

    @Test fun streakCountsConsecutiveDaysEndingToday() {
        val s = listOf(session(1, 30), session(2, 15), session(3, 50))
        val stats = FocusStatsCalculator.compute(s, now = at(3, 18), zone = zone)
        assertEquals(3, stats.streakDays)
        assertEquals(50, stats.todayMinutes)
        assertEquals(listOf(0, 0, 0, 0, 30, 15, 50), stats.lastSevenDays)
    }

    @Test fun streakSurvivesUntilUserFocusesToday() {
        val stats = FocusStatsCalculator.compute(listOf(session(1, 30), session(2, 30)), now = at(3, 7), zone = zone)
        assertEquals(2, stats.streakDays)
    }

    @Test fun shortSessionsDoNotCount() {
        val stats = FocusStatsCalculator.compute(listOf(session(2, 5), session(3, 40)), now = at(3, 20), zone = zone)
        assertEquals(1, stats.streakDays)
    }
}
