package app.glint.dock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TiltClassifierTest {
    private fun feedFor(c: TiltClassifier, x: Float, y: Float, z: Float, from: Long, to: Long, step: Long = 100): List<TiltClassifier.Event> {
        val out = mutableListOf<TiltClassifier.Event>()
        var t = from
        while (t <= to) { c.feed(x, y, z, t)?.let(out::add); t += step }
        return out
    }

    @Test fun landscapeOnStandHeldStillDocks() {
        val c = TiltClassifier()
        // On its side, screen tilted back ~35°: gravity mostly on x, some on z.
        assertEquals(listOf(TiltClassifier.Event.Docked), feedFor(c, 8.0f, 0.3f, 5.6f, 0, 2_000))
    }

    @Test fun briefLandscapeDoesNotDock() {
        val c = TiltClassifier()
        assertEquals(emptyList<TiltClassifier.Event>(), feedFor(c, 8.0f, 0.3f, 5.6f, 0, 1_000))
        assertEquals(emptyList<TiltClassifier.Event>(), feedFor(c, 0.3f, 9.6f, 1.0f, 1_100, 3_000))
    }

    @Test fun lyingFlatNeverDocks() {
        val c = TiltClassifier()
        assertEquals(emptyList<TiltClassifier.Event>(), feedFor(c, 0.2f, 0.1f, 9.8f, 0, 5_000))
    }

    @Test fun pickedUpUprightUndocks() {
        val c = TiltClassifier()
        feedFor(c, 8.0f, 0.3f, 5.6f, 0, 2_000)
        assertEquals(listOf(TiltClassifier.Event.Undocked), feedFor(c, 0.4f, 9.4f, 2.0f, 2_100, 4_500))
    }

    @Test fun alreadyDockedStartNeedsPickUpFirst() {
        val c = TiltClassifier()
        c.reset(isDocked = true)
        assertNull(c.feed(8.0f, 0.3f, 5.6f, 0))
        assertEquals(emptyList<TiltClassifier.Event>(), feedFor(c, 8.0f, 0.3f, 5.6f, 100, 5_000))
    }
}
