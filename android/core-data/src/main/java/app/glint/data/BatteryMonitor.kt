package app.glint.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class BatteryState(val percent: Int, val charging: Boolean)

/** Battery level and charging state. No permission needed. */
fun batteryFlow(context: Context): Flow<BatteryState> = callbackFlow {
    fun parse(i: Intent?): BatteryState? {
        i ?: return null
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0) return null
        return BatteryState(level * 100 / scale, i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0)
    }
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { parse(i)?.let { trySend(it) } }
    }
    val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    parse(sticky)?.let { trySend(it) }
    awaitClose { context.unregisterReceiver(receiver) }
}

/** One-shot read of the sticky battery broadcast. */
fun batteryNow(context: Context): BatteryState? {
    val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
    val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    if (level < 0) return null
    return BatteryState(level * 100 / scale, i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0)
}
