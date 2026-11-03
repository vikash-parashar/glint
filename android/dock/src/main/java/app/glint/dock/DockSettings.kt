package app.glint.dock

import android.content.Context

/** Dock preferences. SharedPreferences on purpose: read synchronously from sensors and the dream. */
class DockSettings(context: Context) {
    private val p = context.getSharedPreferences("glint_dock", Context.MODE_PRIVATE)

    /** Start Dock automatically when the phone is charging, auto-rotate is on and it's tilted to landscape. */
    var autoStart: Boolean
        get() = p.getBoolean("auto_start", true)
        set(v) = p.edit().putBoolean("auto_start", v).apply()

    /** Switch to alarms-only Do Not Disturb while docked. */
    var alarmsOnly: Boolean
        get() = p.getBoolean("alarms_only", true)
        set(v) = p.edit().putBoolean("alarms_only", v).apply()

    /** Red, very dim clock between [nightFromHour] and [nightToHour]. */
    var nightMode: Boolean
        get() = p.getBoolean("night_mode", true)
        set(v) = p.edit().putBoolean("night_mode", v).apply()

    var nightFromHour: Int
        get() = p.getInt("night_from", 22)
        set(v) = p.edit().putInt("night_from", v).apply()

    var nightToHour: Int
        get() = p.getInt("night_to", 6)
        set(v) = p.edit().putInt("night_to", v).apply()

    /** Leave Dock when the charger is pulled out. */
    var exitOnUnplug: Boolean
        get() = p.getBoolean("exit_unplug", true)
        set(v) = p.edit().putBoolean("exit_unplug", v).apply()

    /** When Dock last closed; auto-start waits for the phone to be picked up before docking again. */
    var lastExitAt: Long
        get() = p.getLong("last_exit", 0)
        set(v) = p.edit().putLong("last_exit", v).apply()

    var clockFace: DockFace
        get() = runCatching { DockFace.valueOf(p.getString("face", DockFace.Split.name)!!) }.getOrDefault(DockFace.Split)
        set(v) = p.edit().putString("face", v.name).apply()

    fun isNight(hour: Int): Boolean =
        nightMode && if (nightFromHour > nightToHour) hour >= nightFromHour || hour < nightToHour else hour in nightFromHour until nightToHour
}

/** The three StandBy-style pages; the user swipes between them, the last one is remembered. */
enum class DockFace { Split, BigClock, Agenda }
