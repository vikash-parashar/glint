package app.glint.data

import android.app.AlarmManager
import android.content.Context
import android.os.Environment
import android.os.StatFs

/** Small device facts that need no permission. */
object DeviceInfo {
    /** Next alarm the user set in any clock app, or null. */
    fun nextAlarmMillis(context: Context): Long? =
        context.getSystemService(AlarmManager::class.java).nextAlarmClock?.triggerTime

    data class Storage(val freeGb: Float, val totalGb: Float) {
        val usedFraction: Float get() = if (totalGb <= 0f) 0f else 1f - freeGb / totalGb
    }

    fun storage(): Storage {
        val s = StatFs(Environment.getDataDirectory().path)
        val gb = 1_000_000_000f
        return Storage(s.availableBytes / gb, s.totalBytes / gb)
    }
}
