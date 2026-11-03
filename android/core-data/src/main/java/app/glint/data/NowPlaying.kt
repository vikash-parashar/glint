package app.glint.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.scale

data class Track(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val playing: Boolean,
    val appPackage: String,
)

/** Reads and controls the current media session. Needs Notification access, nothing else. */
object NowPlaying {
    /** Called when media changes so widgets/dock can refresh. Set by the widgets module. */
    @Volatile var onChanged: ((Context) -> Unit)? = null

    fun hasAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun accessIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun controller(context: Context): MediaController? {
        if (!hasAccess(context)) return null
        val msm = context.getSystemService(MediaSessionManager::class.java)
        val sessions = runCatching {
            msm.getActiveSessions(ComponentName(context, MediaListenerService::class.java))
        }.getOrNull().orEmpty()
        return sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()
    }

    fun current(context: Context): Track? {
        val c = controller(context) ?: return null
        val md = c.metadata ?: return null
        val art = (md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md.getBitmap(MediaMetadata.METADATA_KEY_ART))
            ?.let { b -> val s = 256f / maxOf(b.width, b.height); if (s < 1f) b.scale((b.width * s).toInt(), (b.height * s).toInt()) else b }
        return Track(
            title = md.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
            artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
            art = art,
            playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            appPackage = c.packageName,
        )
    }

    enum class Command { PlayPause, Next, Previous }

    fun send(context: Context, cmd: Command) {
        val c = controller(context) ?: return
        val t = c.transportControls
        when (cmd) {
            Command.PlayPause -> if (c.playbackState?.state == PlaybackState.STATE_PLAYING) t.pause() else t.play()
            Command.Next -> t.skipToNext()
            Command.Previous -> t.skipToPrevious()
        }
    }
}

/**
 * We don't read notifications. This service exists because Android only shares
 * media sessions with apps that hold notification-listener access.
 */
class MediaListenerService : NotificationListenerService() {
    private var sessions: MediaSessionManager? = null
    private val callbacks = HashMap<MediaSession.Token, MediaController.Callback>()

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        watch(list.orEmpty())
        notifyChanged()
    }

    override fun onListenerConnected() {
        val msm = getSystemService(MediaSessionManager::class.java)
        sessions = msm
        val cn = ComponentName(this, MediaListenerService::class.java)
        msm.addOnActiveSessionsChangedListener(sessionsListener, cn)
        watch(msm.getActiveSessions(cn))
        notifyChanged()
    }

    override fun onListenerDisconnected() {
        sessions?.removeOnActiveSessionsChangedListener(sessionsListener)
        callbacks.clear()
    }

    private fun watch(list: List<MediaController>) {
        list.forEach { c ->
            if (callbacks.containsKey(c.sessionToken)) return@forEach
            val cb = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) = notifyChanged()
                override fun onPlaybackStateChanged(state: PlaybackState?) = notifyChanged()
            }
            c.registerCallback(cb)
            callbacks[c.sessionToken] = cb
        }
    }

    // Playback state flaps during track changes: coalesce into one update 600 ms later.
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val fire = Runnable { NowPlaying.onChanged?.invoke(applicationContext) }
    private fun notifyChanged() {
        handler.removeCallbacks(fire)
        handler.postDelayed(fire, 600)
    }
}
