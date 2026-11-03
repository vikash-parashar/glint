package app.glint.focus

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Entry point used by the launcher, the Focus widget and Quick Settings.
 * The session end is scheduled with WorkManager (no exact-alarm permission); the
 * launcher also ends an overdue session itself the moment it is shown.
 */
class FocusController(context: Context) {
    private val app = context.applicationContext
    val repo = FocusRepository(app)
    val dnd = DndController(app)

    suspend fun start(intent: String, minutes: Int, allowed: Set<String>, silence: Boolean): FocusSession {
        val s = repo.start(intent, minutes, allowed, System.currentTimeMillis())
        if (silence) dnd.enable()
        if (minutes > 0) {
            WorkManager.getInstance(app).enqueueUniqueWork(
                WORK_NAME, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<FocusEndWorker>().setInitialDelay(minutes.toLong(), TimeUnit.MINUTES).build(),
            )
        }
        return s
    }

    suspend fun stop(): FocusSession? {
        WorkManager.getInstance(app).cancelUniqueWork(WORK_NAME)
        dnd.restore()
        return repo.end(System.currentTimeMillis())
    }

    /** Called when the launcher resumes: closes a session whose time is up. */
    suspend fun endIfOverdue(): FocusSession? {
        val s = repo.activeNow() ?: return null
        val end = s.plannedEndMillis ?: return null
        return if (System.currentTimeMillis() >= end) stop() else null
    }

    enum class LaunchDecision { Allowed, Blocked }

    suspend fun canLaunch(pkg: String): LaunchDecision {
        val s = repo.activeNow() ?: return LaunchDecision.Allowed
        val now = System.currentTimeMillis()
        if (s.plannedEndMillis != null && now >= s.plannedEndMillis) return LaunchDecision.Allowed
        return if (pkg in s.allowedPackages || pkg in ALWAYS_ALLOWED || repo.hasPass(pkg, now)) {
            LaunchDecision.Allowed
        } else LaunchDecision.Blocked
    }

    companion object {
        const val WORK_NAME = "glint_focus_end"
        /** Emergency and core system apps are never blocked. */
        val ALWAYS_ALLOWED = setOf("com.android.settings", "com.android.emergency", "com.google.android.dialer")
    }
}

class FocusEndWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        FocusController(applicationContext).endIfOverdue()
        return Result.success()
    }
}
