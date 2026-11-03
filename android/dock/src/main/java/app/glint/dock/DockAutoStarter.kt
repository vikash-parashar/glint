package app.glint.dock

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Used by the Glint home screen. While home is visible and the phone is charging with
 * auto-rotate on, watches for the phone being set on its side and opens Dock.
 * The sensor only runs while charging, so it costs nothing on battery.
 */
class DockAutoStarter(private val activity: Activity) {
    private val watcher = TiltWatcher(activity) { e ->
        if (e == TiltClassifier.Event.Docked && DockTrigger.ready(activity)) {
            activity.startActivity(DockTrigger.launchIntent(activity, auto = true))
        }
    }
    private var watching = false
    private var registered = false

    private val power = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = update()
    }

    fun onResume() {
        activity.registerReceiver(power, IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_POWER_DISCONNECTED)
        })
        registered = true
        update()
    }

    fun onPause() {
        if (registered) runCatching { activity.unregisterReceiver(power) }
        registered = false
        setWatching(false)
    }

    private fun update() = setWatching(DockTrigger.ready(activity))

    private fun setWatching(on: Boolean) {
        if (on == watching) return
        watching = on
        if (on) {
            // Just left Dock while still on the stand: wait for a pick-up before docking again.
            val recentlyClosed = System.currentTimeMillis() - DockSettings(activity).lastExitAt < 30 * 60_000L
            watcher.start(alreadyDocked = recentlyClosed)
        } else watcher.stop()
    }
}
