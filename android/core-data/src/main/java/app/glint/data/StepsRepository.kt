package app.glint.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import kotlin.coroutines.resume

/**
 * Steps today from the phone's own step counter sensor (no Google Fit, no account).
 * The sensor counts since boot, so we keep a per-day baseline. Reads are cheap
 * (one sensor event) and happen when widgets refresh.
 */
class StepsRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("glint_steps", Context.MODE_PRIVATE)

    fun hasSensor() = context.getSystemService(SensorManager::class.java)
        .getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    fun hasPermission() =
        context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    var goal: Int
        get() = prefs.getInt("goal", 8000)
        set(v) = prefs.edit().putInt("goal", v).apply()

    /** Steps since midnight (approximate: counted from the first read of the day), or null. */
    suspend fun today(): Int? {
        if (!hasPermission() || !hasSensor()) return null
        val counter = readCounter() ?: return null
        val today = LocalDate.now().toString()
        var base = prefs.getFloat("base", -1f)
        val day = prefs.getString("day", null)
        // New day, first ever read, or reboot (counter reset below baseline): rebase.
        if (day != today || base < 0f || counter < base) {
            // Carry steps already counted today across a reboot.
            val carried = if (day == today && counter < base) prefs.getInt("last", 0) else 0
            base = counter - carried
            prefs.edit().putString("day", today).putFloat("base", base).apply()
        }
        val steps = (counter - base).toInt().coerceAtLeast(0)
        prefs.edit().putInt("last", steps).apply()
        return steps
    }

    private suspend fun readCounter(): Float? {
        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null
        return withTimeoutOrNull(3000) {
            suspendCancellableCoroutine { cont ->
                val l = object : SensorEventListener {
                    override fun onSensorChanged(e: SensorEvent) {
                        sm.unregisterListener(this)
                        if (cont.isActive) cont.resume(e.values[0])
                    }
                    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
                }
                sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                cont.invokeOnCancellation { sm.unregisterListener(l) }
            }
        }
    }
}
