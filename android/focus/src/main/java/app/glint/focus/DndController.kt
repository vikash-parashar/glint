package app.glint.focus

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Silences notifications using the system Do Not Disturb filter.
 * Needs the user to grant "Do Not Disturb access" once; everything else works without it.
 *
 * Focus uses priority-only (starred contacts and alarms get through). Dock uses
 * alarms-only, so a bedside phone stays silent except for the alarm. Each owner
 * remembers the filter it replaced and only restores it if nobody changed DND since.
 */
class DndController(private val context: Context) {
    private val nm = context.getSystemService(NotificationManager::class.java)
    private val prefs = context.getSharedPreferences("glint_dnd", Context.MODE_PRIVATE)

    val hasAccess: Boolean get() = nm.isNotificationPolicyAccessGranted

    fun accessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Focus: priority-only. */
    fun enable() = take(Owner.Focus, NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    fun restore() = release(Owner.Focus)

    /** Dock: alarms only. */
    fun enableAlarmsOnly() = take(Owner.Dock, NotificationManager.INTERRUPTION_FILTER_ALARMS)
    fun restoreAfterDock() = release(Owner.Dock)

    private enum class Owner(val prevKey: String, val setKey: String) {
        Focus("previous_filter", "focus_set"), Dock("dock_previous_filter", "dock_set")
    }

    private fun take(owner: Owner, filter: Int) {
        if (!hasAccess) return
        if (!prefs.contains(owner.prevKey)) prefs.edit().putInt(owner.prevKey, nm.currentInterruptionFilter).apply()
        nm.setInterruptionFilter(filter)
        prefs.edit().putInt(owner.setKey, filter).apply()
    }

    private fun release(owner: Owner) {
        if (!hasAccess || !prefs.contains(owner.prevKey)) return
        val prev = prefs.getInt(owner.prevKey, NotificationManager.INTERRUPTION_FILTER_ALL)
        val ours = prefs.getInt(owner.setKey, -1)
        // If the user changed DND by hand while we held it, leave their choice alone.
        if (nm.currentInterruptionFilter == ours) nm.setInterruptionFilter(prev)
        prefs.edit().remove(owner.prevKey).remove(owner.setKey).apply()
    }
}
