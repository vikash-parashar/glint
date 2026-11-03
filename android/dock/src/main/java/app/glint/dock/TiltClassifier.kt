package app.glint.dock

import kotlin.math.abs

/**
 * Turns accelerometer gravity readings into "docked" / "picked up" events.
 *
 * Docked = phone on its side (landscape), screen tilted back or upright like on a stand,
 * not lying flat, and held still for [holdMillis]. Undocked = clearly portrait or picked
 * up and moving for [releaseMillis]. Pure Kotlin so it can be unit-tested on the JVM.
 */
class TiltClassifier(
    private val holdMillis: Long = 1_500,
    private val releaseMillis: Long = 2_000,
) {
    enum class Pose { Landscape, Portrait, Flat, Moving }
    enum class Event { Docked, Undocked }

    private var docked = false
    private var candidateSince = -1L
    private var releaseSince = -1L
    private var last: FloatArray? = null

    fun classify(x: Float, y: Float, z: Float, prev: FloatArray?): Pose {
        if (prev != null) {
            val jolt = abs(x - prev[0]) + abs(y - prev[1]) + abs(z - prev[2])
            if (jolt > 2.5f) return Pose.Moving
        }
        return when {
            abs(z) > 8.6f -> Pose.Flat                           // lying on the table, screen up or down
            abs(x) > 5.5f && abs(x) > abs(y) * 1.4f -> Pose.Landscape
            abs(y) > 6.5f -> Pose.Portrait
            else -> Pose.Moving
        }
    }

    /** Feed one reading; returns an event when the docked state flips. */
    fun feed(x: Float, y: Float, z: Float, t: Long): Event? {
        val pose = classify(x, y, z, last)
        last = floatArrayOf(x, y, z)
        if (!docked) {
            if (pose == Pose.Landscape) {
                if (candidateSince < 0) candidateSince = t
                if (t - candidateSince >= holdMillis) { docked = true; releaseSince = -1; return Event.Docked }
            } else candidateSince = -1
        } else {
            if (pose == Pose.Portrait || pose == Pose.Moving) {
                if (releaseSince < 0) releaseSince = t
                if (t - releaseSince >= releaseMillis) { docked = false; candidateSince = -1; return Event.Undocked }
            } else releaseSince = -1
        }
        return null
    }

    fun reset(isDocked: Boolean = false) { docked = isDocked; candidateSince = -1; releaseSince = -1; last = null }
}
