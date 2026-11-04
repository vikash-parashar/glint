package app.glint.dock

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import app.glint.data.batteryNow

/** The conditions the user asked for: charging + auto-rotate on + tilted to landscape. */
object DockTrigger {
    fun autoRotateOn(context: Context): Boolean =
        Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1

    fun charging(context: Context): Boolean = batteryNow(context)?.charging == true

    fun ready(context: Context): Boolean =
        app.glint.data.ProStore(context).isPro && DockSettings(context).autoStart && autoRotateOn(context) && charging(context)

    fun launchIntent(context: Context, auto: Boolean): Intent =
        Intent(context, DockActivity::class.java)
            .putExtra(DockActivity.EXTRA_AUTO, auto)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
}

/**
 * Listens to gravity while something of ours is on screen (the launcher home or the
 * screen saver) and calls back when the phone is set down on its side or picked up.
 * Android doesn't let apps open screens from the background, so this only runs while
 * Glint is visible; the screen saver covers the locked-phone case.
 */
class TiltWatcher(context: Context, private val onEvent: (TiltClassifier.Event) -> Unit) : SensorEventListener {
    private val sm = context.getSystemService(SensorManager::class.java)
    private val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val classifier = TiltClassifier()

    fun start(alreadyDocked: Boolean = false) {
        classifier.reset(alreadyDocked)
        sensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() = sm.unregisterListener(this)

    override fun onSensorChanged(e: SensorEvent) {
        classifier.feed(e.values[0], e.values[1], e.values[2], e.timestamp / 1_000_000)?.let(onEvent)
    }

    override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
}
