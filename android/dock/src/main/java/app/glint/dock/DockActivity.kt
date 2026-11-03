package app.glint.dock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.glint.focus.DndController
import java.time.LocalTime

/**
 * The bedside clock. Opens when the phone is set on its side while charging (from the
 * Glint home screen), from the Quick Settings tile, or from Glint settings.
 * While open: screen stays on, alarms-only Do Not Disturb, dim red at night.
 * Leaves when the charger is pulled out or the phone is picked up and held upright.
 */
class DockActivity : ComponentActivity() {
    companion object { const val EXTRA_AUTO = "auto" }

    private lateinit var settings: DockSettings
    private lateinit var dnd: DndController
    private var tilt: TiltWatcher? = null

    private val power = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == Intent.ACTION_POWER_DISCONNECTED && settings.exitOnUnplug) finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = DockSettings(this)
        dnd = DndController(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (settings.alarmsOnly) dnd.enableAlarmsOnly()
        registerReceiver(power, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))

        // Only auto-exit on pick-up when we were auto-started by tilting; a manual start stays put.
        if (intent.getBooleanExtra(EXTRA_AUTO, false) && DockTrigger.autoRotateOn(this)) {
            tilt = TiltWatcher(this) { if (it == TiltClassifier.Event.Undocked) finish() }
        }

        setContent {
            val state = rememberDockState(this)
            applyBrightness(settings.isNight(LocalTime.now().hour))
            DockScreen(state, settings)
        }
    }

    private var lastNight: Boolean? = null
    private fun applyBrightness(night: Boolean) {
        if (lastNight == night) return
        lastNight = night
        window.attributes = window.attributes.apply {
            screenBrightness = if (night) 0.02f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    override fun onResume() {
        super.onResume()
        tilt?.start(alreadyDocked = true)
    }

    override fun onPause() {
        super.onPause()
        tilt?.stop()
    }

    override fun onStop() {
        super.onStop()
        // Screen turned off at night: keep Dock (and silence) alive. Went to another app: leave Dock.
        val screenOn = getSystemService(PowerManager::class.java).isInteractive
        if (screenOn && !isChangingConfigurations) finish()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(power) }
        if (!isChangingConfigurations) {
            dnd.restoreAfterDock()
            settings.lastExitAt = System.currentTimeMillis()
        }
        super.onDestroy()
    }
}
