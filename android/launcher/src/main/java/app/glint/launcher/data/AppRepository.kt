package app.glint.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class AppEntry(val label: String, val packageName: String, val icon: Bitmap, val launch: Intent) {
    /** Unique per launcher entry: one package can have several (e.g. Google app and its Assistant). */
    val key: String get() = launch.component?.flattenToShortString() ?: packageName
}

class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    fun loadApps(): List<AppEntry> =
        launcherApps.getActivityList(null, Process.myUserHandle())
            .filter { it.applicationInfo.packageName != context.packageName }
            .map {
                AppEntry(
                    label = it.label.toString(),
                    packageName = it.applicationInfo.packageName,
                    icon = it.getBadgedIcon(0).toBitmap(144, 144),
                    launch = Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(it.componentName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
                )
            }
            .sortedBy { it.label.lowercase() }

    /** Emits the full app list now and again whenever apps are installed, updated or removed. */
    fun apps(): Flow<List<AppEntry>> = callbackFlow {
        trySend(loadApps())
        val cb = object : LauncherApps.Callback() {
            override fun onPackageRemoved(p: String, u: UserHandle) { trySend(loadApps()) }
            override fun onPackageAdded(p: String, u: UserHandle) { trySend(loadApps()) }
            override fun onPackageChanged(p: String, u: UserHandle) { trySend(loadApps()) }
            override fun onPackagesAvailable(p: Array<out String>, u: UserHandle, r: Boolean) { trySend(loadApps()) }
            override fun onPackagesUnavailable(p: Array<out String>, u: UserHandle, r: Boolean) { trySend(loadApps()) }
        }
        launcherApps.registerCallback(cb, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(cb) }
    }

    /** Sensible first-run dock: the user's default dialer, SMS, camera and browser. */
    fun defaultFavorites(): List<String> {
        val pm = context.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:")),
            Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE),
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com")),
        )
        return intents.mapNotNull { pm.resolveActivity(it, 0)?.activityInfo?.packageName }
            .filter { it != "android" }.distinct()
    }
}
