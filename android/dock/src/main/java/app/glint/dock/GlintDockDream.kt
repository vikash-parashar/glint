package app.glint.dock

import android.service.dreams.DreamService
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.glint.focus.DndController
import java.time.LocalTime

/**
 * Glint Dock as the system screen saver. Android starts it on its own when the phone is
 * charging (or docked) and the screen times out, even while locked. That's the only way
 * an app may show something over the lock screen without the user opening it.
 * The user picks it once in Settings → Display → Screen saver.
 */
class GlintDockDream : DreamService(), LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    private val settings by lazy { DockSettings(this) }
    private val dnd by lazy { DndController(this) }

    override fun onCreate() {
        super.onCreate()
        savedState.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false   // a tap wakes the phone, like any screen saver
        isFullscreen = true
        isScreenBright = !settings.isNight(LocalTime.now().hour)
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@GlintDockDream)
            setViewTreeSavedStateRegistryOwner(this@GlintDockDream)
            val isPro = app.glint.data.ProStore(this@GlintDockDream).isPro
            setContent { DockScreen(rememberDockState(this@GlintDockDream), settings, locked = !isPro) }
        }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        if (settings.alarmsOnly && app.glint.data.ProStore(this).isPro) dnd.enableAlarmsOnly()
    }

    override fun onDreamingStopped() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        dnd.restoreAfterDock()
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDetachedFromWindow()
    }
}
