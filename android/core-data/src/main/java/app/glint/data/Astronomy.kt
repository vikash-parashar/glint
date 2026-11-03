package app.glint.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor

object Astronomy {
    private const val SYNODIC = 29.530588853
    /** Reference new moon: 2000-01-06 18:14 UTC. */
    private val REF = Instant.parse("2000-01-06T18:14:00Z").toEpochMilli()

    /** 0.0 = new, 0.5 = full, back to 1.0 = new. */
    fun moonPhase(now: Long = System.currentTimeMillis()): Double {
        val days = (now - REF) / 86_400_000.0
        val p = days / SYNODIC
        return p - floor(p)
    }

    fun moonName(phase: Double) = when {
        phase < 0.03 || phase > 0.97 -> "New moon"
        phase < 0.22 -> "Waxing crescent"
        phase < 0.28 -> "First quarter"
        phase < 0.47 -> "Waxing gibbous"
        phase < 0.53 -> "Full moon"
        phase < 0.72 -> "Waning gibbous"
        phase < 0.78 -> "Last quarter"
        else -> "Waning crescent"
    }

    fun dayOfYear(today: LocalDate = LocalDate.now()) = today.dayOfYear to today.lengthOfYear()

    /** Fraction of the current day that has passed, 0..1. */
    fun dayProgress(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Float {
        val t = Instant.ofEpochMilli(now).atZone(zone).toLocalTime()
        return t.toSecondOfDay() / 86_400f
    }
}
